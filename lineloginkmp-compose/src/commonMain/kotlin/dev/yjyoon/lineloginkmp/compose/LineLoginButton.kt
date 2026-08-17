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
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * A "Log in with LINE" button that follows LINE's
 * [button design guidelines](https://developers.line.biz/en/docs/line-login/login-button/).
 *
 * ```
 * LineLoginButton(onClick = { scope.launch { handle(LineLogin.login()) } })
 * ```
 *
 * That is the whole thing: LINE's own icon, LINE's own caption for the reader's language, and the
 * colours, divider, corner radius and padding taken from LINE's button artwork.
 *
 * The guidelines are not advice — LINE requires them, and a non-designated colour or a modified
 * icon is a compliance problem rather than a cosmetic one. So this button does not let you change
 * the palette, and the state treatments are the ones LINE specifies: a 10% black overlay on hover,
 * 30% on press, and a *white* button with a border and grey content when disabled.
 *
 * ### Sizing
 *
 * Scale with [height]. Everything else — the icon, the divider, the padding, the corner radius, the
 * caption size — is derived from it, so the icon's aspect ratio and the required padding hold at any
 * size. See [LineLoginButtonDefaults] for the ratios and where they were measured from.
 *
 * The button grows past [height] if the reader's font-size setting demands it, rather than clipping
 * the caption.
 *
 * ### Isolation zone
 *
 * The guidelines also require empty space around the button, which a composable cannot enforce from
 * the inside. Keep other content [LineLoginButtonDefaults.isolationZone] away from it.
 *
 * @param onClick Called when the user taps the button. Start the login here.
 * @param enabled When false the button turns white with a border and grey content, per the
 *   guidelines — not a dimmed green.
 * @param text The caption. Defaults to LINE's recommended phrase for the current locale. Pass
 *   `null` for an icon-only button, which the guidelines also permit; anything else must be a
 *   single line that makes clear the button logs in with LINE.
 * @param height The button height, and the side of the square the icon sits in.
 * @param lineIcon Defaults to the icon from LINE's official template, bundled with this library.
 *   Replace it only with another asset from that template — the guidelines forbid a modified or
 *   outdated mark — and use a white one, since it is tinted per state.
 */
@Composable
public fun LineLoginButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    text: String? = LineLoginButtonText.current().long,
    height: Dp = LineLoginButtonDefaults.Height,
    shape: Shape = LineLoginButtonDefaults.shape(height),
    textStyle: TextStyle = LineLoginButtonDefaults.textStyle(height),
    lineIcon: Painter = LineLoginButtonDefaults.lineIcon(),
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
                // A minimum rather than a fixed height: at a large system font size the caption
                // needs the room, and growing is better than clipping LINE's wording.
                .heightIn(min = height)
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
                // paint a second, non-designated colour over it.
                .clickable(
                    enabled = enabled,
                    interactionSource = interactionSource,
                    indication = null,
                    role = Role.Button,
                    onClick = onClick,
                ),
        contentAlignment = Alignment.Center,
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
            // Intrinsic height, so the divider spans the whole button while the button is still
            // sized by its content.
            modifier = Modifier.height(IntrinsicSize.Min),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // A square the full height of the button, matching LINE's artwork. The inset around the
            // speech bubble is part of the icon itself, so there is no padding to add here.
            Image(
                painter = lineIcon,
                contentDescription = null,
                modifier = Modifier.size(height),
                colorFilter = ColorFilter.tint(contentColor),
            )

            if (text != null) {
                Box(
                    Modifier
                        .width(1.dp)
                        .fillMaxHeight()
                        .background(dividerColor),
                )

                // Only horizontal padding is applied. The vertical padding the guidelines recommend
                // — X/2 — is what centring a correctly sized caption in a button this tall already
                // produces; adding it on top would make a 44 dp button 52 dp and break the very
                // proportion it is meant to preserve. LINE's own 44 dp artwork has 13.5 dp of ink
                // with 17/13.5 dp above and below, which is what this renders.
                BasicText(
                    text = text,
                    modifier =
                        Modifier.padding(
                            horizontal = LineLoginButtonDefaults.textHorizontalPadding(height),
                        ),
                    style = textStyle.copy(color = contentColor, textAlign = TextAlign.Center),
                    // The guidelines forbid line breaks in the caption.
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
