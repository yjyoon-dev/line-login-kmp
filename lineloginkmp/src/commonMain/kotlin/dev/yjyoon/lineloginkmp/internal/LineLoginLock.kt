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
package dev.yjyoon.lineloginkmp.internal

/**
 * Runs [block] with no other caller inside it, on either platform.
 *
 * Exists for `LineLogin.configure`, which reads the current configuration, validates it, sets up
 * the platform SDK and stores the result — four steps that have to look like one. A `@Volatile`
 * field makes each step visible across threads but does nothing to keep two callers from
 * interleaving them, and the consequences are platform-specific and unpleasant: the iOS SDK's setup
 * is one-shot and raises an assertion failure if it runs twice, and two callers passing *different*
 * channel IDs could both pass the "already configured with another channel" check and silently
 * leave the process set up for one channel while this library believes it is the other.
 *
 * A blocking lock rather than a `Mutex`: `configure` is not a suspending function, and cannot become
 * one without breaking every caller that runs it from `Application.onCreate` or an iOS initialiser.
 * It is held only for the duration of a one-time setup, so there is nothing to contend with.
 *
 * Not reentrant. Nothing inside a locked section may take the lock again.
 */
internal expect fun <T> withLineLoginLock(block: () -> T): T
