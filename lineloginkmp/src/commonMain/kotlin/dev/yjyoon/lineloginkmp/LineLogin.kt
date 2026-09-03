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
import dev.yjyoon.lineloginkmp.internal.platformIsLineAppInstalled
import dev.yjyoon.lineloginkmp.internal.platformIsLoggedIn
import dev.yjyoon.lineloginkmp.internal.platformLogin
import dev.yjyoon.lineloginkmp.internal.platformLogout
import dev.yjyoon.lineloginkmp.internal.platformResumePendingLogin
import dev.yjyoon.lineloginkmp.internal.withLineLoginLock
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
        // Locked, not merely volatile: validating, setting the platform SDK up and recording the
        // result have to be one step. See withLineLoginLock for what interleaving them costs.
        withLineLoginLock {
            val current = configuration
            if (current == config) return@withLineLoginLock
            check(current == null || current.channelId == config.channelId) {
                "LineLogin is already configured with channel ${current?.channelId} and cannot be " +
                    "reconfigured with ${config.channelId}. The iOS LINE SDK only supports one " +
                    "channel per process."
            }
            platformConfigure(config)
            configuration = config
        }
    }

    /**
     * Signs the user in, suspending until they finish, cancel, or the attempt fails.
     *
     * Never throws: every failure is a [LineLoginResult.Failure], and backing out is
     * [LineLoginResult.Cancelled]. The only exception that escapes is `CancellationException`,
     * when the calling coroutine itself is cancelled.
     *
     * ### Web (wasmJs)
     *
     * In a browser this is a **full-page redirect**, not a dialog: when nobody is signed in, the
     * call navigates to LINE and never resumes — the page unloads underneath it. LINE then
     * redirects back, the app starts fresh, and [configure] completes the login as it initialises —
     * at which point the result has nobody left to go to. **Call [resumePendingLogin] at startup to
     * receive it.** Without that, the app shows its login screen again and a second press is what
     * appears to work, because the tokens were already there.
     *
     * A stored session short-circuits all of that and returns immediately. [request] is ignored
     * on this platform: in LIFF the scopes belong to the LIFF app's console registration.
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
     *
     * On **web** the revocation half does not exist: LIFF clears this browser's tokens and offers
     * no client-side revoke, so the grant itself survives until it expires or the user removes it
     * from their LINE account settings.
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
     *
     * **Web is the exception:** LIFF stores no expiry, so this call asks LINE for it — one network
     * round trip — and reports a token whose expiry cannot be confirmed as absent.
     */
    public suspend fun currentAccessToken(): LineAccessToken? {
        val config = configuration ?: return null
        return platformCurrentAccessToken(config)
    }

    /**
     * The login LINE completed while your app was not running, or null when there is none.
     *
     * **The web needs this; Android and iOS return null.** In a browser [login] is a full-page
     * redirect: the page unloads, LINE authenticates, and the user comes back to a *fresh* app whose
     * `login()` call no longer exists. [configure] hands the returning URL to LIFF, which completes
     * the login and stores the tokens — but the result has nobody left to go to. This is where it
     * goes.
     *
     * Call it at startup, after [configure], when your app has no session of its own:
     *
     * ```
     * LineLogin.configure(LineLoginConfig(channelId = "…", liffId = "…"))
     *
     * val result = restoreMySession() ?: LineLogin.resumePendingLogin()
     * ```
     *
     * Skip it and the app shows its login screen again after a successful login — and pressing the
     * button a *second* time appears to fix it, because the tokens were already there and pressing
     * merely asked for them. That symptom is what this exists to remove.
     *
     * Never presents UI and never redirects: it either has a completed login to hand over or it
     * does not. A [LineLoginResult.Failure] is never returned — a startup call the user did not ask
     * for has no business reporting errors, so an initialisation that failed reads as null and the
     * error surfaces from the [login] the user goes on to attempt.
     *
     * On Android and iOS this is always null, deliberately, because nothing is ever pending there:
     * those logins resume the coroutine that started them. The reason for not reporting a *stored*
     * session instead differs by platform, and only one of them is a limitation — on Android the SDK
     * exposes the access token but never the ID token outside a login result, so a Success would
     * have to omit the one field a backend verifies; on iOS the token store does keep the raw ID
     * token, and the null is a choice: a session persisted from some earlier launch is not a login
     * performed on this one, and an app that wants it can ask [isLoggedIn] and say so itself.
     */
    public suspend fun resumePendingLogin(): LineLoginResult.Success? {
        val config = configuration ?: return null
        return platformResumePendingLogin(config) as? LineLoginResult.Success
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

    /**
     * Whether the LINE app is installed on this device.
     *
     * For deciding what to *show*, not whether to offer a login: [login] works either way, going
     * app-to-app when LINE is present and falling back to a browser when it is not. Use it to label
     * a button, to explain the flow the user is about to see, or to record which path a failing
     * login took.
     *
     * Needs no configuration, so it may be called before [configure].
     *
     * On **iOS** this requires `lineauth2` in your `Info.plist` under
     * `LSApplicationQueriesSchemes` — the same entry app-to-app login needs. Without it iOS answers
     * false on every device, so a false here on a phone that visibly has LINE means that key is
     * missing rather than that LINE is.
     *
     * On **Android** it uses the application `Context` this library picks up through
     * `androidx.startup`. If your app strips that provider, use the Android-only
     * `isLineAppInstalled(context)` overload instead, which takes one directly.
     *
     * On **web** a page cannot see the device's app list, so this answers true only when the page
     * is running inside the LINE app's own browser, and false everywhere else — including before
     * [configure] has loaded the SDK.
     */
    public suspend fun isLineAppInstalled(): Boolean = platformIsLineAppInstalled()

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
