# line-login-kmp

[![Maven Central](https://img.shields.io/maven-central/v/dev.yjyoon.lineloginkmp/lineloginkmp?color=A97BFF&label=Maven%20Central)](https://central.sonatype.com/artifact/dev.yjyoon.lineloginkmp/lineloginkmp)
[![License](https://img.shields.io/badge/License-Apache%202.0-5675DF.svg)](https://www.apache.org/licenses/LICENSE-2.0)
[![Kotlin](https://img.shields.io/badge/Kotlin-Multiplatform-A97BFF.svg?logo=kotlin)](https://kotlinlang.org/docs/multiplatform.html)
[![Platform](https://img.shields.io/badge/Platform-Android-brightgreen.svg)](https://developer.android.com)
[![Platform](https://img.shields.io/badge/Platform-iOS-black.svg)](https://developer.apple.com/ios/)

**LINE Login for Kotlin Multiplatform.** One shared API, LINE's own native SDKs underneath.

```kotlin
LineLogin.configure(LineLoginConfig(channelId = "1234567890"))

when (val result = LineLogin.login()) {
    is LineLoginResult.Success -> println(result.profile?.displayName)
    LineLoginResult.Cancelled -> Unit
    is LineLoginResult.Failure -> println(result.message)
}
```

That is the whole API on both platforms — including the app-to-app flow, the browser fallback when
LINE is not installed, and token storage.

## ✨ Why

- **No Swift bridge.** The iOS half binds LINE's Objective-C layer through Kotlin/Native cinterop,
  so Kotlin drives the login itself. Your app needs [**one line of Swift**](#ios), not a protocol to
  implement.
- **Nothing to add to your Android manifest.** The proxy Activity, the `INTERNET` permission and
  the `Context` bootstrap are all merged in for you.
- **Cancelling is not an error.** [`Cancelled`](#result-model) is its own result, so backing out of
  a login never reaches your error handling.
- **Failures you can act on.** A stable error taxonomy across both platforms, with each SDK's own
  code and message passed through untouched for anything not yet categorised.
- **Real cancellation.** Cancel the calling coroutine and the login is abandoned properly — on iOS the
  LINE screen is dismissed with it; on Android the library's own Activity closes, though LINE's
  screen stays until the user dismisses it ([why](#-design-notes)).
- **No wrapper of your session.** Both SDKs already persist and refresh their own tokens. This
  library adds no second copy of that state.
- **A compliant login button, optional.** [`lineloginkmp-compose`](#-login-button) implements LINE's
  button design guidelines so you do not have to re-derive them from a PSD.

## 📋 Requirements

| | |
|---|---|
| Android | minSdk **24** — the floor LINE's own AAR declares |
| iOS | **15.0** — raised by LINE iOS SDK 5.17.0. LINE's docs still say 13.0; they are stale |
| Kotlin | **2.4.0**+ |
| LINE SDK | Android `5.13.0` (pulled in automatically) · iOS `5.17.0` (you add it via SPM) |

You also need a **LINE Login channel** from the
[LINE Developers Console](https://developers.line.biz/console/), with your Android package name and
signing-certificate SHA-1, and your iOS bundle identifier, registered on it — and the channel
**published**.

## 🚀 Installation

### 1. Gradle

```kotlin
// shared/build.gradle.kts
kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation("dev.yjyoon.lineloginkmp:lineloginkmp:1.0.0")

            // Optional: a Compose Multiplatform login button that follows LINE's design
            // guidelines. Skip it if you would rather build the login button yourself.
            implementation("dev.yjyoon.lineloginkmp:lineloginkmp-compose:1.0.0")
        }
    }
}
```

### 2. iOS: export the framework

Your shared module's iOS framework must **export** this library, or nothing from it appears in the
generated Objective-C header and your Swift code cannot see it:

```kotlin
kotlin {
    listOf(iosArm64(), iosSimulatorArm64()).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "Shared"
            isStatic = true                             // required — frameworks are dynamic by default
            export("dev.yjyoon.lineloginkmp:lineloginkmp:1.0.0")     // ← add this
        }
    }
    sourceSets {
        commonMain.dependencies {
            api("dev.yjyoon.lineloginkmp:lineloginkmp:1.0.0")        // `api`, so there is something to export
        }
    }
}
```

### 3. iOS: add the LINE SDK in Xcode

**File ▸ Add Package Dependencies…** → `https://github.com/line/line-sdk-ios-swift` → choose the
**`LineSDKObjC`** product.

> `LineSDKObjC` — not `LineSDK`. It is the same SDK behind an Objective-C facade, and it is the one
> Kotlin can call. Picking the wrong product fails at link time with a screenful of
> `Undefined symbols: _OBJC_CLASS_$__TtC11LineSDKObjC…`.
>
> CocoaPods is not supported: its podspec renames the module, which breaks the symbol names.

### 4. iOS: two `Info.plist` keys

```xml
<key>CFBundleURLTypes</key>
<array>
    <dict>
        <key>CFBundleTypeRole</key>
        <string>Editor</string>
        <key>CFBundleURLSchemes</key>
        <array>
            <string>line3rdp.$(PRODUCT_BUNDLE_IDENTIFIER)</string>
        </array>
    </dict>
</array>
<key>LSApplicationQueriesSchemes</key>
<array>
    <string>lineauth2</string>
</array>
```

The first is where LINE returns after a web login. The second lets the SDK detect the LINE app —
omit it and every user silently gets the slower browser flow, with nothing anywhere explaining why.

Android needs no manifest changes at all.

## 📖 Usage

### Common

```kotlin
// Once, at startup — App.onCreate on Android, your app's entry point on iOS, or any shared
// initialisation that runs before the first login.
LineLogin.configure(LineLoginConfig(channelId = "1234567890"))

suspend fun signIn() {
    when (val result = LineLogin.login()) {
        is LineLoginResult.Success -> {
            val userId = result.profile?.userId
            val idToken = result.idToken?.rawValue   // send this to your backend
        }

        // The user changed their mind. Show nothing.
        LineLoginResult.Cancelled -> Unit

        is LineLoginResult.Failure -> when (result.code) {
            LineLoginErrorCode.Network -> retryLater()
            LineLoginErrorCode.Authentication -> reportMisconfiguration(result.message)
            else -> showError(result.message)
        }
    }
}
```

Asking for more, or fewer, permissions:

```kotlin
LineLogin.login(
    LineLoginRequest(
        scopes = setOf(LineScope.Profile, LineScope.OpenId, LineScope.Email),
        nonce = nonceFromYourServer,
        botPrompt = LineBotPrompt.Normal,
    ),
)
```

Signing out, and checking whether anyone is signed in:

```kotlin
LineLogin.logout()
if (LineLogin.isLoggedIn()) { /* a token exists on this device */ }
val token = LineLogin.currentAccessToken()
```

### Android

Nothing to do. The application `Context` is picked up by an `androidx.startup` initialiser, so
`configure` works from shared code.

Two Android-only extras are available if you want them:

```kotlin
// Only if your app strips androidx.startup.InitializationProvider from its manifest.
LineLogin.configure(context, LineLoginConfig(channelId = "1234567890"))

// For deciding what to put on a button. Login works either way.
if (LineLogin.isLineAppInstalled(context)) { /* … */ }
```

`login()` must be called while your app is in the foreground — Android does not allow starting an
Activity from the background, and doing so returns a `Failure` saying exactly that.

### iOS

One line, so the SDK sees the callback that finishes a browser-based login:

```swift
import Shared   // your shared framework

ContentView()
    .onOpenURL { url in
        _ = LineLoginUrlHandler.shared.handle(url: url)
    }
```

A SwiftUI `App` is scene-based and UIKit delivers URLs to the scene, so `.onOpenURL` is the right
hook — `AppDelegate.application(_:open:options:)` is never called in an app shaped like that.

In a UIKit app, forward **both** entry points instead:

```swift
func application(_ app: UIApplication, open url: URL, options: […]) -> Bool {
    LineLoginUrlHandler.shared.handle(url: url)
}

func application(_ app: UIApplication, continue userActivity: NSUserActivity, …) -> Bool {
    guard let url = userActivity.webpageURL else { return false }
    return LineLoginUrlHandler.shared.handle(url: url)
}
```

The second one only matters when you configure `universalLinkUrl`, which is exactly what makes
forgetting it fail intermittently.

## 🟢 Login button

**LINE requires login buttons to follow its
[button design guidelines](https://developers.line.biz/en/docs/line-login/login-button/)** — the
colours, the divider, the padding, the isolation zone and the caption are all specified, and
"using a non-designated color" is called out as a mistake. Read them before you draw your own.

`lineloginkmp-compose` implements them:

```kotlin
LineLoginButton(onClick = { scope.launch { handle(LineLogin.login()) } })
```

That gives you LINE's own icon and caption, the exact palette including the hover and press overlays
and the white disabled state, the divider between logo and caption, and geometry taken from LINE's
button artwork. The caption follows the reader's language — 18 of them, in LINE's own wording.

```kotlin
LineLoginButton(
    onClick = ::signIn,
    enabled = !busy,
    text = LineLoginButtonText.current().short,   // "Log in" instead of "Log in with LINE"
    height = 32.dp,                               // scales the whole button
)

LineLoginButton(onClick = ::signIn, text = null)  // icon only, which the guidelines also allow
```

Scale through `height`: the icon, the divider, the corner radius, the padding and the caption size
are all derived from it, so the icon's aspect ratio and the required padding hold at any size. The
ratios come from measuring LINE's own 20/32/44 dp button images, and a rendered 44 dp button matches
that artwork to within a fraction of a dp.

`LineLoginButtonColors` and `LineLoginButtonDefaults` are public, so an app drawing its own button —
in Android Views or SwiftUI — can still take the exact values.

### The icon, and the terms that come with it

The LINE icon is bundled — the button needs no asset from you. It is the unmodified white icon from
LINE's official template, so the mark is LINE's own rather than a lookalike, which the guidelines
require.

That icon is a trademark of LY Corporation and is **not** covered by this project's Apache licence.
Using this button means LINE's
[Usage Guidelines for the LINE Login Button](https://terms2.line.me/LINE_Developers_Guidelines_for_Login_Button)
apply to your app too. See [NOTICE](NOTICE).

The isolation zone is the one rule the button cannot enforce from the inside: keep other content
`LineLoginButtonDefaults.isolationZone()` away from it.

## 🛠️ API

| Type | What it is |
|---|---|
| `LineLogin` | The entry point: `configure`, `isConfigured`, `login`, `logout`, `currentAccessToken`, `isLoggedIn` |
| `LineLoginConfig` | Channel ID, and an optional iOS universal link |
| `LineLoginRequest` | Per-login options: scopes, nonce, `forceWebLogin`, bot prompt |
| `LineScope` | `Profile`, `OpenId`, `Email`, or any scope LINE adds later |
| `LineLoginResult` | `Success` · `Cancelled` · `Failure` |
| `LineLogoutResult` | `Success` · `Failure` |
| `LineLoginErrorCode` | `NotConfigured` `Network` `Server` `Authentication` `LineAppUnavailable` `LoginInProgress` `Internal` |
| `LineProfile` | `userId`, `displayName`, `pictureUrl`, `statusMessage` |
| `LineIdToken` | The raw JWT plus locally decoded claims |
| `LineAccessToken` | Token value and expiry |
| `LineLoginUrlHandler` | iOS only. `handle(url:)` |

From `dev.yjyoon.lineloginkmp:lineloginkmp-compose`:

| Type | What it is |
|---|---|
| `LineLoginButton` | The [guideline-compliant button](#-login-button) |
| `LineLoginButtonColors` | LINE's exact palette, including the state overlays |
| `LineLoginButtonDefaults` | Geometry and typography, all derived from the icon width |
| `LineLoginButtonText` | LINE's recommended captions in 18 languages, resolved by locale |

<a name="result-model"></a>

### The result model

`login()` never throws. Every outcome is a value:

```kotlin
public sealed interface LineLoginResult {
    public class Success(
        public val accessToken: LineAccessToken,
        public val profile: LineProfile?,      // non-null if `profile` was granted
        public val idToken: LineIdToken?,      // non-null if `openid` was granted
        public val friendshipStatusChanged: Boolean,
        public val nonce: String?,
    ) : LineLoginResult

    public data object Cancelled : LineLoginResult

    public class Failure(
        public val code: LineLoginErrorCode,
        public val message: String,
        public val rawCode: String?,     // the native SDK's own code, verbatim
        public val rawMessage: String?,
    ) : LineLoginResult
}
```

The only exception that ever escapes is `CancellationException`, when your own coroutine is
cancelled. iOS dismisses the LINE screen with it. Android closes this library's own invisible
Activity but cannot dismiss LINE's screen once it is on top — and if the user finishes that
login anyway, LINE's SDK stores the token even though the call reported nothing, so
`isLoggedIn()` can be true after a cancelled login.

### Verifying a login on your server

Send **`result.idToken.rawValue`**, never the user ID and never the access token. The ID token is
the only part of a login result LINE signs, so it is the only part your backend can verify — against
`https://api.line.me/oauth2/v2.1/certs`, checking the signature, `aud`, `iss`, `exp`, and the nonce
you issued. A client-supplied user ID is just a string anyone can send.

The claims decoded on `LineIdToken` are **not** signature-verified. They are there so you can greet
the user immediately, and for nothing else.

## 🔧 Platform details

### Android

What the library merges into your manifest:

- `LineLoginProxyActivity` — a headless, translucent Activity that owns the SDK's
  `startActivityForResult` contract, because shared code has no Activity of its own.
- `androidx.startup` registration for the `Context` bootstrap.
- `android.permission.INTERNET`.

The LINE AAR itself adds its two auth Activities, an OpenChat Activity, and the Android 11+
`<queries>` entry for `jp.naver.line.android`. It also ships its own ProGuard rules, which AGP
applies to your build — including two app-wide ones (`-keepattributes *Annotation*` and a keep for
every `Parcelable.Creator`). That is LINE's doing, not this library's, but it is worth knowing about.

### iOS

- The framework must be **static** (`isStatic = true`). A dynamic one has to resolve LineSDKObjC's
  symbols while Kotlin links, long before Xcode has fetched the Swift package.
- Because a *static* framework defers those symbols, they are resolved when your **app** links —
  which is why you add `LineSDKObjC` in Xcode and not in Gradle.
- The LINE SDK ships its own `PrivacyInfo.xcprivacy`, so it needs no privacy-manifest work from you.

## 🩺 Troubleshooting

Keyed by what you actually see.

| Symptom | Cause |
|---|---|
| `cannot find 'LineLoginUrlHandler' in scope` | Missing `export("dev.yjyoon.lineloginkmp:lineloginkmp:…")` in your framework block, or `implementation` instead of `api`. |
| `Undefined symbols: _OBJC_CLASS_$__TtC11LineSDKObjC…` | Your framework is dynamic (set `isStatic = true` — it is **not** the default), the `LineSDKObjC` SPM product is not added to the app target, or the `LineSDK` product was added instead of it. |
| Login always opens the browser, never the LINE app | `lineauth2` is missing from `LSApplicationQueriesSchemes`. Nothing errors — `canOpenURL` just returns `false`. |
| The browser finishes the login and nothing happens | `.onOpenURL` is not wired up on iOS, or `line3rdp.$(PRODUCT_BUNDLE_IDENTIFIER)` is missing from `CFBundleURLTypes`. |
| `Failure(Authentication, …)` on every attempt | The console does not match this build: wrong package name, an unregistered signing SHA-1, a different bundle ID — or the channel is not published. |
| `Failure(Internal, "The LINE login screen did not start…")` | `login()` was called while the app was in the background on Android. |
| `Unknown iOS simulator arch: 'x86_64'` | An Intel simulator slice was requested for a project without an `iosX64` target. Add `EXCLUDED_ARCHS[sdk=iphonesimulator*] = x86_64`. |
| `Failure(NotConfigured, …)` on Android only | Your app removed `androidx.startup.InitializationProvider`. Use `LineLogin.configure(context, config)`. |

## 🤔 Design notes

**Why cinterop against `LineSDKObjC`, and not a Swift bridge.**
LINE's iOS SDK is pure Swift, which Kotlin/Native cannot import — interop goes through Objective-C
only. The usual workaround is to declare a protocol in Kotlin and implement it in Swift inside the
consumer's app, which is both ~90 lines of everybody's boilerplate and impossible to ship
pre-built (Kotlin prefixes exported symbols with *your* framework's name). LINE also publishes
`LineSDKObjC`, an `@objc` facade over the same SDK, and Kotlin/Native binds to that directly. The
cost is that consumers must add that specific SPM product; the benefit is that Kotlin owns the whole
flow — including `LoginProcess.stop()` on cancellation, which the bridge approach cannot do
correctly.

**Why no refresh token.** Both SDKs own token rotation: iOS marks the property `unavailable` and
Android hides it behind an auto-refreshing proxy. Exposing it would create a second, stale copy of
state something else is already managing.

**Why granted scopes are not reported.** iOS cannot report them faithfully — its Objective-C layer
exposes permissions as opaque objects with no readable value and no equality. Read the data instead:
`profile != null`, `idToken != null`, `idToken.email != null`.

**Why `LineLogin` is an object.** `LoginManager` on iOS is a process-wide singleton whose setup is
one-shot and asserts on a second call. An API that let you build several clients would be lying.

## 📱 Sample

[`sample/`](sample) is a Compose Multiplatform app running on both platforms. Put your channel ID in
[`SampleConfig.kt`](sample/composeApp/src/commonMain/kotlin/dev/yjyoon/lineloginkmp/sample/SampleConfig.kt),
then:

```bash
./gradlew :sample:composeApp:installDebug          # Android
open sample/iosApp/iosApp.xcodeproj                # iOS
```

## 🤝 Contributing

Issues and pull requests are welcome — see [CONTRIBUTING.md](CONTRIBUTING.md) for how the project is
laid out, what CI checks, and how a release is cut.

## 📄 License

```
Copyright 2026 yjyoon

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    https://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```

LINE, and the LINE icon bundled in `lineloginkmp-compose`, are trademarks of LY Corporation. The
icon is included under LINE's
[Usage Guidelines for the LINE Login Button](https://terms2.line.me/LINE_Developers_Guidelines_for_Login_Button)
and is not covered by the Apache licence above — see [NOTICE](NOTICE). This is an independent
open-source project and is not affiliated with, endorsed by, or sponsored by LY Corporation.
