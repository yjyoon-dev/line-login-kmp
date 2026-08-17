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
import com.linecorp.linesdk.LineCredential
import com.linecorp.linesdk.Scope
import dev.yjyoon.lineloginkmp.LineLoginErrorCode
import dev.yjyoon.lineloginkmp.LineLoginResult
import dev.yjyoon.lineloginkmp.LineLogoutResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import com.linecorp.linesdk.LineAccessToken as SdkAccessToken
import com.linecorp.linesdk.LineIdToken as SdkIdToken
import com.linecorp.linesdk.LineProfile as SdkProfile
import com.linecorp.linesdk.auth.LineLoginResult as SdkLoginResult

/**
 * Mapping asserted against **real** LINE SDK objects rather than hand-rolled fakes.
 *
 * The SDK exposes public builders and error factories, so the full result matrix can be built on
 * a plain JVM. That matters: this mapping is otherwise only reachable by actually logging in.
 */
class AndroidResultMappingTest {
    @Test
    fun successCarriesTokenProfileAndIdTokenThrough() {
        val result =
            SdkLoginResult
                .Builder()
                .responseCode(LineApiResponseCode.SUCCESS)
                .lineCredential(
                    LineCredential(
                        SdkAccessToken("access-token", EXPIRES_IN_MILLISECONDS, ISSUED_AT_MILLISECONDS),
                        listOf(Scope.PROFILE, Scope.OPENID_CONNECT),
                    ),
                )
                // pictureUrl stays null: android.net.Uri is a stub on the JVM, and the mapping
                // only ever calls toString() on it.
                .lineProfile(SdkProfile("U0123456789abcdef", "Yeojun", null, "Hello"))
                .lineIdToken(
                    SdkIdToken
                        .Builder()
                        .rawString("header.payload.signature")
                        .subject("U0123456789abcdef")
                        .name("Yeojun")
                        .picture("https://example.com/p.jpg")
                        .email("yjyoon@example.com")
                        .nonce("nonce-value")
                        .build(),
                ).friendshipStatusChanged(true)
                .nonce("nonce-value")
                .build()
                .toLineLoginResult()

        val success = assertIs<LineLoginResult.Success>(result)
        assertEquals("access-token", success.accessToken.value)
        assertEquals(
            ISSUED_AT_MILLISECONDS + EXPIRES_IN_MILLISECONDS,
            success.accessToken.expiresAtEpochMilliseconds,
        )
        assertEquals("U0123456789abcdef", success.profile?.userId)
        assertEquals("Yeojun", success.profile?.displayName)
        assertNull(success.profile?.pictureUrl)
        assertEquals("Hello", success.profile?.statusMessage)
        assertEquals("header.payload.signature", success.idToken?.rawValue)
        assertEquals("yjyoon@example.com", success.idToken?.email)
        assertEquals("https://example.com/p.jpg", success.idToken?.pictureUrl)
        assertEquals("nonce-value", success.idToken?.nonce)
        assertTrue(success.friendshipStatusChanged)
        assertEquals("nonce-value", success.nonce)
    }

    @Test
    fun successWithoutProfileOrIdTokenIsStillASuccess() {
        // What a login granted neither `profile` nor `openid` looks like.
        val result =
            SdkLoginResult
                .Builder()
                .responseCode(LineApiResponseCode.SUCCESS)
                .lineCredential(
                    LineCredential(
                        SdkAccessToken("access-token", EXPIRES_IN_MILLISECONDS, ISSUED_AT_MILLISECONDS),
                        emptyList(),
                    ),
                ).build()
                .toLineLoginResult()

        val success = assertIs<LineLoginResult.Success>(result)
        assertNull(success.profile)
        assertNull(success.idToken)
        assertEquals(false, success.friendshipStatusChanged)
    }

    @Test
    fun successWithoutCredentialIsAFailureRatherThanAnInventedToken() {
        val result =
            SdkLoginResult
                .Builder()
                .responseCode(LineApiResponseCode.SUCCESS)
                .build()
                .toLineLoginResult()

        assertEquals(LineLoginErrorCode.Internal, assertIs<LineLoginResult.Failure>(result).code)
    }

    @Test
    fun cancelIsItsOwnResultAndNotAnError() {
        assertEquals(LineLoginResult.Cancelled, SdkLoginResult.canceledError().toLineLoginResult())
    }

    @Test
    fun networkAndServerFailuresKeepTheSdkCodeAndMessage() {
        val result =
            SdkLoginResult
                .error(LineApiResponseCode.NETWORK_ERROR, LineApiError("timed out"))
                .toLineLoginResult()

        val failure = assertIs<LineLoginResult.Failure>(result)
        assertEquals(LineLoginErrorCode.Network, failure.code)
        assertEquals("timed out", failure.message)
        assertEquals("NETWORK_ERROR", failure.rawCode)
        assertEquals("timed out", failure.rawMessage)
    }

    @Test
    fun authenticationAgentErrorIsTheConsoleMismatchCase() {
        val result =
            SdkLoginResult
                .authenticationAgentError(LineApiError("signature mismatch"))
                .toLineLoginResult()

        assertEquals(LineLoginErrorCode.Authentication, assertIs<LineLoginResult.Failure>(result).code)
    }

    @Test
    fun loginActivityNotFoundBecomesLineAppUnavailable() {
        val result =
            SdkLoginResult
                .internalError(
                    LineApiError(
                        -1,
                        "Activity for LINE log-in is not found.",
                        LineApiError.ErrorCode.LOGIN_ACTIVITY_NOT_FOUND,
                    ),
                ).toLineLoginResult()

        assertEquals(
            LineLoginErrorCode.LineAppUnavailable,
            assertIs<LineLoginResult.Failure>(result).code,
        )
    }

    @Test
    fun aFailureWithNoMessageStillReadsLikeSomething() {
        val result = SdkLoginResult.internalError("").toLineLoginResult()

        val failure = assertIs<LineLoginResult.Failure>(result)
        assertEquals(defaultMessageFor(LineLoginErrorCode.Internal), failure.message)
    }

    @Test
    fun logoutMapsBothOutcomes() {
        assertEquals(
            LineLogoutResult.Success,
            LineApiResponse.createAsSuccess(Unit).toLineLogoutResult(),
        )

        val failure =
            LineApiResponse
                .createAsError<Unit>(
                    LineApiResponseCode.SERVER_ERROR,
                    LineApiError("revocation failed"),
                ).toLineLogoutResult()

        assertEquals(LineLoginErrorCode.Server, assertIs<LineLogoutResult.Failure>(failure).code)
    }

    @Test
    fun aCancelResponseCodeOnLogoutDegradesToInternalRatherThanVanishing() {
        // Not something LINE produces for a logout, but the mapper must not return "cancelled"
        // for an operation whose result type has no such case.
        val failure =
            LineApiResponse
                .createAsError<Unit>(LineApiResponseCode.CANCEL, LineApiError("odd"))
                .toLineLogoutResult()

        assertEquals(LineLoginErrorCode.Internal, assertIs<LineLogoutResult.Failure>(failure).code)
    }

    private companion object {
        const val EXPIRES_IN_MILLISECONDS = 3_600_000L
        const val ISSUED_AT_MILLISECONDS = 1_700_000_000_000L
    }
}
