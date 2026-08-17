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

import android.content.Context
import androidx.startup.Initializer

/**
 * Holds the application [Context], so `LineLogin.configure(...)` can be called from shared code
 * with no Android types in its signature.
 *
 * Only ever an *application* context, so there is nothing here that can leak an Activity.
 */
internal object LineLoginContext {
    @Volatile
    private var applicationContext: Context? = null

    fun install(context: Context) {
        applicationContext = context.applicationContext
    }

    fun peek(): Context? = applicationContext
}

/**
 * Captures the application [Context] at process start.
 *
 * This is why `LineLogin.configure(LineLoginConfig(...))` needs no `Context` parameter and can
 * live in shared code. `androidx.startup` merges the provider into the consumer's manifest, so
 * there is nothing for them to declare.
 *
 * Apps that strip `androidx.startup.InitializationProvider` (some do, to shave a few milliseconds
 * off cold start) must call the Android-only `LineLogin.configure(context, config)` instead. That
 * is the only consequence.
 *
 * Public because `androidx.startup` instantiates it reflectively from a manifest string — it is
 * not part of this library's API, and `consumer-rules.pro` keeps it from being stripped.
 */
public class LineLoginInitializer : Initializer<Unit> {
    override fun create(context: Context) {
        LineLoginContext.install(context)
    }

    override fun dependencies(): List<Class<out Initializer<*>>> = emptyList()
}
