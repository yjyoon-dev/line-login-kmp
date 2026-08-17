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

import dev.yjyoon.lineloginkmp.internal.toLoginFailure
import dev.yjyoon.lineloginkmp.internal.toLogoutFailure
import platform.Foundation.NSError
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

/**
 * The iOS half of error handling, against real `NSError` values.
 *
 * The Android mapping is covered against real SDK objects; this is its counterpart. It matters
 * most for the domain filter: LINE's codes are small integers that other Apple frameworks also
 * use, so reading a code without checking where it came from would silently turn an unrelated
 * failure into "the user cancelled" — a login that reports nothing and shows nothing.
 */
class IosErrorMappingTest {
    @Test
    fun lineCancellationCodesBecomeCancelled() {
        assertEquals(LineLoginResult.Cancelled, lineError(USER_CANCELLED).toLoginFailure())
        assertEquals(LineLoginResult.Cancelled, lineError(FORCE_STOPPED).toLoginFailure())
    }

    @Test
    fun lineFailureCodesKeepTheirCategoryAndRawValues() {
        val failure = assertIs<LineLoginResult.Failure>(lineError(URL_SESSION_ERROR).toLoginFailure())

        assertEquals(LineLoginErrorCode.Network, failure.code)
        assertEquals(URL_SESSION_ERROR.toString(), failure.rawCode)
    }

    @Test
    fun anErrorFromAnotherDomainIsNeverReadAsACancel() {
        // 3003 is `userCancelled` to LINE and something else entirely to everyone else. Without
        // the domain check this would resolve to Cancelled and the login would fail in silence.
        val failure = assertIs<LineLoginResult.Failure>(foreignError(USER_CANCELLED).toLoginFailure())

        assertEquals(LineLoginErrorCode.Internal, failure.code)
    }

    @Test
    fun logoutNeverReportsACancellation() {
        // LineLogoutResult has no Cancelled case, so a cancellation code must degrade rather than
        // vanish.
        val failure = assertIs<LineLogoutResult.Failure>(lineError(USER_CANCELLED).toLogoutFailure())

        assertEquals(LineLoginErrorCode.Internal, failure.code)
    }

    @Test
    fun aMessagelessErrorStillReadsLikeSomething() {
        val failure = assertIs<LineLoginResult.Failure>(lineError(URL_SESSION_ERROR).toLoginFailure())

        assertEquals(true, failure.message.isNotBlank())
    }

    private fun lineError(code: Long): NSError = NSError.errorWithDomain(domain = LINE_SDK_ERROR_DOMAIN, code = code, userInfo = null)

    private fun foreignError(code: Long): NSError = NSError.errorWithDomain(domain = "NSURLErrorDomain", code = code, userInfo = null)

    private companion object {
        /** `LineSDKErrorConstant.errorDomain`, asserted in [LineSdkInteropTest]. */
        const val LINE_SDK_ERROR_DOMAIN = "LineSDKError"

        const val USER_CANCELLED = 3003L
        const val FORCE_STOPPED = 3004L
        const val URL_SESSION_ERROR = 2001L
    }
}
