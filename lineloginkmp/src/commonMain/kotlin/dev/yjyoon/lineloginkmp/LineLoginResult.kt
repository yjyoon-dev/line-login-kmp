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

/**
 * The outcome of [LineLogin.login].
 *
 * Modelled as a sealed type rather than an exception or a `Result`, because backing out of a
 * login is not an error and should not be handled like one:
 *
 * ```
 * when (val result = LineLogin.login()) {
 *     is LineLoginResult.Success -> signIn(result)
 *     LineLoginResult.Cancelled -> Unit          // the user changed their mind; say nothing
 *     is LineLoginResult.Failure -> showError(result.message)
 * }
 * ```
 */
public sealed interface LineLoginResult {
    /**
     * The user signed in.
     *
     * Which permissions were actually granted is not reported as a list, because iOS cannot
     * report it: the LINE SDK's Objective-C layer exposes login permissions as opaque objects
     * with no readable value and no equality, so any such list would be honest on one platform
     * and invented on the other. Read it off the data instead — [profile] is non-null when
     * `profile` was granted, [idToken] when `openid` was, and [LineIdToken.email] when `email`
     * was.
     *
     * @property accessToken Always present.
     * @property profile Present when [LineScope.Profile] was granted.
     * @property idToken Present when [LineScope.OpenId] was granted. The only part of this result
     *   a backend can verify.
     * @property friendshipStatusChanged True when the user added your LINE Official Account as a
     *   friend during this login. Only meaningful with [LineLoginRequest.botPrompt].
     * @property nonce The nonce carried by this login, whether it came from
     *   [LineLoginRequest.nonce] or from the SDK.
     */
    public class Success(
        public val accessToken: LineAccessToken,
        public val profile: LineProfile?,
        public val idToken: LineIdToken?,
        public val friendshipStatusChanged: Boolean,
        public val nonce: String?,
    ) : LineLoginResult {
        override fun toString(): String = "LineLoginResult.Success(profile=$profile, friendshipStatusChanged=$friendshipStatusChanged)"
    }

    /**
     * The user dismissed the LINE login screen, or a login already in flight was cancelled.
     *
     * Not an error: nothing went wrong, and there is nothing to report. Showing a message here is
     * the most common way an integration feels broken.
     */
    public data object Cancelled : LineLoginResult

    /**
     * The login could not be completed.
     *
     * @property code A coarse category, stable across both platforms — switch on this.
     * @property message A human-readable description, in English. Suitable for logs and bug
     *   reports, not for end users.
     * @property rawCode The native SDK's own code, verbatim: the `LineApiResponseCode` name on
     *   Android, the `LineSDKError` numeric code on iOS. Preserved so you can react to something
     *   this library has not categorised yet, without waiting for a release.
     * @property rawMessage The native SDK's own message, verbatim.
     */
    public class Failure(
        public val code: LineLoginErrorCode,
        public val message: String,
        public val rawCode: String? = null,
        public val rawMessage: String? = null,
    ) : LineLoginResult {
        override fun toString(): String = "LineLoginResult.Failure(code=$code, message=$message, rawCode=$rawCode, rawMessage=$rawMessage)"
    }
}

/**
 * The outcome of [LineLogin.logout].
 *
 * A [Failure] means LINE was not reached, so the token was not revoked server-side — the user is
 * signed out of your app either way, because the SDK drops its local copy regardless.
 */
public sealed interface LineLogoutResult {
    /**
     * The session is gone: the token was revoked with LINE, or there was no session to revoke in
     * the first place.
     */
    public data object Success : LineLogoutResult

    /**
     * Revoking the token with LINE failed — usually because the device is offline. The token
     * expires on its own; retrying is optional.
     */
    public class Failure(
        public val code: LineLoginErrorCode,
        public val message: String,
        public val rawCode: String? = null,
        public val rawMessage: String? = null,
    ) : LineLogoutResult {
        override fun toString(): String = "LineLogoutResult.Failure(code=$code, message=$message, rawCode=$rawCode, rawMessage=$rawMessage)"
    }
}

/**
 * Why a LINE call failed, in terms that mean the same thing on Android and iOS.
 *
 * Native SDK codes are mapped onto these; the original is always available as
 * [LineLoginResult.Failure.rawCode].
 */
public enum class LineLoginErrorCode {
    /**
     * [LineLogin.configure] was never called, or was called with something the SDK rejected.
     *
     * On Android this also covers the app having removed the `androidx.startup` provider that
     * supplies the application `Context`; call `LineLogin.configure(context, config)` instead.
     */
    NotConfigured,

    /** The device could not reach LINE. Retrying later is reasonable. */
    Network,

    /** LINE answered, but with an error. */
    Server,

    /**
     * LINE refused this app's registration.
     *
     * In practice this nearly always means the LINE Developers Console does not match the app
     * that is running: a missing or wrong package name, a signing certificate whose SHA-1 is not
     * registered, a bundle ID mismatch on iOS, or a channel that has not been published. It is a
     * configuration problem, not a runtime one — the same build fails every time.
     */
    Authentication,

    /** Neither the LINE app nor a browser could handle the login request on this device. */
    LineAppUnavailable,

    /** A login is already running. Wait for it, or cancel it, before starting another. */
    LoginInProgress,

    /** Anything else. [LineLoginResult.Failure.rawCode] carries what the SDK actually said. */
    Internal,
}
