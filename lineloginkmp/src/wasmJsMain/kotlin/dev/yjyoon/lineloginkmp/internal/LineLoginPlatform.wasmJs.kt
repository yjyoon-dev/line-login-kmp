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
@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package dev.yjyoon.lineloginkmp.internal

import dev.yjyoon.lineloginkmp.LineAccessToken
import dev.yjyoon.lineloginkmp.LineIdToken
import dev.yjyoon.lineloginkmp.LineLoginConfig
import dev.yjyoon.lineloginkmp.LineLoginErrorCode
import dev.yjyoon.lineloginkmp.LineLoginRequest
import dev.yjyoon.lineloginkmp.LineLoginResult
import dev.yjyoon.lineloginkmp.LineLogoutResult
import dev.yjyoon.lineloginkmp.LineProfile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.await
import kotlinx.coroutines.awaitCancellation

/**
 * The browser runs on LIFF, LINE's official JavaScript SDK — not on the plain OAuth web flow, and
 * the reason is worth recording: LINE's token endpoint requires the channel **secret**
 * (`client_secret` is Required in the API reference, and PKCE only adds `code_verifier` beside it),
 * and a secret shipped in a browser is a secret published. LIFF is LINE's own answer to that —
 * its login completes without the secret ever existing client-side.
 *
 * The prices of that answer, all documented on the public API's KDoc:
 *  - a LIFF app has to exist on the channel, and [LineLoginConfig.liffId] names it;
 *  - `login()` in an external browser is a full-page redirect — the call never resumes, the app
 *    reloads on the way back, and the next `login()` completes without UI;
 *  - `logout()` clears this browser only. LIFF has no client-side revoke.
 */

private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

/**
 * Completed by the initialisation [platformConfigure] starts: null when LIFF is ready, or the
 * failure every later call should report. `configure` cannot suspend — it has to be callable from
 * plain startup code — so it starts this and everything else waits on it.
 */
private var ready: Deferred<LineLoginResult.Failure?>? = null

internal actual fun platformConfigure(config: LineLoginConfig) {
    // configure() has already serialised callers and refused reconfiguration, so plain state is
    // enough here — and the browser has no second thread to race it anyway.
    ready = scope.async { initialiseLiff(config) }
}

private suspend fun initialiseLiff(config: LineLoginConfig): LineLoginResult.Failure? {
    val liffId =
        config.liffId
            ?: return LineLoginResult.Failure(
                code = LineLoginErrorCode.NotConfigured,
                message =
                    "The web target needs LineLoginConfig.liffId: LINE's browser login runs on " +
                        "LIFF, and a channel ID alone does not name a LIFF app. Add a LIFF app to " +
                        "the channel in the LINE Developers Console and pass its ID.",
            )

    try {
        loadLiffScript(LIFF_SDK_URL).await<JsAny?>()
    } catch (failure: Throwable) {
        return LineLoginResult.Failure(
            code = LineLoginErrorCode.Network,
            message = failure.message ?: "Could not load the LIFF SDK from LINE's CDN.",
        )
    }

    return try {
        // On the way back from a login redirect this same call is what completes the login:
        // liff.init consumes the code in the URL and stores the tokens.
        liffInit(liffId).await<JsAny?>()
        null
    } catch (failure: Throwable) {
        val message = failure.message ?: defaultMessageFor(LineLoginErrorCode.Internal)
        LineLoginResult.Failure(
            code = mapLiffInitError(message),
            message = message,
            rawMessage = failure.message,
        )
    }
}

/**
 * LIFF surfaces initialisation failures as an error with a code inside its message rather than a
 * structured value Kotlin can reach reliably, so this matches on the documented code names
 * conservatively: the two shapes that are clearly the app's configuration, and everything else as
 * [LineLoginErrorCode.Internal] rather than a guess.
 */
private fun mapLiffInitError(message: String): LineLoginErrorCode =
    when {
        "INVALID_ARGUMENT" in message || "INVALID_CONFIG" in message -> LineLoginErrorCode.NotConfigured
        "UNAUTHORIZED" in message || "FORBIDDEN" in message -> LineLoginErrorCode.Authentication
        "Failed to fetch" in message || "NetworkError" in message -> LineLoginErrorCode.Network
        else -> LineLoginErrorCode.Internal
    }

/**
 * Null when LIFF is ready; the failure to report otherwise.
 *
 * Deliberately not written with an elvis operator. `ready?.await() ?: failure` collapses the two
 * cases that matter — "configure was never called" and "initialisation finished successfully",
 * which is also a null — into the same branch, so every successful login would report
 * [LineLoginErrorCode.NotConfigured]. It did, until a real login in a browser said so.
 */
private suspend fun awaitReady(): LineLoginResult.Failure? {
    val initialisation =
        ready
            ?: return LineLoginResult.Failure(
                code = LineLoginErrorCode.NotConfigured,
                message = defaultMessageFor(LineLoginErrorCode.NotConfigured),
            )
    return initialisation.await()
}

