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

package dev.yjyoon.lineloginkmp

import dev.yjyoon.lineloginkmp.internal.linesdk.LineSDKErrorConstant
import dev.yjyoon.lineloginkmp.internal.linesdk.LineSDKLoginManager
import dev.yjyoon.lineloginkmp.internal.linesdk.LineSDKLoginPermission
import kotlinx.cinterop.ExperimentalForeignApi
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull

/**
 * Proves the cinterop binding still resolves against a real LineSDKObjC.
 *
 * This is a link test as much as a unit test: a test binary is a genuine executable, so it has to
 * find every LineSDKObjC symbol this library references. If LINE ever renames a class, changes a
 * selector, or stops shipping the Objective-C layer, this fails at build time here rather than at
 * runtime in somebody's app.
 *
 * It deliberately never calls `setup`, so nothing touches the keychain or the network.
 */
class LineSdkInteropTest {
    @Test
    fun theSharedLoginManagerResolvesAndReportsNoSetup() {
        val manager = LineSDKLoginManager.sharedManager()

        assertNotNull(manager)
        assertFalse(manager.isSetupFinished(), "No test may configure the process-wide SDK.")

        // `isAuthorized` is deliberately NOT called here. Before setup it does not return false —
        // it kills the process:
        //
        //   LineSDKObjC/Helpers.swift:149: Fatal error: [LineSDK] Use AccessTokenStore before
        //   setup. Please call `LoginManager.setup` before you do any other things in LineSDK.
        //
        // Which is why every accessor in LineLoginPlatform.ios.kt is guarded by isSetupFinished()
        // instead of relying on the SDK to answer sensibly. This comment is the regression test:
        // if the guards are ever dropped, `LineLogin.isLoggedIn()` becomes a crash.
    }

    @Test
    fun permissionsCanBeBuiltFromArbitraryScopeCodes() {
        // How LineScope reaches the SDK: by raw value, so scopes LINE adds later need no release.
        assertNotNull(LineSDKLoginPermission("profile"))
        assertNotNull(LineSDKLoginPermission("openid"))
        assertNotNull(LineSDKLoginPermission("something.line.invents.later"))
    }

    @Test
    fun theErrorDomainIsTheOneErrorMappingFiltersOn() {
        // IosResultMapping ignores codes from any other domain, so this string is load-bearing.
        assertEquals("LineSDKError", LineSDKErrorConstant.errorDomain)
    }
}
