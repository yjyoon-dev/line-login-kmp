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

import dev.yjyoon.lineloginkmp.LineAccessToken
import dev.yjyoon.lineloginkmp.LineLoginConfig
import dev.yjyoon.lineloginkmp.LineLoginRequest
import dev.yjyoon.lineloginkmp.LineLoginResult
import dev.yjyoon.lineloginkmp.LineLogoutResult

/**
 * The seam between the shared API and each platform's LINE SDK.
 *
 * Deliberately thin and stateless: policy — validation, the not-configured check, serialising
 * concurrent logins — lives in `LineLogin` so that it behaves identically everywhere, and only
 * the parts that genuinely differ are implemented twice. [config] is threaded through rather than
 * cached here, so there is exactly one copy of that state in the whole library.
 */
internal expect fun platformConfigure(config: LineLoginConfig)

internal expect suspend fun platformLogin(
    config: LineLoginConfig,
    request: LineLoginRequest,
): LineLoginResult

internal expect suspend fun platformLogout(config: LineLoginConfig): LineLogoutResult

internal expect suspend fun platformCurrentAccessToken(config: LineLoginConfig): LineAccessToken?

internal expect suspend fun platformIsLoggedIn(config: LineLoginConfig): Boolean

/**
 * Takes no [LineLoginConfig]: whether LINE is on the device has nothing to do with which channel
 * the app uses, so this answers before `configure` has ever run.
 */
internal expect suspend fun platformIsLineAppInstalled(): Boolean
