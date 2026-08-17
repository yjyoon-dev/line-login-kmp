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
 * The caption table and its locale resolution.
 *
 * Worth testing because both failure modes are silent: a wrong tag shows English to someone who
 * expected their own language, and a mangled string ships a caption LINE never wrote.
 */
class LineLoginButtonTextTest {
    @Test
    fun capturesEveryLanguageLinePublishes() {
        assertEquals(18, LineLoginButtonText.Labels.size)
    }

    @Test
    fun captionsAreVerbatim() {
        assertEquals("Log in with LINE", LineLoginButtonText.Labels.getValue("en").long)
        assertEquals("LINE으로 로그인", LineLoginButtonText.Labels.getValue("ko").long)
        assertEquals("로그인", LineLoginButtonText.Labels.getValue("ko").short)
        assertEquals("LINEでログイン", LineLoginButtonText.Labels.getValue("ja").long)
        assertEquals("與LINE連動", LineLoginButtonText.Labels.getValue("zh-TW").long)
    }

    @Test
    fun noCaptionContainsALineBreak() {
        // The guidelines forbid line breaks outright.
        LineLoginButtonText.Labels.forEach { (tag, label) ->
            assertTrue('\n' !in label.long && '\n' !in label.short, tag)
            assertTrue(label.long.isNotBlank() && label.short.isNotBlank(), tag)
        }
    }

    @Test
    fun resolvesPlainLanguageTags() {
        assertEquals(LineLoginButtonText.Labels.getValue("ko"), LineLoginButtonText.of("ko"))
        assertEquals(LineLoginButtonText.Labels.getValue("th"), LineLoginButtonText.of("th-TH"))
        assertEquals(LineLoginButtonText.Labels.getValue("ja"), LineLoginButtonText.of("ja_JP"))
    }

    @Test
    fun resolvesTheTwoRegionSensitiveLanguages() {
        assertEquals(LineLoginButtonText.Labels.getValue("pt-BR"), LineLoginButtonText.of("pt-BR"))
        assertEquals(LineLoginButtonText.Labels.getValue("pt-PT"), LineLoginButtonText.of("pt-PT"))
        // Portuguese with no region: European, matching LINE's own "pt-PT" default ordering.
        assertEquals(LineLoginButtonText.Labels.getValue("pt-PT"), LineLoginButtonText.of("pt"))

        assertEquals(LineLoginButtonText.Labels.getValue("zh-CN"), LineLoginButtonText.of("zh"))
        assertEquals(LineLoginButtonText.Labels.getValue("zh-CN"), LineLoginButtonText.of("zh-Hans-CN"))
        assertEquals(LineLoginButtonText.Labels.getValue("zh-TW"), LineLoginButtonText.of("zh-TW"))
        assertEquals(LineLoginButtonText.Labels.getValue("zh-TW"), LineLoginButtonText.of("zh-HK"))
        assertEquals(LineLoginButtonText.Labels.getValue("zh-TW"), LineLoginButtonText.of("zh-Hant"))
    }

    @Test
    fun fallsBackToEnglishRatherThanFailing() {
        assertEquals(LineLoginButtonText.Default, LineLoginButtonText.of("sv"))
        assertEquals(LineLoginButtonText.Default, LineLoginButtonText.of(""))
        assertEquals(LineLoginButtonText.Default, LineLoginButtonText.of("-"))
        assertEquals("Log in with LINE", LineLoginButtonText.Default.long)
    }
}
