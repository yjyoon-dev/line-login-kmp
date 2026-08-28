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
@file:OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)

package dev.yjyoon.lineloginkmp.sample

import androidx.compose.ui.window.ComposeViewport
import dev.yjyoon.lineloginkmp.LineLogin
import dev.yjyoon.lineloginkmp.LineLoginConfig

fun main() {
    // Before the first frame, as on every platform — and on web this is also what completes a
    // login: coming back from LINE's redirect, configure() consumes the code in the URL.
    if (SAMPLE_LINE_CHANNEL_ID.isNotBlank() && SAMPLE_LIFF_ID.isNotBlank()) {
        LineLogin.configure(
            LineLoginConfig(
                channelId = SAMPLE_LINE_CHANNEL_ID,
                liffId = SAMPLE_LIFF_ID,
            ),
        )
    }

    ComposeViewport(viewportContainerId = "sample") {
        App()
    }
}
