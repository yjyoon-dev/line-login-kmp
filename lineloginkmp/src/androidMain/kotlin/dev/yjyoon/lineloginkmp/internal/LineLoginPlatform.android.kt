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
package dev.yjyoon.lineloginkmp.internal

import android.content.Context
import android.content.pm.PackageManager
import com.linecorp.linesdk.Constants
import com.linecorp.linesdk.api.LineApiClient
import com.linecorp.linesdk.api.LineApiClientBuilder
import dev.yjyoon.lineloginkmp.LineAccessToken
import dev.yjyoon.lineloginkmp.LineLoginConfig
import dev.yjyoon.lineloginkmp.LineLoginErrorCode
import dev.yjyoon.lineloginkmp.LineLoginRequest
import dev.yjyoon.lineloginkmp.LineLoginResult
import dev.yjyoon.lineloginkmp.LineLogoutResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.ConcurrentHashMap

/**
 * How long to wait for [LineLoginProxyActivity] to reach `onCreate` after `startActivity`.
 *
 * There is no callback for "the system refused to start your Activity". Android silently drops
 * activity starts from the background, so without this a `login()` called from the background
 * would suspend forever, holding its caller's coroutine — and everything that coroutine
 * references — alive with no way out. Ten seconds is far longer than a local Activity start ever
 * takes and short enough to surface as a bug rather than a hang.
 */
private const val ACTIVITY_START_TIMEOUT_MILLISECONDS = 10_000L

private const val MISSING_CONTEXT_MESSAGE =
    "No Android Context is available. This library reads one through androidx.startup; if your " +
        "app removes androidx.startup.InitializationProvider from its manifest, call " +
        "LineLogin.configure(context, config) instead."

/**
 * The Android SDK's entry point for everything except login itself. Cached per channel because
 * building one parses the app's manifest metadata, and discarded state is not free.
 *
 * It captures the *application* context internally, so holding it costs nothing.
 */
private val apiClients = ConcurrentHashMap<String, LineApiClient>()

private fun apiClientFor(
    context: Context,
    channelId: String,
): LineApiClient =
    // computeIfAbsent, not getOrPut: the latter is the plain MutableMap extension — a get, then a
    // put — so two threads reaching a channel for the first time would each build a client.
    apiClients.computeIfAbsent(channelId) {
        LineApiClientBuilder(context.applicationContext, channelId).build()
    }

internal actual fun platformConfigure(config: LineLoginConfig) {
    // Deliberately does nothing and deliberately does not throw.
    //
    // There is nothing to set up ahead of time on Android — the channel ID travels with each
    // call. Checking for the Context here looks tempting, because it would catch a stripped
    // androidx.startup provider early, but `androidx.startup.InitializationProvider` is only
    // instantiated in the app's *default* process. An app with a secondary process (a media
    // service, a WebView process, a crash handler) runs Application.onCreate there too, and
    // throwing would turn a configure() call that works everywhere else into a crash loop in that
    // process. The missing Context surfaces as a typed Failure(NotConfigured) from login()
    // instead, which is what the documentation promises.
}

internal actual suspend fun platformLogin(
    config: LineLoginConfig,
    request: LineLoginRequest,
): LineLoginResult {
    val context =
        LineLoginContext.peek()
            ?: return LineLoginResult.Failure(LineLoginErrorCode.NotConfigured, MISSING_CONTEXT_MESSAGE)

    val (requestId, pending) = LineLoginRequestBus.open()
    try {
        try {
            context.startActivity(
                LineLoginProxyActivity.intent(context, requestId, config.channelId, request),
            )
        } catch (error: Exception) {
            return LineLoginResult.Failure(
                code = LineLoginErrorCode.Internal,
                message = error.message ?: "Could not start the LINE login screen.",
            )
        }

        val started =
            withTimeoutOrNull(ACTIVITY_START_TIMEOUT_MILLISECONDS) { pending.started.await() }
        if (started == null) {
            return LineLoginResult.Failure(
                code = LineLoginErrorCode.Internal,
                message =
                    "The LINE login screen did not start. Android does not allow starting an " +
                        "Activity from the background — call LineLogin.login() while your app is " +
                        "in the foreground.",
            )
        }

        return pending.outcome.await()
    } catch (cancellation: CancellationException) {
        // Close our own invisible Activity. If LINE's screen is already on top it stays until the
        // user dismisses it; the result it eventually produces is discarded with this request.
        pending.finishProxy()
        throw cancellation
    } finally {
        LineLoginRequestBus.close(requestId)
    }
}

internal actual suspend fun platformLogout(config: LineLoginConfig): LineLogoutResult {
    val context =
        LineLoginContext.peek()
            ?: return LineLogoutResult.Failure(LineLoginErrorCode.NotConfigured, MISSING_CONTEXT_MESSAGE)

    // Every LineApiClient call is a blocking network call and throws NetworkOnMainThreadException
    // if run on the main thread.
    return withContext(Dispatchers.IO) {
        val client = apiClientFor(context, config.channelId)

        // Logging out with no session is a no-op, not a failure. The SDK disagrees: it reports
        // Failure("access token is null"), which would make the documented "idempotent, and safe
        // to call when nobody is signed in" contract false, and would differ from iOS.
        if (!client.currentAccessToken.isSuccess) return@withContext LineLogoutResult.Success

        client.logout().toLineLogoutResult()
    }
}

internal actual suspend fun platformCurrentAccessToken(config: LineLoginConfig): LineAccessToken? {
    val context = LineLoginContext.peek() ?: return null
    return withContext(Dispatchers.IO) {
        val response = apiClientFor(context, config.channelId).currentAccessToken
        if (!response.isSuccess) return@withContext null
        val token = response.responseData
        LineAccessToken(
            value = token.tokenString,
            expiresAtEpochMilliseconds = token.estimatedExpirationTimeMillis,
        )
    }
}

internal actual suspend fun platformIsLoggedIn(config: LineLoginConfig): Boolean = platformCurrentAccessToken(config) != null

internal actual suspend fun platformIsLineAppInstalled(): Boolean {
    // No Context means no way to ask the package manager. False rather than a thrown exception:
    // this is a "which button should I draw" check, and login works either way.
    val context = LineLoginContext.peek() ?: return false
    return isLineAppInstalledIn(context)
}

/**
 * Needs no `<queries>` entry from the consumer: the LINE SDK's own manifest already declares
 * `jp.naver.line.android` for Android 11+ package visibility, and manifest merging brings it in.
 */
internal fun isLineAppInstalledIn(context: Context): Boolean =
    try {
        context.packageManager.getPackageInfo(Constants.LINE_APP_PACKAGE_NAME, 0)
        true
    } catch (notInstalled: PackageManager.NameNotFoundException) {
        false
    }
