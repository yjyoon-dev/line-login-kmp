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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
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
 * The three overlay colours are meant to be composited *over* [Base], not to replace it.
 */
public object LineLoginButtonColors {
    /** `#06C755`. The button's own green. */
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
 * Geometry and typography defaults for [LineLoginButton].
 *
 * Every measurement is derived from one number — the width of the LINE icon's speech bubble, which
 * the guidelines call **X**. That is what makes the button scalable without breaking the rules:
 * change [IconSize] and the padding, the isolation zone and the overall height all follow.
 *
 *  - Text padding, left and right: **X** (the guidelines require *at least* X).
 *  - Text padding, top and bottom: **X / 2** (the recommended value).
 *  - Padding around the icon: **X / 2**.
 *  - Isolation zone — the empty space that must surround the button: **X / 2**.
 */
public object LineLoginButtonDefaults {
    /** **X**: the width of the LINE icon. Scale the whole button by changing this. */
    public val IconSize: Dp = 24.dp

    /** Space around the icon inside the button. */
    public fun iconPadding(iconSize: Dp = IconSize): Dp = iconSize / 2

    /** Left and right padding around the caption. */
    public fun textHorizontalPadding(iconSize: Dp = IconSize): Dp = iconSize

    /** Top and bottom padding around the caption. */
    public fun textVerticalPadding(iconSize: Dp = IconSize): Dp = iconSize / 2

    /**
     * The empty space that must surround the button. Apply it yourself — as padding on the button,
     * or as spacing in the layout around it — and keep other text and graphics out of it.
     */
    public fun isolationZone(iconSize: Dp = IconSize): Dp = iconSize / 2

    /**
     * The guidelines do not fix a corner radius, so this is a neutral default rather than a rule.
     * Match whichever image set you took from LINE's template.
     */
    public val Shape: Shape = RoundedCornerShape(6.dp)

    /**
     * The guidelines ask only that the caption be readable and fit the padding on one line.
     * Colour is applied by the button, per state.
     */
    public val TextStyle: TextStyle =
        TextStyle(
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
        )
}
