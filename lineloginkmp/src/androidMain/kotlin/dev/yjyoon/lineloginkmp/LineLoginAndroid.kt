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

import android.content.Context
import android.content.pm.PackageManager
import com.linecorp.linesdk.Constants
import dev.yjyoon.lineloginkmp.internal.LineLoginContext

/**
 * Configures LINE Login with an explicit [Context].
 *
 * Only needed if your app removes `androidx.startup.InitializationProvider` from its merged
 * manifest — otherwise the shared `LineLogin.configure(config)` already has everything it needs.
 *
 * Any `Context` works; only its application context is retained.
 */
public fun LineLogin.configure(
    context: Context,
    config: LineLoginConfig,
) {
    LineLoginContext.install(context)
    configure(config)
}

/**
 * Whether the LINE app is installed on this device.
 *
 * Useful for deciding what to put on a button — logging in works either way, falling back to a
 * browser when LINE is missing.
 *
 * Needs no `<queries>` entry: the LINE SDK's own manifest already declares `jp.naver.line.android`
 * for Android 11+ package visibility.
 */
public fun LineLogin.isLineAppInstalled(context: Context): Boolean =
    try {
        context.packageManager.getPackageInfo(Constants.LINE_APP_PACKAGE_NAME, 0)
        true
    } catch (notInstalled: PackageManager.NameNotFoundException) {
        false
    }
