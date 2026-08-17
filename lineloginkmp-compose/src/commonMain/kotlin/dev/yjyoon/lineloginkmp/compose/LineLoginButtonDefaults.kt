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

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * The exact palette LINE's
 * [button design guidelines](https://developers.line.biz/en/docs/line-login/login-button/) allow.
 *
 * Read-only by design: "using a non-designated color" is one of the mistakes the guidelines call
 * out, so [LineLoginButton] does not let you override these. They are public so that an app
 * building its own button — a UIKit or Android View one, say — can still get them right.
 *
 * The overlay colours are meant to be composited *over* [Base], not to replace it.
 */
public object LineLoginButtonColors {
    /** `#06C755`. The button's own green, sampled from LINE's own template to confirm it. */
    public val Base: Color = Color(0xFF06C755)

    /** `#000000` at 10%, over [Base]. */
    public val HoverOverlay: Color = Color(0x1A000000)

    /** `#000000` at 30%, over [Base]. */
    public val PressOverlay: Color = Color(0x4D000000)

    /** `#FFFFFF`. A disabled button is white, not dimmed green. */
    public val DisabledBackground: Color = Color(0xFFFFFFFF)

    /** `#FFFFFF`. Text and logo, in every state except disabled. */
    public val Content: Color = Color(0xFFFFFFFF)

    /** `#1E1E1E` at 20%. Text and logo when disabled. */
    public val DisabledContent: Color = Color(0x331E1E1E)

    /** `#000000` at 8%. The rule between the logo and the text. */
    public val Divider: Color = Color(0x14000000)

    /** `#E5E5E5` at 60%. The rule when disabled. */
    public val DisabledDivider: Color = Color(0x99E5E5E5)

    /** `#E5E5E5` at 60%. A border exists only on the disabled button. */
    public val DisabledBorder: Color = Color(0x99E5E5E5)
}

/**
 * Geometry and typography for [LineLoginButton], derived from LINE's own button artwork.
 *
 * Everything here follows from **one** number, the button's height, because that is how LINE's
 * template is built: the icon sits in a square the full height of the button, and the guidelines
 * then define the text padding in terms of the speech bubble inside that square (which they call
 * **X**). The ratios below were measured from the 20dp, 32dp and 44dp buttons in LINE's template
 * rather than guessed:
 *
 * | Button height | Corner radius | X (bubble width) | Text padding |
 * |---|---|---|---|
 * | 44 dp | 6.0 dp | 32.0 dp | 32.5 dp |
 * | 32 dp | 4.0 dp | 23.0 dp | 24.5 dp |
 * | 20 dp | 2.5 dp | 15.0 dp | 16.0 dp |
 *
 * So `X ≈ height × 0.727`, the corner radius is `height / 8`, the horizontal text padding is `X`
 * and the vertical padding is `X / 2` — which is exactly what the guidelines ask for.
 */
public object LineLoginButtonDefaults {
    /** LINE's largest reference button, and the size its template is centred on. */
    public val Height: Dp = 44.dp

    /**
     * **X**: the width of the LINE icon's speech bubble at this button height — the measurement the
     * guidelines define the padding and the isolation zone from.
     *
     * Smaller than the height because the icon artwork carries its own inset, exactly as LINE's
     * template does.
     */
    public fun bubbleWidth(height: Dp = Height): Dp = height * BUBBLE_WIDTH_RATIO

    /** Left and right padding around the caption: **X**. */
    public fun textHorizontalPadding(height: Dp = Height): Dp = bubbleWidth(height)

    /**
     * Top and bottom padding around the caption: **X / 2**.
     *
     * [LineLoginButton] does not apply this as padding — at a correctly sized caption, centring in a
     * button [height] tall produces it, and adding it on top would inflate a 44 dp button to 52 dp.
     * It is here for anyone laying out their own button.
     */
    public fun textVerticalPadding(height: Dp = Height): Dp = bubbleWidth(height) / 2

    /**
     * The empty space that must surround the button. Apply it yourself — as padding on the button,
     * or as spacing in the layout around it — and keep other text and graphics out of it.
     */
    public fun isolationZone(height: Dp = Height): Dp = bubbleWidth(height) / 2

    /** `height / 8`, matching the radius of LINE's own button images. */
    public fun shape(height: Dp = Height): Shape = RoundedCornerShape(height / 8)

    /**
     * Scaled with the button, so a caption still fits the padding the guidelines require. In `sp`,
     * so it respects the reader's font-size setting; a very large setting grows the button rather
     * than clipping the text.
     */
    public fun textStyle(height: Dp = Height): TextStyle =
        TextStyle(
            fontSize = (height.value / FONT_SIZE_DIVISOR).sp,
            fontWeight = FontWeight.Medium,
        )

    /**
     * The LINE icon, from LINE's official button template, bundled with this library.
     *
     * The white variant: [LineLoginButton] tints it per state, so this one source is correct
     * everywhere, including the grey disabled state. See [LineIcon] for why it is embedded rather
     * than shipped as a Compose resource.
     */
    @Composable
    public fun lineIcon(): Painter = remember { BitmapPainter(LineIcon.bitmap) }

    /** 32 dp of bubble in a 44 dp button — measured, not estimated. */
    private const val BUBBLE_WIDTH_RATIO = 32f / 44f

    /** Puts a 44 dp button's caption at ~15 sp, matching LINE's artwork. */
    private const val FONT_SIZE_DIVISOR = 3f
}
