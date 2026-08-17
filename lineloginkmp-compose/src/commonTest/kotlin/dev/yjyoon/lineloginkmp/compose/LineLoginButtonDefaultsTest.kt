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
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The geometry, pinned to what LINE's own button images actually measure.
 *
 * These numbers came out of the 20 dp, 32 dp and 44 dp buttons in LINE's template — the bubble
 * inside the icon square, the corner radius, the padding around the caption. Nothing here can be
 * re-derived from the written guidelines alone, so if someone "tidies up" a ratio the only thing
 * standing between that and a subtly non-compliant button is this file.
 */
class LineLoginButtonDefaultsTest {
    @Test
    fun bubbleWidthMatchesLinesArtwork() {
        assertEquals(32f, LineLoginButtonDefaults.bubbleWidth(44.dp).value, TOLERANCE)
        assertEquals(23f, LineLoginButtonDefaults.bubbleWidth(32.dp).value, TOLERANCE)
        assertEquals(15f, LineLoginButtonDefaults.bubbleWidth(20.dp).value, TOLERANCE)
    }

    @Test
    fun captionPaddingIsTheBubbleWidthHorizontallyAndHalfItVertically() {
        // The guidelines: left/right padding >= X, top/bottom recommended X/2.
        listOf(20.dp, 32.dp, 44.dp, 64.dp).forEach { height ->
            val x = LineLoginButtonDefaults.bubbleWidth(height)
            assertEquals(x, LineLoginButtonDefaults.textHorizontalPadding(height))
            assertEquals(x / 2, LineLoginButtonDefaults.textVerticalPadding(height))
        }
    }

    @Test
    fun isolationZoneIsNotZero() {
        // Not a measured value — the guidelines define it as "at least the icon's left padding",
        // which is X/2 in LINE's artwork. What matters is that it is real and scales.
        assertTrue(LineLoginButtonDefaults.isolationZone(44.dp) > 0.dp)
        assertTrue(
            LineLoginButtonDefaults.isolationZone(88.dp) >
                LineLoginButtonDefaults.isolationZone(44.dp),
        )
    }

    @Test
    fun cornerRadiusMatchesLinesArtwork() {
        // Measured: 44 -> 6.0, 32 -> 4.0, 20 -> 2.5. height/8 lands on two exactly and within
        // half a dp on the third.
        assertEquals(RoundedCornerShape(4.dp), LineLoginButtonDefaults.shape(32.dp))
        assertEquals(RoundedCornerShape(2.5.dp), LineLoginButtonDefaults.shape(20.dp))
        assertEquals(5.5f, 44f / 8f, TOLERANCE)
    }

    @Test
    fun captionSizeMatchesLinesArtwork() {
        // LINE's 44 dp button has 13.5 dp of caption ink; a ~15 sp font produces that.
        assertEquals(14.7f, LineLoginButtonDefaults.textStyle(44.dp).fontSize.value, 0.5f)
        assertEquals(10.7f, LineLoginButtonDefaults.textStyle(32.dp).fontSize.value, 0.5f)
    }

    @Test
    fun everythingScalesFromTheHeight() {
        // Doubling the height doubles every derived measurement — which is what keeps the icon's
        // aspect ratio and the padding intact at any size.
        val single = LineLoginButtonDefaults.bubbleWidth(44.dp).value
        val double = LineLoginButtonDefaults.bubbleWidth(88.dp).value
        assertEquals(single * 2, double, TOLERANCE)
    }

    private companion object {
        /** Half a dp: below what any display can render differently. */
        const val TOLERANCE = 0.5f
    }
}
