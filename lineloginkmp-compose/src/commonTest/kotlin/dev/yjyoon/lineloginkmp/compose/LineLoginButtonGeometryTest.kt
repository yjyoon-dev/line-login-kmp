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

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The button's geometry, pinned. A `Row` used to lay this out, which looked right at the content
 * width and drifted the icon inward the moment the button was stretched — so the invariants that
 * broke are the ones asserted here.
 */
class LineLoginButtonGeometryTest {
    /** 44 dp at 3x, LINE's own artwork size. */
    private val iconSide = 132
    private val dividerWidth = 3
    private val textWidth = 300
    private val textHeight = 44

    private fun geometry(
        minWidth: Int = 0,
        maxWidth: Int = 10_000,
        minHeight: Int = 0,
        maxHeight: Int = 10_000,
        textWidth: Int = this.textWidth,
        textHeight: Int = this.textHeight,
        dividerWidth: Int = this.dividerWidth,
    ) = lineLoginButtonGeometry(
        minWidth = minWidth,
        maxWidth = maxWidth,
        minHeight = minHeight,
        maxHeight = maxHeight,
        iconSide = iconSide,
        dividerWidth = dividerWidth,
        textWidth = textWidth,
        textHeight = textHeight,
    )

    @Test
    fun wrapsItsContentWhenGivenNoWidth() {
        val g = geometry()

        assertEquals(iconSide + dividerWidth + textWidth, g.width)
        assertEquals(iconSide, g.height)
        // Flush against the divider, which at the content width is where the caption belongs.
        assertEquals(iconSide + dividerWidth, g.textX)
    }

    @Test
    fun iconStaysASquareOnTheLeadingEdgeWhenStretched() {
        val stretched = 1_000
        val g = geometry(minWidth = stretched, maxWidth = stretched)

        assertEquals(stretched, g.width)
        // The regression: the icon must not drift inward, and it must stay 1:1.
        assertEquals(0, g.iconX)
        assertEquals(iconSide, g.iconSide)
        assertEquals(iconSide, g.height)
        assertEquals(iconSide, g.dividerX)
    }

    @Test
    fun theCaptionAreaAbsorbsTheExtraWidth() {
        val stretched = 1_000
        val g = geometry(minWidth = stretched, maxWidth = stretched)

        val textArea = stretched - iconSide - dividerWidth
        assertEquals(iconSide + dividerWidth + (textArea - textWidth) / 2, g.textX)
        // Centred in the area beside the icon: equal gaps on both sides of the caption.
        val leadingGap = g.textX - (iconSide + dividerWidth)
        val trailingGap = stretched - (g.textX + textWidth)
        assertTrue(
            (leadingGap - trailingGap) <= 1,
            "caption off-centre: $leadingGap vs $trailingGap",
        )
    }

    @Test
    fun anIconOnlyButtonCentresTheMark() {
        val stretched = 600
        val g =
            geometry(
                minWidth = stretched,
                maxWidth = stretched,
                textWidth = 0,
                textHeight = 0,
                dividerWidth = 0,
            )

        assertEquals((stretched - iconSide) / 2, g.iconX)
        assertEquals(stretched, g.width)
    }

    @Test
    fun growsTallerThanTheIconForAnOversizedCaption() {
        val g = geometry(textHeight = 200)

        assertEquals(200, g.height)
        // The square keeps its size and centres vertically rather than stretching with the button.
        assertEquals(iconSide, g.iconSide)
        assertEquals((200 - iconSide) / 2, g.iconY)
    }

    @Test
    fun neverOverflowsAButtonPinnedShorterThanItsIcon() {
        val g = geometry(minHeight = 90, maxHeight = 90)

        assertEquals(90, g.height)
        assertEquals(90, g.iconSide)
        assertEquals(0, g.iconY)
    }

    @Test
    fun neverExceedsTheWidthItWasOffered() {
        val g = geometry(maxWidth = 200)

        assertEquals(200, g.width)
        assertTrue(g.textX >= iconSide + dividerWidth, "caption overlaps the divider")
    }

    @Test
    fun theCaptionIsVerticallyCentred() {
        val g = geometry()

        assertEquals((iconSide - textHeight) / 2, g.textY)
    }
}
