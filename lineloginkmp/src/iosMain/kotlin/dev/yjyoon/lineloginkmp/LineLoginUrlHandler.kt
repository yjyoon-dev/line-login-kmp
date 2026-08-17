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
@file:OptIn(ExperimentalForeignApi::class)

package dev.yjyoon.lineloginkmp

import dev.yjyoon.lineloginkmp.internal.linesdk.LineSDKLoginManager
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSURL
import platform.UIKit.UIApplication

/**
 * The one thing LINE Login needs from your iOS app.
 *
 * When the LINE app is not installed, login happens in a web view and LINE hands control back
 * through a URL. Something in the app has to pass that URL on, or the login never completes —
 * the browser closes, and nothing happens.
 *
 * From SwiftUI:
 *
 * ```swift
 * ContentView()
 *     .onOpenURL { url in
 *         _ = LineLoginUrlHandler.shared.handle(url: url)
 *     }
 * ```
 *
 * From a UIKit `AppDelegate`, forward **both** entry points — the second one only matters when
 * `universalLinkUrl` is configured, which makes forgetting it fail intermittently and confusingly:
 *
 * ```swift
 * func application(_ app: UIApplication, open url: URL, options: …) -> Bool {
 *     LineLoginUrlHandler.shared.handle(url: url)
 * }
 *
 * func application(_ app: UIApplication, continue userActivity: NSUserActivity, …) -> Bool {
 *     guard let url = userActivity.webpageURL else { return false }
 *     return LineLoginUrlHandler.shared.handle(url: url)
 * }
 * ```
 *
 * A SwiftUI `App` is scene-based, and UIKit delivers incoming URLs to the scene — so in a SwiftUI
 * app `application(_:open:options:)` is never called and `.onOpenURL` is the only thing that
 * works.
 */
public object LineLoginUrlHandler {
    /**
     * Hands [url] to the LINE SDK.
     *
     * @return true if LINE consumed it. The SDK returns false for anything unrelated to a login
     *   it started, so forwarding every URL your app receives is safe.
     */
    public fun handle(url: NSURL): Boolean =
        LineSDKLoginManager.sharedManager().application(
            app = UIApplication.sharedApplication,
            open = url,
            options = emptyMap<Any?, Any>(),
        )
}
