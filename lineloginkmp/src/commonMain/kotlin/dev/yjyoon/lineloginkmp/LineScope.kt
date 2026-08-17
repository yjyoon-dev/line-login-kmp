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

/**
 * A LINE Login permission scope.
 *
 * Deliberately an open value type rather than an enum: LINE adds scopes over time, and both
 * native SDKs model them as open values too (`Scope(String)` on Android, `LoginPermission` on
 * iOS). An enum here would mean a release of this library for every scope LINE invents.
 *
 * The three scopes exposed as constants are the ones available to every channel. Anything else —
 * including scopes that need extra approval — goes through the constructor:
 *
 * ```
 * LineLoginRequest(scopes = setOf(LineScope.Profile, LineScope.OpenId, LineScope("openchat")))
 * ```
 *
 * @property code The wire value, exactly as LINE defines it.
 */
public class LineScope(
    public val code: String,
) {
    init {
        require(code.isNotBlank()) { "A scope code must not be blank." }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        return other is LineScope && code == other.code
    }

    override fun hashCode(): Int = code.hashCode()

    override fun toString(): String = code

    public companion object {
        /**
         * `profile` — the user's ID, display name, and picture.
         *
         * Without it a login still succeeds, but [LineLoginResult.Success.profile] is null and
         * there is no user ID to identify the account by.
         */
        public val Profile: LineScope = LineScope("profile")

        /**
         * `openid` — issues an OpenID Connect ID token alongside the access token.
         *
         * Request it whenever a backend has to verify who signed in:
         * [LineLoginResult.Success.idToken] is the only part of a login result a server can
         * validate on its own.
         */
        public val OpenId: LineScope = LineScope("openid")

        /**
         * `email` — adds the user's email address to the ID token.
         *
         * Requires `openid`, and requires the "Email address permission" to be applied for and
         * granted for the channel in the LINE Developers Console. Requesting it without that
         * approval fails the login rather than silently omitting the claim.
         */
        public val Email: LineScope = LineScope("email")
    }
}
