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

/**
 * Where [LineLoginButton]'s parts end up, in pixels. Everything is in the leading-edge-first
 * coordinate space; mirroring for right-to-left layouts is left to `placeRelative`.
 */
internal class LineLoginButtonGeometry(
    val width: Int,
    val height: Int,
    /** The icon occupies a square of this side — the invariant the whole button is built around. */
    val iconSide: Int,
    val iconX: Int,
    val iconY: Int,
    val dividerX: Int,
    val textX: Int,
    val textY: Int,
)

/**
 * Lays out the icon square, the divider and the caption inside whatever space the button was handed.
 *
 * Two behaviours have to hold at once, which is why this is arithmetic rather than a `Row`:
 *
 * - Given no width of its own, the button wraps its content, exactly as LINE's artwork is drawn.
 * - Given a wider width — `Modifier.fillMaxWidth()`, a fixed `width`, a stretching parent — the
 *   **icon square keeps its 1:1 ratio at the leading edge** and the caption area absorbs every extra
 *   pixel. The icon must not drift inward, and no background gap may open up beside it.
 *
 * A `Row` cannot do both: content-sized, it leaves the extra width outside itself; weighted, it
 * claims the parent's full width even when the caller never asked for it.
 *
 * @param iconSide the button height, which is also the side of the icon's square
 * @param dividerWidth 0 for an icon-only button, which has no divider
 * @param textWidth 0 for an icon-only button
 */
internal fun lineLoginButtonGeometry(
    minWidth: Int,
    maxWidth: Int,
    minHeight: Int,
    maxHeight: Int,
    iconSide: Int,
    dividerWidth: Int,
    textWidth: Int,
    textHeight: Int,
): LineLoginButtonGeometry {
    // Tall enough for the icon, and taller still if the reader's font size demands it — growing is
    // better than clipping LINE's wording.
    val height = maxOf(minHeight, iconSide, textHeight).coerceAtMost(maxHeight)

    // Only ever shrinks the icon, and only when the caller pinned the button shorter than the
    // height it was asked for. A square that overflows its button is worse than a smaller one.
    val side = iconSide.coerceAtMost(height)

    val contentWidth = side + dividerWidth + textWidth
    val width = contentWidth.coerceIn(minWidth, maxOf(minWidth, maxWidth))

    val hasText = dividerWidth > 0 || textWidth > 0
    val textArea = (width - side - dividerWidth).coerceAtLeast(0)

    return LineLoginButtonGeometry(
        width = width,
        height = height,
        iconSide = side,
        // An icon-only button has no caption to balance against, so the mark centres instead.
        iconX = if (hasText) 0 else (width - side) / 2,
        iconY = (height - side) / 2,
        dividerX = side,
        // Centred in the area left over, which is what LINE's own artwork shows: the caption is
        // centred in the region beside the icon, not butted up against the divider.
        textX = side + dividerWidth + ((textArea - textWidth) / 2).coerceAtLeast(0),
        textY = (height - textHeight) / 2,
    )
}
