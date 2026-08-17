/*
 * Copyright 2026 yjyoon
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package dev.yjyoon.lineloginkmp

import dev.yjyoon.lineloginkmp.internal.platformConfigure
import dev.yjyoon.lineloginkmp.internal.platformCurrentAccessToken
import dev.yjyoon.lineloginkmp.internal.platformIsLoggedIn
import dev.yjyoon.lineloginkmp.internal.platformLogin
import dev.yjyoon.lineloginkmp.internal.platformLogout
import kotlinx.coroutines.sync.Mutex
import kotlin.concurrent.Volatile

/**
 * LINE Login, driven from shared code.
 *
 * ```
 * // once, at startup
 * LineLogin.configure(LineLoginConfig(channelId = "1234567890"))
 *
 * // wherever the user taps "Sign in with LINE"
 * when (val result = LineLogin.login()) {
 *     is LineLoginResult.Success -> println(result.profile?.displayName)
 *     LineLoginResult.Cancelled -> Unit
 *     is LineLoginResult.Failure -> println(result.message)
 * }
 * ```
 *
 * Underneath, this is LINE's own Android SDK on Android and LINE's own iOS SDK on iOS — the same
 * app-to-app login, the same browser fallback, the same token storage. Only the API is shared.
 *
 * ### Why an object
 *
 * Not a design preference: `LoginManager` on iOS is a process-wide singleton whose setup is
 * one-shot and asserts if called twice. An API that let you construct several clients would be
 * lying about what the platform can do.
 */
public object LineLogin {
    @Volatile
    private var configuration: LineLoginConfig? = null

    /**
     * Serialises logins. Two concurrent attempts would fight over one shared token store on both
     * platforms, and on iOS the second `login` call returns nil and never calls back at all.
     */
    private val loginMutex = Mutex()

    /** True once [configure] has run successfully. */
    public val isConfigured: Boolean
        get() = configuration != null

    /**
     * Prepares the LINE SDK. Call once, as early as possible — `App.onCreate` and your iOS app's
     * initialiser are both fine, and so is any shared startup code that runs before the first
     * login.
     *
     * Calling it again with an equal [config] is a no-op, so it is safe in code that may run more
     * than once.
     *
     * On Android the application `Context` is supplied automatically by an `androidx.startup`
     * initialiser, so there is nothing platform-specific to pass. If your app removes that
     * provider, use the Android-only `configure(context, config)` overload instead.
     *
     * @throws IllegalStateException if called again with a *different* [LineLoginConfig.channelId].
     *   iOS cannot honour it — the underlying SDK refuses to be set up twice — so failing loudly
     *   here beats behaving differently on each platform.
     */
    public fun configure(config: LineLoginConfig) {
        val current = configuration
        if (current == config) return
        check(current == null || current.channelId == config.channelId) {
            "LineLogin is already configured with channel ${current?.channelId} and cannot be " +
                "reconfigured with ${config.channelId}. The iOS LINE SDK only supports one " +
                "channel per process."
        }
        platformConfigure(config)
        configuration = config
    }

    /**
     * Signs the user in, suspending until they finish, cancel, or the attempt fails.
     *
     * Never throws: every failure is a [LineLoginResult.Failure], and backing out is
     * [LineLoginResult.Cancelled]. The only exception that escapes is `CancellationException`,
     * when the calling coroutine itself is cancelled.
     *
     * What cancellation does to the login already on screen differs by platform, and the
     * difference is not one this library can paper over:
     *  - **iOS** stops the login process outright, which dismisses the LINE screen or the web
     *    sheet.
     *  - **Android** closes this library's own invisible Activity, but cannot dismiss LINE's
     *    authentication screen once it is on top. If the user then completes that login, LINE's
     *    SDK stores the token even though this call reported nothing — so a cancelled login can
     *    leave [isLoggedIn] true. Check it after cancelling if that matters to you.
     *
     * Call it while your app is in the foreground. Android does not allow starting an Activity
     * from the background and gives no callback when it refuses, so a background call resolves to
     * [LineLoginErrorCode.Internal] after a ten-second timeout rather than suspending forever.
     *
     * Concurrent calls do not queue: the second one returns
     * [LineLoginErrorCode.LoginInProgress] immediately.
     */
    public suspend fun login(request: LineLoginRequest = LineLoginRequest()): LineLoginResult {
        val config = configuration ?: return NOT_CONFIGURED_LOGIN

        if (!loginMutex.tryLock()) {
            return LineLoginResult.Failure(
                code = LineLoginErrorCode.LoginInProgress,
                message = "A LINE login is already in progress.",
            )
        }
        return try {
            platformLogin(config, request)
        } finally {
            loginMutex.unlock()
        }
    }

    /**
     * Signs the user out: clears the credentials this device holds and revokes the token with
     * LINE.
     *
     * Idempotent, and safe to call when nobody is signed in — with no session to revoke it simply
     * reports [LineLogoutResult.Success].
     *
     * A [LineLogoutResult.Failure] means LINE was not reached, so the token was not revoked
     * server-side. It expires on its own, and the SDK has already dropped its local copy either
     * way, so the user is signed out of your app regardless.
     */
    public suspend fun logout(): LineLogoutResult {
        val config = configuration ?: return NOT_CONFIGURED_LOGOUT
        return platformLogout(config)
    }

    /**
     * The access token this device currently holds, or null if there is none.
     *
     * Read from local storage — this does not contact LINE, and does not prove the token is still
     * valid. It can be expired or revoked; treat a failing LINE API call, not this, as the source
     * of truth.
     */
    public suspend fun currentAccessToken(): LineAccessToken? {
        val config = configuration ?: return null
        return platformCurrentAccessToken(config)
    }

    /**
     * Whether this device holds a token at all.
     *
     * A cheap "has this user signed in before?" check for deciding which screen to show at
     * startup. It says nothing about whether the token still works — see [currentAccessToken].
     */
    public suspend fun isLoggedIn(): Boolean {
        val config = configuration ?: return false
        return platformIsLoggedIn(config)
    }

    /** Resets configuration. Visible for tests in this library only. */
    internal fun resetForTest() {
        configuration = null
    }

    private val NOT_CONFIGURED_MESSAGE =
        "LineLogin.configure(LineLoginConfig(channelId = \"…\")) has not been called."

    private val NOT_CONFIGURED_LOGIN =
        LineLoginResult.Failure(LineLoginErrorCode.NotConfigured, NOT_CONFIGURED_MESSAGE)

    private val NOT_CONFIGURED_LOGOUT =
        LineLogoutResult.Failure(LineLoginErrorCode.NotConfigured, NOT_CONFIGURED_MESSAGE)
}
