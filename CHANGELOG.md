# Changelog

All notable changes to this project are documented here. The format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and versions follow
[Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added

- **Web support** (`wasmJs`, browser): both artifacts now publish a Kotlin/Wasm target. The browser
  half runs on LIFF, LINE's official JavaScript SDK, loaded from LINE's CDN at configure time —
  the plain OAuth web flow is off the table because LINE's token endpoint requires the channel
  secret, which must never ship in a browser. `LineLoginConfig` gains `liffId` to name the LIFF
  app; Android and iOS ignore it.
- Web semantics, documented on the KDoc and in the README: `login()` is a full-page redirect that
  does not resume, `logout()` clears the browser only, and `currentAccessToken()` confirms the
  expiry with LINE because LIFF does not store one.

## [1.0.1] - 2026-08-17

### Added

- `LineLogin.isLineAppInstalled()` in shared code. Android already had an Android-only overload
  taking a `Context`; iOS had nothing, so the answer was unavailable from common code. The iOS half
  asks the same question LINE's own SDK asks internally — whether anything can open the `lineauth2`
  scheme — which means it needs `LSApplicationQueriesSchemes` in your `Info.plist`, the entry
  app-to-app login already requires.

### Fixed

- `LineLoginButton` kept its icon in a square at the leading edge when the button is wider than its
  content. It centred the whole row instead, so a stretched button — `Modifier.fillMaxWidth()`, the
  common case on a sign-in screen — opened a gap of bare background to the left of the icon.
- An icon-only `LineLoginButton` (`text = null`) now carries LINE's own wording for the reader's
  language as its accessibility label. It previously had none at all, so a screen reader announced
  only "button".
- `LineLogin.configure` is atomic. Validating, setting the platform SDK up and recording the result
  were three separate steps, so two threads could interleave them — running the iOS SDK's one-shot
  setup twice, or letting two different channel IDs both pass the "already configured" check.

### Changed

- `LineLoginButtonText.current()` remembers its parse against the language tag instead of
  re-resolving the locale's caption on every recomposition. It is the login button's default
  caption, so it ran on every frame that recomposed one.

## [1.0.0] - 2026-08-17

First release.

### Added

- `LineLogin` — `configure`, `login`, `logout`, `currentAccessToken`, `isLoggedIn` — from shared
  Kotlin on Android and iOS.
- A result model where cancelling is its own case, and failures carry both a cross-platform
  `LineLoginErrorCode` and the native SDK's own code and message.
- Per-login options: scopes (including any scope LINE adds later), OpenID nonce, forced web login,
  and the LINE Official Account friend prompt.
- Android: the proxy Activity, `INTERNET` permission and `androidx.startup` bootstrap are merged
  into consumer manifests, so integrating apps declare nothing. Plus `isLineAppInstalled(context)`
  and a `configure(context, config)` escape hatch.
- iOS: driven directly from Kotlin through cinterop against LINE's `LineSDKObjC` layer — no Swift
  bridge. `LineLoginUrlHandler.handle(url:)` is the single Swift touchpoint.
- Cancelling the calling coroutine abandons the login: iOS dismisses the LINE screen, Android
  closes the library's own Activity.
- `dev.yjyoon.lineloginkmp:lineloginkmp-compose`, an optional second artifact with `LineLoginButton` — a Compose
  Multiplatform button built to LINE's
  [design guidelines](https://developers.line.biz/en/docs/line-login/login-button/), including the
  state overlays, the disabled treatment, icon-derived padding, and LINE's recommended captions in
  18 languages. LINE's own icon is bundled — unmodified, from LINE's official template — so the
  button needs no asset from the consumer; see NOTICE for the trademark terms that come with it.

[Unreleased]: https://github.com/yjyoon-dev/line-login-kmp/compare/v1.0.0...HEAD
[1.0.0]: https://github.com/yjyoon-dev/line-login-kmp/releases/tag/v1.0.0
