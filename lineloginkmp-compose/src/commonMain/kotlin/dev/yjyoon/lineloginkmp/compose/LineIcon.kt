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
package dev.yjyoon.lineloginkmp.compose

import androidx.compose.ui.graphics.ImageBitmap
import org.jetbrains.compose.resources.decodeToImageBitmap
import kotlin.io.encoding.Base64

/**
 * The LINE icon from LINE's official
 * [button template](https://vos.line-scdn.net/line-developers/docs/media/line-login/login-button/LINE_Login_Button_Image.zip),
 * embedded byte-for-byte.
 *
 * It is the white 132x132 variant — `images/iOS/44dp/3x/line_132.png` in the template — which is
 * the highest resolution LINE ships. [LineLoginButton] draws it at the button height and tints it
 * per state, so this single source covers every size and every state, including the grey disabled
 * one.
 *
 * ### Why it is Base64 in a Kotlin file
 *
 * Not for fun. Compose Multiplatform's resource packaging does not reach the AAR produced by the
 * `com.android.kotlin.multiplatform.library` plugin: the resource is generated for the iOS targets
 * and silently missing on Android, so a consumer app crashes at first composition with
 * `MissingResourceException`. Bytes in the binary cannot go missing, work identically on both
 * platforms, and keep LINE's artwork unmodified — which the guidelines require.
 *
 * The image is decoded once, lazily, the first time a button is composed.
 */
@OptIn(kotlin.io.encoding.ExperimentalEncodingApi::class)
internal object LineIcon {
    val bitmap: ImageBitmap by lazy {
        Base64.decode(PNG_BASE64.filterNot { it.isWhitespace() }).decodeToImageBitmap()
    }

    /** Wrapped for legibility; the whitespace is stripped before decoding. */
    private val PNG_BASE64 =
        """
        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
        +m2hnikAAAAASUVORK5CYII=
        """
}
