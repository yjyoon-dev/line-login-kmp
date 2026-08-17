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

import dev.yjyoon.lineloginkmp.LineLoginErrorCode

// Translation from each native SDK's error vocabulary into LineLoginErrorCode.
//
// Pure functions over primitives, deliberately: this is the part of the library most likely to be
// wrong and least likely to be exercised by hand, so it is kept free of platform types and
// table-tested in commonTest. The platform layers do nothing but pull the primitive out of the
// SDK's error object and call in here.
//
// In both mappers, a null return means *cancelled* — not an error, and not something to report.

/**
 * Maps a `LineApiResponseCode` name, plus the `LineApiError.ErrorCode` name when one is present.
 *
 * Names rather than the enums themselves, so this stays in common code. Android's SDK has exactly
 * six response codes and three error codes; anything unrecognised lands on
 * [LineLoginErrorCode.Internal] rather than throwing, because a new SDK version must not be able
 * to crash a login.
 */
internal fun mapAndroidError(
    responseCode: String,
    apiErrorCode: String? = null,
): LineLoginErrorCode? {
    // Checked first: it is the machine-readable "no LINE app and no browser could handle this",
    // and it arrives underneath an otherwise uninformative INTERNAL_ERROR.
    if (apiErrorCode == ANDROID_LOGIN_ACTIVITY_NOT_FOUND) return LineLoginErrorCode.LineAppUnavailable

    return when (responseCode) {
        "CANCEL" -> null
        "NETWORK_ERROR" -> LineLoginErrorCode.Network
        "SERVER_ERROR" -> LineLoginErrorCode.Server
        // Practically always a package name or signing-certificate mismatch against the console.
        "AUTHENTICATION_AGENT_ERROR" -> LineLoginErrorCode.Authentication
        else -> LineLoginErrorCode.Internal
    }
}

/**
 * Maps `LineSDKError.errorCode`.
 *
 * The numbering is the SDK's own: 1xxx building the request, 2xxx handling the response, 3xxx
 * authorising, 4xxx general. The full table is spelled out rather than collapsed into ranges,
 * because several codes inside a range mean something categorically different from their
 * neighbours — 3003 and 3004 are cancellations sitting in the middle of the authorisation errors.
 */
@Suppress("MagicNumber")
internal fun mapIosError(errorCode: Long): LineLoginErrorCode? =
    when (errorCode) {
        // ── Cancellation, not failure ──────────────────────────────────────────────────────
        3003L, // userCancelled — the user dismissed the LINE app or the web login sheet.
        3004L, // forceStopped — LoginProcess.stop(), which is how this library cancels.
        4004L, // processDiscarded
        4005L, // loginManagerReset
        -> null

        // ── Reachability ───────────────────────────────────────────────────────────────────
        2001L -> LineLoginErrorCode.Network // URLSessionError

        // ── LINE answered, but badly ───────────────────────────────────────────────────────
        2002L, // nonHTTPURLResponse
        2003L, // dataParsingFailed
        2004L, // invalidHTTPStatusAPIError
        3014L, // lackOfIDToken — openid was granted but no ID token came back
        3015L, // JWTPublicKeyNotFound — LINE's JWKS did not carry the signing key
        -> LineLoginErrorCode.Server

        // ── No way to present a login at all ───────────────────────────────────────────────
        3001L -> LineLoginErrorCode.LineAppUnavailable // exhaustedLoginFlow

        // ── The app's LINE registration or URL wiring does not match the console ───────────
        3005L, // callbackURLSchemeNotMatching
        3006L, // invalidSourceApplication
        3007L, // malformedRedirectURL
        3008L, // invalidLineURLResultCode
        3009L, // lineClientError
        3010L, // responseStateValueNotMatching
        3011L, // webLoginError
        -> LineLoginErrorCode.Authentication

        // ── Everything else: 1xxx request building, 3002 view hierarchy, 3012/3013 keychain,
        //    3016 crypto, 4001–4003 and 4006 general ────────────────────────────────────────
        else -> LineLoginErrorCode.Internal
    }

/** `LineApiError.ErrorCode.LOGIN_ACTIVITY_NOT_FOUND`, as a name so common code can match it. */
internal const val ANDROID_LOGIN_ACTIVITY_NOT_FOUND: String = "LOGIN_ACTIVITY_NOT_FOUND"

/**
 * A readable fallback for when the SDK reports a failure with no message of its own, which both
 * of them do more often than you would hope.
 */
internal fun defaultMessageFor(code: LineLoginErrorCode): String =
    when (code) {
        LineLoginErrorCode.NotConfigured ->
            "LINE Login is not configured. Call LineLogin.configure(...) before logging in."
        LineLoginErrorCode.Network ->
            "Could not reach LINE. Check the device's network connection."
        LineLoginErrorCode.Server ->
            "LINE returned an error."
        LineLoginErrorCode.Authentication ->
            "LINE rejected this app. Check that the package name, signing certificate and bundle " +
                "ID registered in the LINE Developers Console match this build, and that the " +
                "channel is published."
        LineLoginErrorCode.LineAppUnavailable ->
            "Neither the LINE app nor a browser could handle the login on this device."
        LineLoginErrorCode.LoginInProgress ->
            "A LINE login is already in progress."
        LineLoginErrorCode.Internal ->
            "LINE Login failed."
    }
