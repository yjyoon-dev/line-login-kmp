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

import dev.yjyoon.lineloginkmp.LineLoginResult
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The handoff between the caller's coroutine and the proxy Activity.
 *
 * Everything here is about the paths that are painful to reproduce on a device: a result arriving
 * after the caller gave up, two results arriving, a request nobody is waiting for any more.
 */
class LineLoginRequestBusTest {
    @Test
    fun eachLoginGetsItsOwnIdAndIsFindableUntilClosed() {
        val (firstId, first) = LineLoginRequestBus.open()
        val (secondId, _) = LineLoginRequestBus.open()

        assertNotEquals(firstId, secondId)
        assertEquals(first, LineLoginRequestBus.find(firstId))

        LineLoginRequestBus.close(firstId)
        LineLoginRequestBus.close(secondId)

        // Null is what the proxy Activity checks to decide not to reopen a login nobody wants.
        assertNull(LineLoginRequestBus.find(firstId))
        assertNull(LineLoginRequestBus.find(secondId))
    }

    @Test
    fun theFirstOutcomeWinsAndLaterOnesAreIgnored() =
        runTest {
            val (requestId, pending) = LineLoginRequestBus.open()
            try {
                assertTrue(pending.outcome.complete(LineLoginResult.Cancelled))
                // A recreated Activity redelivering its result must not blow up a second time.
                assertFalse(
                    pending.outcome.complete(
                        LineLoginResult.Failure(
                            dev.yjyoon.lineloginkmp.LineLoginErrorCode.Internal,
                            "second",
                        ),
                    ),
                )
                assertEquals(LineLoginResult.Cancelled, pending.outcome.await())
            } finally {
                LineLoginRequestBus.close(requestId)
            }
        }

    @Test
    fun startedIsSignalledOnceTheProxyIsAttached() =
        runTest {
            val (requestId, pending) = LineLoginRequestBus.open()
            try {
                assertFalse(pending.started.isCompleted)

                // The real attach() takes an Activity; the signal is what the caller waits on to
                // tell "the system dropped my start" from "the user is taking their time".
                pending.started.complete(Unit)

                assertTrue(pending.started.isCompleted)
                pending.started.await()
            } finally {
                LineLoginRequestBus.close(requestId)
            }
        }

    @Test
    fun finishingAProxyThatWasNeverAttachedIsHarmless() {
        val (requestId, pending) = LineLoginRequestBus.open()
        try {
            pending.finishProxy()
            pending.detach()
            pending.finishProxy()
        } finally {
            LineLoginRequestBus.close(requestId)
        }
    }

    @Test
    fun closingTwiceIsHarmless() {
        val (requestId, _) = LineLoginRequestBus.open()
        LineLoginRequestBus.close(requestId)
        LineLoginRequestBus.close(requestId)
        assertNull(LineLoginRequestBus.find(requestId))
    }
}
