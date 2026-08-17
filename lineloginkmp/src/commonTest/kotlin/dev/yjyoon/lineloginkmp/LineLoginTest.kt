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

import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull

/**
 * What the entry point does before anyone has configured it.
 *
 * Everything here stops short of the platform layer on purpose — these are the paths a consumer
 * hits when they wire the library up wrong, which is exactly when a clear answer matters most.
 */
class LineLoginTest {
    @BeforeTest
    @AfterTest
    fun reset() {
        LineLogin.resetForTest()
    }

    @Test
    fun isNotConfiguredBeforeConfigure() {
        assertFalse(LineLogin.isConfigured)
    }

    @Test
    fun loginWithoutConfiguringFailsInsteadOfThrowing() =
        runTest {
            val result = LineLogin.login()

            // Never an exception: a caller doing `when (login())` must not have to also try/catch.
            val failure = assertIs<LineLoginResult.Failure>(result)
            assertEquals(LineLoginErrorCode.NotConfigured, failure.code)
        }

    @Test
    fun logoutWithoutConfiguringFailsInsteadOfThrowing() =
        runTest {
            val failure = assertIs<LineLogoutResult.Failure>(LineLogin.logout())
            assertEquals(LineLoginErrorCode.NotConfigured, failure.code)
        }

    @Test
    fun tokenQueriesAreEmptyRatherThanFatalBeforeConfigure() =
        runTest {
            assertNull(LineLogin.currentAccessToken())
            assertFalse(LineLogin.isLoggedIn())
        }
}
