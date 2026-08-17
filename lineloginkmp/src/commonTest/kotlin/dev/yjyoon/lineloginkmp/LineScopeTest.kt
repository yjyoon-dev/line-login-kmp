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
import kotlin.test.assertTrue

class LineScopeTest {
    @Test
    fun constantsCarryTheWireValuesLineExpects() {
        assertEquals("profile", LineScope.Profile.code)
        assertEquals("openid", LineScope.OpenId.code)
        assertEquals("email", LineScope.Email.code)
    }

    @Test
    fun scopesAreComparedByCodeSoTheyWorkInSets() {
        assertEquals(LineScope.Profile, LineScope("profile"))
        assertEquals(LineScope.Profile.hashCode(), LineScope("profile").hashCode())
        assertNotEquals(LineScope.Profile, LineScope.OpenId)

        val scopes = setOf(LineScope("profile"), LineScope.Profile, LineScope.OpenId)
        assertEquals(2, scopes.size)
        assertTrue(LineScope("openid") in scopes)
    }

    @Test
    fun toStringIsTheCodeItself() {
        assertEquals("profile", LineScope.Profile.toString())
    }

    @Test
    fun customScopesAreAllowedBecauseLineKeepsAddingThem() {
        assertEquals("openchat", LineScope("openchat").code)
    }

    @Test
    fun blankScopesAreRejected() {
        assertFailsWith<IllegalArgumentException> { LineScope("") }
        assertFailsWith<IllegalArgumentException> { LineScope("   ") }
    }
}
