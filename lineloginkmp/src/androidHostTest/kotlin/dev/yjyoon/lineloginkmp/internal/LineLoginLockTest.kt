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

import dev.yjyoon.lineloginkmp.LineLogin
import dev.yjyoon.lineloginkmp.LineLoginConfig
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import kotlin.concurrent.thread
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The lock behind `LineLogin.configure`.
 *
 * Both tests start every thread from a latch, so they contend for real rather than running one
 * after another.
 */
class LineLoginLockTest {
    @AfterTest
    fun tearDown() {
        LineLogin.resetForTest()
    }

    @Test
    fun `serialises its callers`() {
        val threadCount = 16
        val incrementsPerThread = 2_000
        // Deliberately not atomic: an unsynchronised read-modify-write is what loses increments,
        // and losing them is what a broken lock looks like.
        var counter = 0

        runContended(threadCount) {
            repeat(incrementsPerThread) {
                withLineLoginLock { counter += 1 }
            }
        }

        assertEquals(threadCount * incrementsPerThread, counter)
    }

    @Test
    fun `configure never accepts two channels at once`() {
        val perChannel = 8
        val channels = List(perChannel) { "1111111111" } + List(perChannel) { "2222222222" }
        val rejected = CopyOnWriteArrayList<String>()

        runContended(channels.size) { index ->
            try {
                LineLogin.configure(LineLoginConfig(channelId = channels[index]))
            } catch (refused: IllegalStateException) {
                rejected += channels[index]
            }
        }

        // Whichever channel got there first, every caller holding the other one must have been
        // refused — that is the contract configure documents. Interleaved callers could each pass
        // the "already configured" check and leave this short.
        assertEquals(perChannel, rejected.size, "refused: $rejected")
        assertEquals(1, rejected.distinct().size, "both channels were refused: $rejected")
        assertTrue(LineLogin.isConfigured)
    }

    private fun runContended(
        threadCount: Int,
        block: (index: Int) -> Unit,
    ) {
        val start = CountDownLatch(1)
        val threads =
            List(threadCount) { index ->
                thread {
                    start.await()
                    block(index)
                }
            }
        start.countDown()
        threads.forEach(Thread::join)
    }
}
