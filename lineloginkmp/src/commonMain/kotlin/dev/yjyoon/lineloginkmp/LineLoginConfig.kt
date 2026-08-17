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
 * Everything the LINE SDKs need to identify your app.
 *
 * Pass it to [LineLogin.configure] once, before any other call.
 *
 * @property channelId The LINE Login channel ID from the
 *   [LINE Developers Console](https://developers.line.biz/console/). This is a public identifier —
 *   it ships inside every APK and IPA either way — so hard-coding it is fine. The channel
 *   *secret* is a different value, is confidential, and is never needed by a client app.
 * @property universalLinkUrl iOS only, ignored on Android. When set, LINE returns to your app
 *   through this universal link instead of the `line3rdp.<bundle id>` URL scheme, which removes
 *   the "Open in …?" confirmation dialog during app-to-app login. It must be a URL your app
 *   already handles via an `applinks:` associated domain, and the same URL must be registered in
 *   the console. Leave it null to use the URL scheme.
 */
public class LineLoginConfig(
    public val channelId: String,
    public val universalLinkUrl: String? = null,
) {
    init {
        require(channelId.isNotBlank()) {
            "channelId must not be blank. Copy it from the LINE Developers Console."
        }
        require(universalLinkUrl == null || universalLinkUrl.isNotBlank()) {
            "universalLinkUrl must be a URL or null, not blank."
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is LineLoginConfig) return false
        return channelId == other.channelId && universalLinkUrl == other.universalLinkUrl
    }

    override fun hashCode(): Int = 31 * channelId.hashCode() + universalLinkUrl.hashCode()

    override fun toString(): String = "LineLoginConfig(channelId=$channelId, universalLinkUrl=$universalLinkUrl)"
}
