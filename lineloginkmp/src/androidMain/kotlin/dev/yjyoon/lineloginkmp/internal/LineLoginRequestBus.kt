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

import android.app.Activity
import android.os.Handler
import android.os.Looper
import dev.yjyoon.lineloginkmp.LineLoginResult
import kotlinx.coroutines.CompletableDeferred
import java.lang.ref.WeakReference
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * The rendezvous between the coroutine that asked for a login and the Activity that runs it.
 *
 * The LINE SDK offers only the `startActivityForResult` contract, and shared code has no Activity
 * to start it from — just an application `Context`. So each login creates the Activity it needs
 * ([LineLoginProxyActivity]), which finishes the moment it has a result, and the two halves find
 * each other through this map.
 *
 * Deliberately process-level and deliberately free of any `Context`:
 *  - process-level, so it survives the proxy being destroyed and recreated by a configuration
 *    change or "Don't keep activities";
 *  - `Context`-free, so it structurally cannot leak one. The only Android reference it holds is a
 *    [WeakReference] to the live proxy, used to close it on cancellation.
 *
 * It intentionally does not survive process death: after a kill there is no coroutine left to
 * resume, and the proxy checks for exactly that before reopening any UI.
 */
internal object LineLoginRequestBus {
    private val sequence = AtomicLong(0L)
    private val pending = ConcurrentHashMap<String, PendingLogin>()

    /**
     * Unique per process, so an Intent left over from a previous process can never be mistaken for
     * a live request in this one — a counter alone restarts from zero on every process start.
     */
    private val processTag = UUID.randomUUID().toString().take(8)

    /** Registers a new login and returns its id. Always paired with a [close] in a `finally`. */
    fun open(): Pair<String, PendingLogin> {
        val requestId = "line-login-$processTag-${sequence.incrementAndGet()}"
        val login = PendingLogin()
        pending[requestId] = login
        return requestId to login
    }

    /** Null once the caller has given up, or after process death. */
    fun find(requestId: String): PendingLogin? = pending[requestId]

    fun close(requestId: String) {
        pending.remove(requestId)
    }

    class PendingLogin {
        /** Completed by the proxy's `onCreate`, so the caller can tell a dropped start from a slow user. */
        val started: CompletableDeferred<Unit> = CompletableDeferred()

        /** Completed exactly once, by the proxy's activity result. Extra completions are ignored. */
        val outcome: CompletableDeferred<LineLoginResult> = CompletableDeferred()

        @Volatile
        private var activity: WeakReference<Activity>? = null

        fun attach(activity: Activity) {
            this.activity = WeakReference(activity)
            started.complete(Unit)
        }

        fun detach() {
            activity = null
        }

        /**
         * Closes the proxy after the caller's coroutine was cancelled, so no invisible Activity is
         * left sitting in the task.
         *
         * Best-effort by nature: if LINE's own screen is already on top it stays there until the
         * user dismisses it, and a login they then complete is persisted by the SDK even though
         * this result is discarded.
         */
        fun finishProxy() {
            val target = activity?.get() ?: return
            activity = null
            if (Looper.myLooper() == Looper.getMainLooper()) {
                target.finish()
            } else {
                Handler(Looper.getMainLooper()).post { target.finish() }
            }
        }
    }
}

internal const val EXTRA_REQUEST_ID: String = "dev.yjyoon.lineloginkmp.REQUEST_ID"
internal const val EXTRA_CHANNEL_ID: String = "dev.yjyoon.lineloginkmp.CHANNEL_ID"
internal const val EXTRA_SCOPES: String = "dev.yjyoon.lineloginkmp.SCOPES"
internal const val EXTRA_NONCE: String = "dev.yjyoon.lineloginkmp.NONCE"
internal const val EXTRA_FORCE_WEB_LOGIN: String = "dev.yjyoon.lineloginkmp.FORCE_WEB_LOGIN"
internal const val EXTRA_BOT_PROMPT: String = "dev.yjyoon.lineloginkmp.BOT_PROMPT"
internal const val EXTRA_PROMPT_BOT_ID: String = "dev.yjyoon.lineloginkmp.PROMPT_BOT_ID"
