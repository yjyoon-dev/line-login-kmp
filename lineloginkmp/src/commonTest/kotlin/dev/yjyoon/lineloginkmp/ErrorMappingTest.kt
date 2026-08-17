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

import dev.yjyoon.lineloginkmp.internal.mapAndroidError
import dev.yjyoon.lineloginkmp.internal.mapIosError
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * The whole error taxonomy of both SDKs, asserted as a table.
 *
 * This is the highest-risk logic in the library — it is what turns "something went wrong" into
 * something a consumer can act on, and it is the part least likely to be exercised by hand, since
 * most of these codes need a broken device or a misconfigured console to produce. Keeping the
 * mappers pure is what makes covering all of it possible without either.
 */
class ErrorMappingTest {
    @Test
    fun androidResponseCodesMapToTheSharedTaxonomy() {
        assertEquals(LineLoginErrorCode.Network, mapAndroidError("NETWORK_ERROR"))
        assertEquals(LineLoginErrorCode.Server, mapAndroidError("SERVER_ERROR"))
        assertEquals(LineLoginErrorCode.Authentication, mapAndroidError("AUTHENTICATION_AGENT_ERROR"))
        assertEquals(LineLoginErrorCode.Internal, mapAndroidError("INTERNAL_ERROR"))
    }

    @Test
    fun androidCancelIsNotAFailure() {
        assertNull(mapAndroidError("CANCEL"))
        assertNull(mapAndroidError("CANCEL", "NOT_DEFINED"))
    }

    @Test
    fun androidLoginActivityNotFoundOutranksItsResponseCode() {
        // The SDK reports "no LINE app and no browser could handle this" as an INTERNAL_ERROR
        // carrying a specific error code. The specific code is the one worth acting on.
        assertEquals(
            LineLoginErrorCode.LineAppUnavailable,
            mapAndroidError("INTERNAL_ERROR", "LOGIN_ACTIVITY_NOT_FOUND"),
        )
    }

    @Test
    fun androidOtherErrorCodesDoNotOverrideTheResponseCode() {
        assertEquals(LineLoginErrorCode.Network, mapAndroidError("NETWORK_ERROR", "NOT_DEFINED"))
        assertEquals(
            LineLoginErrorCode.Server,
            mapAndroidError("SERVER_ERROR", "HTTP_RESPONSE_PARSE_ERROR"),
        )
    }

    @Test
    fun androidUnknownResponseCodeFallsBackToInternal() {
        // A future SDK adding a response code must not be able to crash a login.
        assertEquals(LineLoginErrorCode.Internal, mapAndroidError("SOMETHING_NEW"))
        assertEquals(LineLoginErrorCode.Internal, mapAndroidError(""))
    }

    @Test
    fun iosCancellationCodesAreNotFailures() {
        assertNull(mapIosError(3003L), "userCancelled")
        assertNull(mapIosError(3004L), "forceStopped — this library's own cancel path")
        assertNull(mapIosError(4004L), "processDiscarded")
        assertNull(mapIosError(4005L), "loginManagerReset")
    }

    @Test
    fun iosNetworkAndServerCodes() {
        assertEquals(LineLoginErrorCode.Network, mapIosError(2001L), "URLSessionError")

        listOf(
            2002L to "nonHTTPURLResponse",
            2003L to "dataParsingFailed",
            2004L to "invalidHTTPStatusAPIError",
            3014L to "lackOfIDToken",
            3015L to "JWTPublicKeyNotFound",
        ).forEach { (code, name) ->
            assertEquals(LineLoginErrorCode.Server, mapIosError(code), name)
        }
    }

    @Test
    fun iosRegistrationMismatchesAreAuthenticationFailures() {
        listOf(
            3005L to "callbackURLSchemeNotMatching",
            3006L to "invalidSourceApplication",
            3007L to "malformedRedirectURL",
            3008L to "invalidLineURLResultCode",
            3009L to "lineClientError",
            3010L to "responseStateValueNotMatching",
            3011L to "webLoginError",
        ).forEach { (code, name) ->
            assertEquals(LineLoginErrorCode.Authentication, mapIosError(code), name)
        }
    }

    @Test
    fun iosExhaustedLoginFlowMeansNothingCouldPresentALogin() {
        assertEquals(LineLoginErrorCode.LineAppUnavailable, mapIosError(3001L))
    }

    @Test
    fun iosRemainingCodesFallBackToInternal() {
        listOf(
            1001L to "missingURL",
            1002L to "lackOfAccessToken",
            1003L to "jsonEncodingFailed",
            1004L to "invalidParameter",
            3002L to "malformedHierarchy",
            3012L to "keychainOperation",
            3013L to "invalidDataInKeychain",
            3016L to "cryptoError",
            4001L to "conversionError",
            4002L to "parameterError",
            4003L to "notOriginalTask",
            4006L to "deprecatedAPI",
        ).forEach { (code, name) ->
            assertEquals(LineLoginErrorCode.Internal, mapIosError(code), name)
        }
    }

    @Test
    fun iosUnknownCodeFallsBackToInternal() {
        // -1 is what this library passes for an error from another error domain.
        assertEquals(LineLoginErrorCode.Internal, mapIosError(-1L))
        assertEquals(LineLoginErrorCode.Internal, mapIosError(9999L))
    }
}
