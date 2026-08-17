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

import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import dev.yjyoon.lineloginkmp.LineLogin
import dev.yjyoon.lineloginkmp.LineLoginConfig

/**
 * Configuration happens here, in shared-code terms: no `Context`, no Android types.
 *
 * The application `Context` the LINE SDK needs is picked up by an `androidx.startup` initialiser
 * this library merges into the manifest, so `configure` looks the same on both platforms.
 */
class SampleApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        LineLogin.configure(LineLoginConfig(channelId = SAMPLE_LINE_CHANNEL_ID))
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { App() }
    }
}
