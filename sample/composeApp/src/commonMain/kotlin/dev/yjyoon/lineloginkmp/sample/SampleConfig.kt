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
package dev.yjyoon.lineloginkmp.sample

/**
 * ⚠️ Put your own channel ID here before running the sample.
 *
 * Create a LINE Login channel at https://developers.line.biz/console/, then copy its Channel ID.
 * You also need to register, in that same console:
 *
 *  - Android: the package name `dev.yjyoon.lineloginkmp.sample` and your debug keystore's SHA-1;
 *  - iOS: the bundle identifier you set in `iosApp/Configuration/Config.xcconfig`.
 *
 * And the channel must be **published** — an unpublished channel fails login for everyone except
 * the channel's own testers, with an error that does not say so.
 *
 * The channel ID is not a secret: it ships inside every app that uses it. The channel *secret* is,
 * and no client app ever needs it.
 */
internal const val SAMPLE_LINE_CHANNEL_ID = "0000000000"
