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

import com.linecorp.linesdk.LineApiError
import com.linecorp.linesdk.LineApiResponse
import com.linecorp.linesdk.LineApiResponseCode
import dev.yjyoon.lineloginkmp.LineAccessToken
import dev.yjyoon.lineloginkmp.LineIdToken
import dev.yjyoon.lineloginkmp.LineLoginErrorCode
import dev.yjyoon.lineloginkmp.LineLoginResult
import dev.yjyoon.lineloginkmp.LineLogoutResult
import dev.yjyoon.lineloginkmp.LineProfile
import com.linecorp.linesdk.LineIdToken as SdkIdToken
import com.linecorp.linesdk.LineProfile as SdkProfile
import com.linecorp.linesdk.auth.LineLoginResult as SdkLoginResult

/**
 * Translates the Android SDK's types into this library's.
 *
 * Kept apart from the Activity and the coroutine plumbing so it can be exercised on the JVM with
 * real SDK objects — `LineLoginResult` exposes public factories for every response code, which
 * makes the whole table testable without a device.
 */
internal fun SdkLoginResult.toLineLoginResult(): LineLoginResult {
    if (responseCode == LineApiResponseCode.SUCCESS) {
        val accessToken =
            lineCredential?.accessToken
                ?: return LineLoginResult.Failure(
                    code = LineLoginErrorCode.Internal,
                    message = "LINE reported a successful login without an access token.",
                    rawCode = responseCode.name,
                )

        return LineLoginResult.Success(
            accessToken =
                LineAccessToken(
                    value = accessToken.tokenString,
                    expiresAtEpochMilliseconds = accessToken.estimatedExpirationTimeMillis,
                ),
            profile = lineProfile?.toLineProfile(),
            idToken = lineIdToken?.toLineIdToken(),
            // The getter is typed java.lang.Boolean but is documented — and implemented — to
            // return false rather than null when no bot prompt was requested.
            friendshipStatusChanged = friendshipStatusChanged == true,
            nonce = nonce,
        )
    }

    val code =
        mapAndroidError(responseCode.name, errorData.errorCode.name)
            ?: return LineLoginResult.Cancelled

    return LineLoginResult.Failure(
        code = code,
        message = errorData.readableMessage() ?: defaultMessageFor(code),
        rawCode = responseCode.name,
        rawMessage = errorData.message,
    )
}

internal fun LineApiResponse<*>.toLineLogoutResult(): LineLogoutResult {
    if (isSuccess) return LineLogoutResult.Success

    // A cancel is not a thing a logout can produce, so a null mapping falls back to Internal.
    val code = mapAndroidError(responseCode.name, errorData.errorCode.name) ?: LineLoginErrorCode.Internal
    return LineLogoutResult.Failure(
        code = code,
        message = errorData.readableMessage() ?: defaultMessageFor(code),
        rawCode = responseCode.name,
        rawMessage = errorData.message,
    )
}

private fun SdkProfile.toLineProfile(): LineProfile =
    LineProfile(
        userId = userId,
        displayName = displayName,
        pictureUrl = pictureUrl?.toString(),
        statusMessage = statusMessage,
    )

private fun SdkIdToken.toLineIdToken(): LineIdToken =
    LineIdToken(
        rawValue = rawString,
        subject = subject,
        name = name,
        // Unlike LineProfile.pictureUrl, this one is already a String on Android.
        pictureUrl = picture,
        email = email,
        nonce = nonce,
    )

/** The SDK fills this with an empty string more often than with anything useful. */
private fun LineApiError.readableMessage(): String? = message?.takeIf { it.isNotBlank() }
