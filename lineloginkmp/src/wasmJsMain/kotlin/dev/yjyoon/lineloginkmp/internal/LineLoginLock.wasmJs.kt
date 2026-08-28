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
 * The browser is single-threaded, so mutual exclusion is free: nothing can interleave with [block]
 * because nothing else runs while it does. The lock exists on this target only to satisfy the
 * shared contract.
 */
internal actual fun <T> withLineLoginLock(block: () -> T): T = block()
