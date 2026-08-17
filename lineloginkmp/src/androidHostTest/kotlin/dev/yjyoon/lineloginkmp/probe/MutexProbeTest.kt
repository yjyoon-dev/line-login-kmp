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
package dev.yjyoon.lineloginkmp.probe

import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import dev.yjyoon.lineloginkmp.LineLogin
import dev.yjyoon.lineloginkmp.LineLoginConfig
import dev.yjyoon.lineloginkmp.LineLoginErrorCode
import dev.yjyoon.lineloginkmp.LineLoginResult
import dev.yjyoon.lineloginkmp.internal.LineLoginContext
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

private class FakeContext : ContextWrapper(null) {
    override fun getApplicationContext(): Context = this

    override fun startActivity(intent: Intent?) { /* swallow: never actually starts */ }

    override fun getPackageName(): String = "dev.yjyoon.probe"
}

class MutexProbeTest {
    @Test
    fun probe() =
        runTest {
            LineLoginContext.install(FakeContext())
            LineLogin.configure(LineLoginConfig(channelId = "1234567890"))

            val first = async { LineLogin.login() }
            delay(1) // let `first` reach its suspension point inside platformLogin
            val second = LineLogin.login()
            println("PROBE-SECOND=$second")

            val firstResult = first.await()
            println("PROBE-FIRST=$firstResult")

            val third = LineLogin.login()
            println("PROBE-THIRD=$third")
        }
}
