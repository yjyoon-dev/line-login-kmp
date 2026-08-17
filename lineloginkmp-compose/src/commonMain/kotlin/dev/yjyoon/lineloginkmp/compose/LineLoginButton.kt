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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
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
 * Given no width, the button wraps its content. Given a wider one — `Modifier.fillMaxWidth()`, a
 * fixed `width`, a stretching parent — the icon square keeps its 1:1 ratio at the leading edge and
 * the caption area takes the extra width.
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
 *   `null` for an icon-only button, which the guidelines also permit — the icon then carries LINE's
 *   wording as its accessibility label, so a screen reader still announces what the button does.
 *   Anything else must be a single line that makes clear the button logs in with LINE.
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

    // A caption describes the button to a screen reader by itself. Without one there is nothing to
    // announce but "button", so the icon carries LINE's own wording for the reader's language.
    val iconDescription = if (text == null) LineLoginButtonText.current().long else null

    val captionStyle =
        remember(textStyle, contentColor) {
            textStyle.copy(color = contentColor, textAlign = TextAlign.Center)
        }

    // Layer order matters, and the guidelines spell it out: the state overlay sits on the base
    // colour, and the divider and content sit above the overlay — never tinted by it. Both colours
    // are background layers of the button itself, so every child draws over them untouched.
    val overlay =
        when {
            !enabled -> Color.Transparent
            pressed -> LineLoginButtonColors.PressOverlay
            hovered -> LineLoginButtonColors.HoverOverlay
            else -> Color.Transparent
        }

    // A Layout rather than a Row: see lineLoginButtonGeometry for why a Row cannot both wrap its
    // content and pin the icon square to the leading edge when the button is stretched.
    Layout(
        modifier =
            modifier
                .clip(shape)
                .background(
                    if (enabled) LineLoginButtonColors.Base else LineLoginButtonColors.DisabledBackground,
                ).background(overlay)
                // A border belongs to the disabled state only — it is what keeps a white button
                // visible on a white surface.
                .then(
                    if (enabled) {
                        Modifier
                    } else {
                        Modifier.border(1.dp, LineLoginButtonColors.DisabledBorder, shape)
                    },
                )
                // indication = null: the state overlay above is LINE's, and Material's ripple would
                // paint a second, non-designated colour over it.
                .clickable(
                    enabled = enabled,
                    interactionSource = interactionSource,
                    indication = null,
                    role = Role.Button,
                    onClick = onClick,
                ),
        content = {
            // The inset around the speech bubble is part of the icon itself, so there is no padding
            // to add here — the square is measured to the button's height and the artwork fills it.
            Image(
                painter = lineIcon,
                contentDescription = iconDescription,
                colorFilter = ColorFilter.tint(contentColor),
            )

            if (text != null) {
                Box(Modifier.background(dividerColor))

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
                    style = captionStyle,
                    // The guidelines forbid line breaks in the caption.
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        },
    ) { measurables, constraints ->
        val requestedIconSide = height.roundToPx()
        val dividerWidth = if (measurables.size > 1) DividerThickness.roundToPx() else 0

        // Measured against the room actually left beside the icon, so an over-long caption
        // ellipsises instead of pushing the icon out of its square.
        val textPlaceable =
            measurables.getOrNull(2)?.measure(
                Constraints(
                    maxWidth =
                        if (constraints.maxWidth == Constraints.Infinity) {
                            Constraints.Infinity
                        } else {
                            (constraints.maxWidth - requestedIconSide - dividerWidth).coerceAtLeast(0)
                        },
                ),
            )

        val geometry =
            lineLoginButtonGeometry(
                minWidth = constraints.minWidth,
                maxWidth = constraints.maxWidth,
                minHeight = constraints.minHeight,
                maxHeight = constraints.maxHeight,
                iconSide = requestedIconSide,
                dividerWidth = dividerWidth,
                textWidth = textPlaceable?.width ?: 0,
                textHeight = textPlaceable?.height ?: 0,
            )

        val iconPlaceable =
            measurables[0].measure(Constraints.fixed(geometry.iconSide, geometry.iconSide))
        val dividerPlaceable =
            measurables.getOrNull(1)?.measure(Constraints.fixed(dividerWidth, geometry.height))

        layout(geometry.width, geometry.height) {
            // placeRelative, so a right-to-left layout mirrors the whole button and the icon stays
            // on the leading edge.
            iconPlaceable.placeRelative(geometry.iconX, geometry.iconY)
            dividerPlaceable?.placeRelative(geometry.dividerX, 0)
            textPlaceable?.placeRelative(geometry.textX, geometry.textY)
        }
    }
}

/** The hairline between the icon square and the caption, as drawn in LINE's artwork. */
private val DividerThickness = 1.dp
