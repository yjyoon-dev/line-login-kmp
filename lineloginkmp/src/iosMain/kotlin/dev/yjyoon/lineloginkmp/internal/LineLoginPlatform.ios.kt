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
@file:OptIn(ExperimentalForeignApi::class)

package dev.yjyoon.lineloginkmp.internal

import dev.yjyoon.lineloginkmp.LineAccessToken
import dev.yjyoon.lineloginkmp.LineBotPrompt
import dev.yjyoon.lineloginkmp.LineLoginConfig
import dev.yjyoon.lineloginkmp.LineLoginErrorCode
import dev.yjyoon.lineloginkmp.LineLoginRequest
import dev.yjyoon.lineloginkmp.LineLoginResult
import dev.yjyoon.lineloginkmp.LineLogoutResult
import dev.yjyoon.lineloginkmp.internal.linesdk.LineSDKAccessTokenStore
import dev.yjyoon.lineloginkmp.internal.linesdk.LineSDKLoginManager
import dev.yjyoon.lineloginkmp.internal.linesdk.LineSDKLoginManagerBotPrompt
import dev.yjyoon.lineloginkmp.internal.linesdk.LineSDKLoginManagerParameters
import dev.yjyoon.lineloginkmp.internal.linesdk.LineSDKLoginPermission
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import platform.Foundation.NSURL
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue
import kotlin.coroutines.resume

/**
 * iOS runs entirely on `LineSDKObjC`, LINE's own Objective-C compatibility layer, bound through
 * cinterop. There is no Swift bridge and nothing for the consumer to implement: Kotlin owns the
 * whole flow, which is also what makes cancellation work properly — the coroutine that started
 * the login is the one holding the `LoginProcess` it has to stop.
 */

private const val SETUP_MISSING_MESSAGE =
    "The LINE SDK is not set up. Call LineLogin.configure(LineLoginConfig(channelId = \"…\")) first."

private val loginManager: LineSDKLoginManager
    get() = LineSDKLoginManager.sharedManager()

internal actual fun platformConfigure(config: LineLoginConfig) {
    // The SDK raises an assertion failure — fatal in debug builds — if setup runs twice, and the
    // host app may have called it itself before reaching shared code.
    if (loginManager.isSetupFinished()) return

    loginManager.setupWithChannelID(
        channelID = config.channelId,
        universalLinkURL = config.universalLinkUrl?.let { NSURL(string = it) },
    )
}

internal actual suspend fun platformLogin(
    config: LineLoginConfig,
    request: LineLoginRequest,
): LineLoginResult =
    // `login` presents UIKit and is main-actor isolated in the SDK. The explicit type argument
    // stops inference from narrowing the whole block to the first branch's Failure type.
    withContext<LineLoginResult>(Dispatchers.Main) {
        if (!loginManager.isSetupFinished()) {
            return@withContext LineLoginResult.Failure(
                code = LineLoginErrorCode.NotConfigured,
                message = SETUP_MISSING_MESSAGE,
            )
        }

        suspendCancellableCoroutine<LineLoginResult> { continuation ->
            val process =
                loginManager.loginWithPermissions(
                    permissions = request.scopes.mapTo(mutableSetOf()) { LineSDKLoginPermission(it.code) },
                    // Null lets the SDK present from the topmost view controller, which is more
                    // robust than walking the hierarchy from a library that knows nothing about
                    // the host app's structure.
                    inViewController = null,
                    parameters = request.toSdkParameters(),
                ) { result, error ->
                    if (!continuation.isActive) return@loginWithPermissions
                    continuation.resume(
                        when {
                            result != null -> result.toLineLoginResult()
                            error != null -> error.toLoginFailure()
                            else ->
                                LineLoginResult.Failure(
                                    code = LineLoginErrorCode.Internal,
                                    message = "The LINE SDK reported neither a result nor an error.",
                                )
                        },
                    )
                }

            if (process == null) {
                // The SDK returns nil — and never calls the completion handler — when a login is
                // already running. Without this branch the coroutine would suspend forever.
                continuation.resume(
                    LineLoginResult.Failure(
                        code = LineLoginErrorCode.LoginInProgress,
                        message = "A LINE login is already in progress.",
                    ),
                )
                return@suspendCancellableCoroutine
            }

            if (!continuation.isActive) {
                // The completion handler already fired, synchronously, from inside the call above.
                // The SDK can do that — a login with no view controller to present from fails
                // immediately — and it installs `currentProcess` only *after* the process has
                // started. So a dead process is now sitting in the login manager, `isAuthorizing`
                // is stuck true, and every later login would return nil and report
                // LoginInProgress for the rest of the app's lifetime.
                //
                // Stopping it makes the SDK clear the reference. The resulting `forceStopped`
                // callback is ignored: this continuation is already resumed.
                loginManager.currentProcess()?.stop()
                return@suspendCancellableCoroutine
            }

            continuation.invokeOnCancellation {
                // Runs on whichever thread cancelled us; `stop` touches UIKit. Stopping the
                // process dismisses the login UI and fails it with `forceStopped`, which the
                // completion handler ignores because the continuation is no longer active.
                dispatch_async(dispatch_get_main_queue()) { process.stop() }
            }
        }
    }

internal actual suspend fun platformLogout(config: LineLoginConfig): LineLogoutResult {
    if (!loginManager.isSetupFinished()) {
        return LineLogoutResult.Failure(
            code = LineLoginErrorCode.NotConfigured,
            message = SETUP_MISSING_MESSAGE,
        )
    }

    return suspendCancellableCoroutine { continuation ->
        loginManager.logoutWithCompletionHandler { error ->
            if (!continuation.isActive) return@logoutWithCompletionHandler
            continuation.resume(error?.toLogoutFailure() ?: LineLogoutResult.Success)
        }
    }
}

internal actual suspend fun platformCurrentAccessToken(config: LineLoginConfig): LineAccessToken? {
    // AccessTokenStore.shared traps if the SDK was never set up.
    if (!loginManager.isSetupFinished()) return null
    return LineSDKAccessTokenStore.sharedStore().currentToken()?.toLineAccessToken()
}

internal actual suspend fun platformIsLoggedIn(config: LineLoginConfig): Boolean =
    loginManager.isSetupFinished() && loginManager.isAuthorized()

private fun LineLoginRequest.toSdkParameters(): LineSDKLoginManagerParameters =
    LineSDKLoginManagerParameters().apply {
        setOnlyWebLogin(forceWebLogin)
        setIDTokenNonce(nonce)
        setPromptBotID(promptBotId)
        setBotPromptStyle(
            when (botPrompt) {
                LineBotPrompt.Normal -> LineSDKLoginManagerBotPrompt.normal()
                LineBotPrompt.Aggressive -> LineSDKLoginManagerBotPrompt.aggressive()
                null -> null
            },
        )
    }
