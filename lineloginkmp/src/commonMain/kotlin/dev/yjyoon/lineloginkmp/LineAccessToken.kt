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
 * An access token for LINE's own APIs.
 *
 * Both native SDKs persist this themselves — Keystore-backed storage on Android, the Keychain on
 * iOS — and refresh it in the background, so there is nothing to store and nothing to schedule.
 *
 * The matching **refresh token is deliberately not exposed**, on either platform. The SDKs own
 * rotation: iOS marks the property `unavailable` outright, and Android hides it behind an
 * auto-refreshing proxy. Reading it would only create a second, stale copy of state the SDK is
 * already managing.
 *
 * @property value The bearer token. Sending it to your own backend proves nothing about who the
 *   user is — anyone can post any string. Use [LineLoginResult.Success.idToken] for that.
 * @property expiresAtEpochMilliseconds When the token stops working, in milliseconds since the
 *   Unix epoch.
 */
public class LineAccessToken(
    public val value: String,
    public val expiresAtEpochMilliseconds: Long,
) {
    override fun toString(): String = "LineAccessToken(value=***, expiresAtEpochMilliseconds=$expiresAtEpochMilliseconds)"
}
