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
package dev.yjyoon.lineloginkmp

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertNull

class LineLoginConfigTest {
    @Test
    fun blankChannelIdIsRejectedAtConstruction() {
        // Caught here rather than inside the Android SDK, which throws
        // IllegalArgumentException("channel id is empty") from a builder several frames away.
        assertFailsWith<IllegalArgumentException> { LineLoginConfig(channelId = "") }
        assertFailsWith<IllegalArgumentException> { LineLoginConfig(channelId = "  ") }
    }

    @Test
    fun blankUniversalLinkIsRejectedButNullIsFine() {
        assertFailsWith<IllegalArgumentException> {
            LineLoginConfig(channelId = "1234567890", universalLinkUrl = " ")
        }
        assertNull(LineLoginConfig(channelId = "1234567890").universalLinkUrl)
    }

    @Test
    fun configsAreComparedByValueSoReconfiguringWithTheSameThingIsANoOp() {
        assertEquals(LineLoginConfig("1234567890"), LineLoginConfig("1234567890"))
        assertEquals(LineLoginConfig("1234567890").hashCode(), LineLoginConfig("1234567890").hashCode())
        assertNotEquals(LineLoginConfig("1234567890"), LineLoginConfig("9876543210"))
        assertNotEquals(
            LineLoginConfig("1234567890"),
            LineLoginConfig("1234567890", universalLinkUrl = "https://example.com/line"),
        )
    }
}

class LineLoginRequestTest {
    @Test
    fun defaultsToProfileAndOpenId() {
        assertEquals(setOf(LineScope.Profile, LineScope.OpenId), LineLoginRequest().scopes)
        assertNull(LineLoginRequest().nonce)
        assertEquals(false, LineLoginRequest().forceWebLogin)
        assertNull(LineLoginRequest().botPrompt)
    }

    @Test
    fun emptyScopesAreRejected() {
        // LINE rejects an authorization request with no scope, and the Android SDK's builder does
        // no validation of its own — it would send a request whose scope list is simply missing.
        assertFailsWith<IllegalArgumentException> { LineLoginRequest(scopes = emptySet()) }
    }

    @Test
    fun toStringDoesNotLeakTheNonce() {
        val rendered = LineLoginRequest(nonce = "super-secret-nonce").toString()
        assertEquals(false, rendered.contains("super-secret-nonce"))
    }

    @Test
    fun blankLiffIdIsRejectedButNullIsNot() {
        // Null means "no web target in this app" and is the default; blank is always a mistake.
        assertFailsWith<IllegalArgumentException> {
            LineLoginConfig(channelId = "123", liffId = " ")
        }
        LineLoginConfig(channelId = "123", liffId = null)
        LineLoginConfig(channelId = "123", liffId = "1234567890-abcdefgh")
    }

    @Test
    fun liffIdParticipatesInEqualitySoConfigureCanTreatARepeatAsANoOp() {
        val withLiff = LineLoginConfig(channelId = "123", liffId = "1234567890-abcdefgh")
        val withoutLiff = LineLoginConfig(channelId = "123")

        kotlin.test.assertEquals(withLiff, LineLoginConfig(channelId = "123", liffId = "1234567890-abcdefgh"))
        kotlin.test.assertNotEquals(withLiff, withoutLiff)
    }
}
