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
 * The signed-in user, as LINE describes them.
 *
 * Present only when [LineScope.Profile] was granted.
 *
 * @property userId Stable identifier, formatted `U` followed by 32 hex characters. It is scoped
 *   to the **provider** that owns the channel, not to the channel itself: every channel under one
 *   provider sees the same ID for the same person, and moving a channel to another provider
 *   changes it for everyone.
 * @property displayName The user's LINE display name. They can change it at any time, so treat it
 *   as a label, never as a key.
 * @property pictureUrl Profile image. Append `/large` or `/small` for other sizes. Null when the
 *   user has no picture set.
 * @property statusMessage The user's status message, if any.
 */
public class LineProfile(
    public val userId: String,
    public val displayName: String,
    public val pictureUrl: String?,
    public val statusMessage: String?,
) {
    override fun toString(): String = "LineProfile(userId=$userId, displayName=$displayName)"
}
