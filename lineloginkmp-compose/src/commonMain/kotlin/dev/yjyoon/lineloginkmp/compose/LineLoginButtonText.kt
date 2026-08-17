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

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.intl.Locale

/**
 * The two button captions LINE publishes for a language.
 *
 * @property long The full phrase, e.g. "Log in with LINE". LINE's recommended default.
 * @property short The compact phrase, e.g. "Log in", for buttons too narrow for the long one.
 */
public class LineLoginButtonLabel internal constructor(
    public val long: String,
    public val short: String,
) {
    override fun toString(): String = "LineLoginButtonLabel(long=$long, short=$short)"
}

/**
 * LINE's recommended login button captions, verbatim from its
 * [button design guidelines](https://developers.line.biz/en/docs/line-login/login-button/).
 *
 * Using these is the easiest way to stay compliant: the guidelines require text that clearly says
 * the button logs in with LINE, and translating that yourself tends to drift. If you do write your
 * own, keep it to a single line with no line breaks.
 */
public object LineLoginButtonText {
    /** Every language LINE publishes a caption for, keyed by the tag LINE uses. */
    public val Labels: Map<String, LineLoginButtonLabel> =
        mapOf(
            "en" to LineLoginButtonLabel("Log in with LINE", "Log in"),
            "ja" to LineLoginButtonLabel("LINEでログイン", "ログイン"),
            "ko" to LineLoginButtonLabel("LINE으로 로그인", "로그인"),
            "de" to LineLoginButtonLabel("Mit LINE anmelden", "Anmelden"),
            "es" to LineLoginButtonLabel("Iniciar sesión con LINE", "Iniciar sesión"),
            "fr" to LineLoginButtonLabel("Connexion avec LINE", "Se connecter"),
            "id" to LineLoginButtonLabel("Masuk dengan LINE", "Masuk"),
            "it" to LineLoginButtonLabel("Login con LINE", "Login"),
            "ms" to LineLoginButtonLabel("Log masuk dengan LINE", "Log Masuk"),
            "pt-BR" to LineLoginButtonLabel("Login com o LINE", "Login"),
            "pt-PT" to LineLoginButtonLabel("Iniciar sessão com o LINE", "Iniciar sessão"),
            "ru" to LineLoginButtonLabel("Войти в LINE", "Войти"),
            "th" to LineLoginButtonLabel("ล็อกอินด้วย LINE", "ล็อกอิน"),
            "tr" to LineLoginButtonLabel("LINE ile oturum açın", "Oturum Aç"),
            "ar" to LineLoginButtonLabel("تسجيل دخول باستخدام LINE", "تسجيل دخول"),
            "vi" to LineLoginButtonLabel("Đăng nhập với LINE", "Đăng nhập"),
            "zh-CN" to LineLoginButtonLabel("用LINE帐号登录", "登录"),
            "zh-TW" to LineLoginButtonLabel("與LINE連動", "連動"),
        )

    /** The English caption, and the fallback for any language LINE has not published one for. */
    public val Default: LineLoginButtonLabel = Labels.getValue("en")

    /**
     * The caption for [languageTag], falling back to [Default].
     *
     * Accepts anything a platform locale produces — `ko`, `ko-KR`, `zh-Hant-TW`, `pt_BR` — and
     * resolves the two languages whose caption depends on the region: Portuguese splits into
     * Brazilian and European, and Chinese into Simplified and Traditional.
     */
    public fun of(languageTag: String): LineLoginButtonLabel {
        val parts = languageTag.split('-', '_').filter { it.isNotBlank() }
        val language = parts.firstOrNull()?.lowercase() ?: return Default
        val subtags = parts.drop(1).map { it.uppercase() }

        return when (language) {
            "pt" -> Labels.getValue(if ("BR" in subtags) "pt-BR" else "pt-PT")
            "zh" -> Labels.getValue(if (subtags.any { it in TRADITIONAL_CHINESE }) "zh-TW" else "zh-CN")
            else -> Labels[language] ?: Default
        }
    }

    /** The caption for the locale the app is currently running in. */
    @Composable
    public fun current(): LineLoginButtonLabel = of(Locale.current.toLanguageTag())

    /** Regions and scripts written in Traditional Chinese. */
    private val TRADITIONAL_CHINESE = setOf("TW", "HK", "MO", "HANT")
}