internal actual suspend fun platformLogin(
    config: LineLoginConfig,
    request: LineLoginRequest,
): LineLoginResult {
    awaitReady()?.let { return it }

    if (!liffIsLoggedIn()) {
        // A full-page redirect to LINE. The page is about to unload, so there is no result to
        // produce and nobody left to receive one — suspending until the page dies is the only
        // truthful behaviour. After LINE redirects back, configure() runs again on the fresh page,
        // liff.init completes the login, and the next login() call returns Success without UI.
        //
        // request is deliberately not consulted: in LIFF the scopes belong to the LIFF app's
        // console registration, and app-to-app versus web is the browser's reality, not an option.
        liffLogin()
        awaitCancellation()
    }

    val tokenValue =
        liffGetAccessToken()
            ?: return LineLoginResult.Failure(
                code = LineLoginErrorCode.Internal,
                message = "LIFF reports a login but holds no access token.",
            )

    // LIFF never says when the token dies, so LINE is asked directly. Failing the login here
    // rather than inventing an expiry keeps LineAccessToken honest — and the user stays logged
    // in, so a retry only repeats this one call.
    val expiresAtEpochMilliseconds =
        try {
            val body = fetchTokenExpiry(tokenValue).await<JsAny?>()
            val expiresInSeconds = body?.let { jsDoubleField(it, "expires_in") } ?: 0.0
            (epochMilliseconds() + expiresInSeconds * 1_000).toLong()
        } catch (failure: Throwable) {
            return LineLoginResult.Failure(
                code = LineLoginErrorCode.Network,
                message =
                    "Signed in, but the token's expiry could not be confirmed with LINE. " +
                        "Calling login() again retries only this step.",
                rawMessage = failure.message,
            )
        }

    // The profile scope may not be granted to the LIFF app, and getProfile throws in that case.
    // A login without a profile is still a login, exactly as on Android and iOS.
    val profile =
        try {
            liffGetProfile().await<JsAny?>()?.let { raw ->
                LineProfile(
                    userId = jsStringField(raw, "userId") ?: return@let null,
                    displayName = jsStringField(raw, "displayName") ?: "",
                    pictureUrl = jsStringField(raw, "pictureUrl"),
                    statusMessage = jsStringField(raw, "statusMessage"),
                )
            }
        } catch (_: Throwable) {
            null
        }

    val decodedIdToken = liffGetDecodedIdToken()
    val idToken =
        liffGetIdToken()?.let { raw ->
            LineIdToken(
                rawValue = raw,
                subject = decodedIdToken?.let { jsStringField(it, "sub") },
                name = decodedIdToken?.let { jsStringField(it, "name") },
                pictureUrl = decodedIdToken?.let { jsStringField(it, "picture") },
                email = decodedIdToken?.let { jsStringField(it, "email") },
                nonce = decodedIdToken?.let { jsStringField(it, "nonce") },
            )
        }

    return LineLoginResult.Success(
        accessToken = LineAccessToken(tokenValue, expiresAtEpochMilliseconds),
        profile = profile,
        idToken = idToken,
        // LIFF has no bot-prompt flow, so there is no friendship change to report.
        friendshipStatusChanged = false,
        nonce = decodedIdToken?.let { jsStringField(it, "nonce") },
    )
}

internal actual suspend fun platformLogout(config: LineLoginConfig): LineLogoutResult {
    awaitReady()?.let { failure ->
        return LineLogoutResult.Failure(failure.code, failure.message, failure.rawCode, failure.rawMessage)
    }
    // Clears this browser's tokens only. LIFF offers no client-side revoke, so — unlike Android
    // and iOS — the grant itself survives, which LineLogin.logout's KDoc spells out.
    liffLogout()
    return LineLogoutResult.Success
}

internal actual suspend fun platformCurrentAccessToken(config: LineLoginConfig): LineAccessToken? {
    if (awaitReady() != null) return null
    if (!liffIsLoggedIn()) return null
    val tokenValue = liffGetAccessToken() ?: return null

    // LIFF does not persist an expiry, so — alone among the platforms, and noted on the public
    // KDoc — this read asks LINE. A token whose expiry cannot be confirmed is reported as absent
    // rather than guessed at.
    return try {
        val body = fetchTokenExpiry(tokenValue).await<JsAny?>()
        val expiresInSeconds = body?.let { jsDoubleField(it, "expires_in") } ?: return null
        LineAccessToken(tokenValue, (epochMilliseconds() + expiresInSeconds * 1_000).toLong())
    } catch (_: Throwable) {
        null
    }
}

internal actual suspend fun platformIsLoggedIn(config: LineLoginConfig): Boolean = awaitReady() == null && liffIsLoggedIn()

internal actual suspend fun platformIsLineAppInstalled(): Boolean {
    // Knowable only from inside the LINE app's own browser. An external browser cannot see the
    // device's app list, so false is the honest answer there — and before configure() has loaded
    // the SDK there is nothing to ask at all.
    val initialised = ready?.let { it.isCompleted && it.await() == null } ?: false
    return initialised && liffIsInClient()
}
