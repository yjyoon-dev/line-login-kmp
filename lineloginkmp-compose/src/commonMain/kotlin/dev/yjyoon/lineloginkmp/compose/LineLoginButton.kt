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

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * A "Log in with LINE" button that follows LINE's
 * [button design guidelines](https://developers.line.biz/en/docs/line-login/login-button/).
 *
 * ```
 * LineLoginButton(
 *     lineIcon = painterResource(Res.drawable.line_icon),
 *     onClick = { scope.launch { handle(LineLogin.login()) } },
 * )
 * ```
 *
 * The guidelines are not advice — LINE requires them, and getting the colours, the divider, the
 * padding or the caption wrong is a compliance problem, not a cosmetic one. This composable owns
 * all of that: the exact palette including the hover and press overlays and the white disabled
 * state, the divider between logo and caption, padding derived from the icon width, and a caption
 * that defaults to LINE's own wording for the user's language.
 *
 * ### You supply the icon
 *
 * [lineIcon] has no default, and this library ships no LINE artwork. The mark is LINE's, and
 * downloading their
 * [button template](https://vos.line-scdn.net/line-developers/docs/media/line-login/login-button/LINE_Login_Button_Image.zip)
 * is how you accept the
 * [usage guidelines](https://terms2.line.me/LINE_Developers_Guidelines_for_Login_Button) that come
 * with it — a step nobody can take on your behalf. Use the **white** icon from that template: it is
 * tinted per state, so a white source is correct in every state, including the grey disabled one.
 *
 * ### Sizing
 *
 * Scale the button with [iconSize], not with a fixed height. Everything else is derived from it, so
 * the icon's aspect ratio and the required padding survive any size — which is exactly what the
 * guidelines ask.
 *
 * ### Isolation zone
 *
 * The guidelines also require empty space around the button. This composable cannot enforce that
 * from the inside; keep other content [LineLoginButtonDefaults.isolationZone] away from it.
 *
 * @param lineIcon The LINE icon from LINE's official template. See above.
 * @param onClick Called when the user taps the button. Start the login here.
 * @param enabled When false the button turns white with a border and grey content, per the
 *   guidelines — not a dimmed green.
 * @param text The caption. Defaults to LINE's recommended phrase for the current locale. Pass
 *   `null` for an icon-only button, which the guidelines also permit; anything else must be a
 *   single line that makes clear the button logs in with LINE.
 * @param iconSize The width of the LINE icon — the **X** the guidelines derive every other
 *   measurement from.
 */
@Composable
public fun LineLoginButton(
    lineIcon: Painter,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    text: String? = LineLoginButtonText.current().long,
    iconSize: Dp = LineLoginButtonDefaults.IconSize,
    shape: Shape = LineLoginButtonDefaults.Shape,
    textStyle: TextStyle = LineLoginButtonDefaults.TextStyle,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) {
    val pressed by interactionSource.collectIsPressedAsState()
    val hovered by interactionSource.collectIsHoveredAsState()

    val contentColor =
        if (enabled) LineLoginButtonColors.Content else LineLoginButtonColors.DisabledContent
    val dividerColor =
        if (enabled) LineLoginButtonColors.Divider else LineLoginButtonColors.DisabledDivider

    Box(
        modifier =
            modifier
                .clip(shape)
                .background(
                    if (enabled) LineLoginButtonColors.Base else LineLoginButtonColors.DisabledBackground,
                )
                // A border belongs to the disabled state only — it is what keeps a white button
                // visible on a white surface.
                .then(
                    if (enabled) {
                        Modifier
                    } else {
                        Modifier.border(1.dp, LineLoginButtonColors.DisabledBorder, shape)
                    },
                )
                // indication = null: the state overlay below is LINE's, and Material's ripple would
                // paint a second, non-designated colour on top of it.
                .clickable(
                    enabled = enabled,
                    interactionSource = interactionSource,
                    indication = null,
                    role = Role.Button,
                    onClick = onClick,
                ),
    ) {
        // Layer order matters, and the guidelines spell it out: the state overlay sits on the base
        // colour, and the divider and content sit above the overlay — never tinted by it.
        val overlay =
            when {
                !enabled -> Color.Transparent
                pressed -> LineLoginButtonColors.PressOverlay
                hovered -> LineLoginButtonColors.HoverOverlay
                else -> Color.Transparent
            }
        if (overlay != Color.Transparent) {
            Box(Modifier.matchParentSize().background(overlay))
        }

        Row(
            // Intrinsic height, so the divider can span the full button while the button still
            // sizes itself from its content.
            modifier = Modifier.height(IntrinsicSize.Min),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                painter = lineIcon,
                contentDescription = null,
                modifier =
                    Modifier
                        .padding(LineLoginButtonDefaults.iconPadding(iconSize))
                        .size(iconSize),
                colorFilter = ColorFilter.tint(contentColor),
            )

            if (text != null) {
                Box(
                    Modifier
                        .width(1.dp)
                        .fillMaxHeight()
                        .background(dividerColor),
                )

                BasicText(
                    text = text,
                    modifier =
                        Modifier.padding(
                            horizontal = LineLoginButtonDefaults.textHorizontalPadding(iconSize),
                            vertical = LineLoginButtonDefaults.textVerticalPadding(iconSize),
                        ),
                    style = textStyle.copy(color = contentColor),
                    // The guidelines forbid line breaks in the caption.
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
