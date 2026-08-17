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
import dev.yjyoon.lineloginkmp.LineIdToken
import dev.yjyoon.lineloginkmp.LineLoginErrorCode
import dev.yjyoon.lineloginkmp.LineLoginResult
import dev.yjyoon.lineloginkmp.LineLogoutResult
import dev.yjyoon.lineloginkmp.LineProfile
import dev.yjyoon.lineloginkmp.internal.linesdk.LineSDKAccessToken
import dev.yjyoon.lineloginkmp.internal.linesdk.LineSDKErrorConstant
import dev.yjyoon.lineloginkmp.internal.linesdk.LineSDKLoginResult
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSDate
import platform.Foundation.NSError
import platform.Foundation.timeIntervalSince1970

/**
 * Translates the iOS SDK's Objective-C types into this library's.
 *
 * Split out from the coroutine plumbing for the same reason as its Android counterpart: the
 * mapping is the part worth reading twice, and it should be possible to read it without wading
 * through continuations.
 */
internal fun LineSDKLoginResult.toLineLoginResult(): LineLoginResult {
    val token = accessToken()

    return LineLoginResult.Success(
        accessToken = token.toLineAccessToken(),
        profile =
            userProfile()?.let { profile ->
                LineProfile(
                    userId = profile.userID(),
                    displayName = profile.displayName(),
                    pictureUrl = profile.pictureURL()?.absoluteString,
                    statusMessage = profile.statusMessage(),
                )
            },
        idToken =
            token.IDTokenRaw()?.let { raw ->
                val payload = token.IDToken()?.payload()
                LineIdToken(
                    rawValue = raw,
                    subject = payload?.subject(),
                    name = payload?.name(),
                    pictureUrl = payload?.picture()?.absoluteString,
                    email = payload?.email(),
                    // Not a typed accessor on iOS the way the others are.
                    nonce = payload?.getStringForKey("nonce"),
                )
            },
        friendshipStatusChanged = friendshipStatusChanged()?.boolValue == true,
        nonce = IDTokenNonce(),
    )
}

internal fun LineSDKAccessToken.toLineAccessToken(): LineAccessToken =
    LineAccessToken(
        value = value(),
        expiresAtEpochMilliseconds = expiresAt().toEpochMilliseconds(),
    )

/** Null when the error means the login was cancelled, which is not a failure. */
internal fun NSError.toLoginFailure(): LineLoginResult {
    val errorCode = mapIosError(lineSdkErrorCode()) ?: return LineLoginResult.Cancelled
    return LineLoginResult.Failure(
        code = errorCode,
        message = localizedDescription.ifBlank { defaultMessageFor(errorCode) },
        rawCode = code.toString(),
        rawMessage = localizedDescription,
    )
}

internal fun NSError.toLogoutFailure(): LineLogoutResult {
    // Cancelling is not something logout can report, so an unmapped code falls back to Internal.
    val errorCode = mapIosError(lineSdkErrorCode()) ?: LineLoginErrorCode.Internal
    return LineLogoutResult.Failure(
        code = errorCode,
        message = localizedDescription.ifBlank { defaultMessageFor(errorCode) },
        rawCode = code.toString(),
        rawMessage = localizedDescription,
    )
}

/**
 * The numeric code, but only when this really is a LINE SDK error.
 *
 * Anything from another domain — a URL loading error surfacing untouched, say — would otherwise
 * collide with LINE's numbering by accident. `-1` matches nothing in [mapIosError] and lands on
 * [LineLoginErrorCode.Internal], which is the honest answer for an error this library does not
 * recognise.
 */
private fun NSError.lineSdkErrorCode(): Long = if (domain == LineSDKErrorConstant.errorDomain) code else -1L

private fun NSDate.toEpochMilliseconds(): Long = (timeIntervalSince1970 * MILLISECONDS_PER_SECOND).toLong()

private const val MILLISECONDS_PER_SECOND = 1_000
