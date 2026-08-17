# Changelog

All notable changes to this project are documented here. The format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and versions follow
[Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [0.1.0]

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
  18 languages. It ships no LINE artwork: the icon is a parameter, because downloading LINE's
  template is how a developer accepts the usage guidelines attached to it.

[Unreleased]: https://github.com/yjyoon-dev/line-login-kmp/compare/v0.1.0...HEAD
[0.1.0]: https://github.com/yjyoon-dev/line-login-kmp/releases/tag/v0.1.0
