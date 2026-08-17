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
package dev.yjyoon.lineloginkmp.sample

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * A generic speech bubble standing in for the real LINE icon.
 *
 * **This is not the LINE icon, and a shipping app must not use it.** LINE's mark comes from their
 * [button template](https://vos.line-scdn.net/line-developers/docs/media/line-login/login-button/LINE_Login_Button_Image.zip);
 * downloading it is how you accept the usage guidelines attached to it, which is why neither this
 * library nor this sample bundles it. Drop the **white** icon from that template into your
 * resources and pass it as `LineLoginButton(lineIcon = …)`.
 *
 * The placeholder exists so the sample builds and runs out of the box, and so the geometry the
 * button derives from the icon is visible.
 */
internal val PlaceholderSpeechBubble: ImageVector =
    ImageVector
        .Builder(
            name = "PlaceholderSpeechBubble",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(fill = SolidColor(Color.White)) {
                moveTo(6f, 3f)
                lineTo(18f, 3f)
                quadToRelative(4f, 0f, 4f, 4f)
                lineTo(22f, 13f)
                quadToRelative(0f, 4f, -4f, 4f)
                lineTo(13f, 17f)
                lineTo(9f, 21f)
                lineTo(9f, 17f)
                lineTo(6f, 17f)
                quadToRelative(-4f, 0f, -4f, -4f)
                lineTo(2f, 7f)
                quadToRelative(0f, -4f, 4f, -4f)
                close()
            }
        }.build()
