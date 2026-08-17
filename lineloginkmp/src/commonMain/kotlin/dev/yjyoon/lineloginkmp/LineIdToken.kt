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
 * The OpenID Connect ID token issued when [LineScope.OpenId] is granted.
 *
 * This is the one part of a login result that carries proof of its own authenticity: LINE signs
 * it, so a backend can verify it without trusting the app. Send [rawValue] to your server and
 * validate it there against `https://api.line.me/oauth2/v2.1/certs` — checking the signature, the
 * `aud` (your channel ID), the `iss`, the `exp`, and the `nonce` you issued.
 *
 * The decoded properties below are read straight out of the payload **without verifying the
 * signature**. They exist so you can greet the user immediately after login. Never use them for
 * an authorization decision, and never trust them on a server.
 *
 * @property rawValue The JWT exactly as LINE issued it. This is what your backend needs.
 * @property subject The `sub` claim — the same value as [LineProfile.userId].
 * @property name The `name` claim. Present only when [LineScope.Profile] was also granted.
 * @property pictureUrl The `picture` claim.
 * @property email The `email` claim. Present only when [LineScope.Email] was granted and the
 *   channel has the email permission approved.
 * @property nonce The `nonce` claim, echoing [LineLoginRequest.nonce].
 */
public class LineIdToken(
    public val rawValue: String,
    public val subject: String?,
    public val name: String?,
    public val pictureUrl: String?,
    public val email: String?,
    public val nonce: String?,
) {
    override fun toString(): String = "LineIdToken(rawValue=***, subject=$subject)"
}
