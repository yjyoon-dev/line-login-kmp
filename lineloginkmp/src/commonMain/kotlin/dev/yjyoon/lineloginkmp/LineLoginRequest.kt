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
 * Options for a single [LineLogin.login] call.
 *
 * The defaults are what most apps want: sign in with `profile` + `openid`, through the LINE app
 * when it is installed and the browser when it is not.
 *
 * @property scopes Permissions to request. Must not be empty. What the user actually granted is
 *   not reported as a list — read it off the result instead: [LineLoginResult.Success.profile] is
 *   non-null when `profile` was granted, [LineLoginResult.Success.idToken] when `openid` was, and
 *   [LineIdToken.email] when `email` was.
 * @property nonce An OpenID Connect nonce, echoed back in the ID token so a backend can prove the
 *   token was minted for *this* login attempt. Generate it server-side and verify it there; a
 *   nonce a client both creates and checks proves nothing. Null lets the SDK supply its own.
 * @property forceWebLogin Skip app-to-app login and always use the web flow, even when the LINE
 *   app is installed. Mostly useful for testing the browser path.
 * @property botPrompt Ask the user to add your LINE Official Account as a friend during login.
 *   Requires the channel to be linked to a bot in the console; null shows no prompt.
 * @property promptBotId Which bot to prompt for, when the channel is linked to more than one.
 */
public class LineLoginRequest(
    public val scopes: Set<LineScope> = DefaultScopes,
    public val nonce: String? = null,
    public val forceWebLogin: Boolean = false,
    public val botPrompt: LineBotPrompt? = null,
    public val promptBotId: String? = null,
) {
    init {
        require(scopes.isNotEmpty()) {
            "scopes must not be empty. LINE rejects an authorization request with no scope."
        }
    }

    override fun toString(): String =
        "LineLoginRequest(scopes=$scopes, nonce=${if (nonce == null) "null" else "***"}, " +
            "forceWebLogin=$forceWebLogin, botPrompt=$botPrompt, promptBotId=$promptBotId)"

    public companion object {
        /** [LineScope.Profile] + [LineScope.OpenId]: an identifiable user plus a verifiable token. */
        public val DefaultScopes: Set<LineScope> = setOf(LineScope.Profile, LineScope.OpenId)
    }
}

/**
 * How insistently to ask the user to add your LINE Official Account as a friend.
 *
 * @see LineLoginRequest.botPrompt
 */
public enum class LineBotPrompt {
    /** Shows the option on the consent screen; the user can ignore it. */
    Normal,

    /** Opens a dedicated screen after consent. Harder to miss, easier to resent. */
    Aggressive,
}
