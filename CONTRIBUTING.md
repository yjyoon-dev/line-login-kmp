# Contributing

Thanks for taking a look. Issues and pull requests are both welcome — including "the README lied to
me", which is a real bug.

## Getting set up

You need **macOS**, **JDK 17**, **Xcode**, and the Android SDK. macOS is not a preference: the iOS
targets bind LINE's Objective-C SDK through cinterop, and that only runs here.

```bash
git clone https://github.com/yjyoon-dev/line-login-kmp.git
cd line-login-kmp
./gradlew :lineloginkmp:assemble :lineloginkmp:allTests
```

The first build downloads `LineSDKObjC.xcframework` (12 MB) into your Gradle home and checks its
SHA-256. It is a compile-time input only, and it is not committed.

To run the sample, put a real channel ID in
`sample/composeApp/src/commonMain/kotlin/dev/yjyoon/lineloginkmp/sample/SampleConfig.kt` first.

## How the project is laid out

```
lineloginkmp/                 the published library
├── interop/LineSDKObjC.def   cinterop definition for LINE's Objective-C layer
├── consumer-rules.pro        R8 rules shipped to consumers
└── src/
    ├── commonMain/           the entire public API, plus the error mappers
    │   └── internal/         the expect seam and the mapping tables
    ├── androidMain/          proxy Activity, request bus, androidx.startup bootstrap
    ├── iosMain/              cinterop calls into LineSDKObjC
    ├── commonTest/           value types and both error tables
    ├── androidHostTest/      mapping against real LINE SDK objects, and the request bus
    └── iosTest/              the cinterop link smoke test
lineloginkmp-compose/         optional second artifact: the Compose login button
└── src/commonMain/           LineLoginButton, LINE's palette, geometry, captions, bundled icon
sample/                       Compose Multiplatform app; also the iOS integration gate
gradle-conventions/           publishing and formatting convention plugins
```

Each published module carries its own `gradle.properties` with `POM_ARTIFACT_ID` / `POM_NAME` /
`POM_DESCRIPTION`; everything else about the POM lives in the root one.

Two rules keep the two platforms honest:

1. **Policy lives in `commonMain`.** Validation, the not-configured check, serialising concurrent
   logins — all of it is in `LineLogin`, so it cannot drift between platforms. The `platform*`
   functions do only what genuinely differs.
2. **Error mapping is pure.** `mapAndroidError` and `mapIosError` take primitives and return an
   enum. That is what makes the riskiest logic in the library testable without a device — keep it
   that way when adding codes.

## Things that look wrong but are not

Before "cleaning up" any of these, read the comment above them:

- `LineLoginProxyActivity` extends the **platform** `Activity`, not AndroidX's `ComponentActivity`.
  That keeps `androidx.activity` out of every consumer's dependency graph.
- Its manifest entry deliberately omits `noHistory`, `taskAffinity`, `singleTask`/`singleInstance`
  and `screenOrientation`. Each one breaks the login in a different way.
- `RESULT_CANCELED` with a null `Intent` is special-cased before the SDK's parser sees it.
- Every iOS accessor is guarded by `isSetupFinished()`. Calling `isAuthorized` before setup does not
  return `false` — it kills the process with a Swift `fatalError`.
- The class name `dev.yjyoon.lineloginkmp.internal.LineLoginProxyActivity` is public API: it appears
  in every consumer's merged manifest. Renaming it is a breaking change.
- Kotlin **file names** in `iosMain` are part of the Swift-facing API surface for anything exported
  as a file facade. `LineLoginUrlHandler` is an `object` precisely so it does not depend on one.
- `LineLoginButtonColors` has no way to override its values, and that is the point: LINE's
  [button guidelines](https://developers.line.biz/en/docs/line-login/login-button/) permit exactly
  those colours. Anything that makes a non-compliant button easy to build is a regression.
- The LINE icon is a Base64 PNG in `LineIcon.kt` rather than a Compose resource. That is not
  laziness: Compose Multiplatform's resource packaging does not reach the AAR that
  `com.android.kotlin.multiplatform.library` produces, so the resource exists on iOS and is silently
  missing on Android, and a consumer app dies at first composition with `MissingResourceException`.
  Verified, not assumed.
- That icon is LY Corporation's trademark, unmodified, and outside this project's Apache licence.
  Never redraw, recolour or crop it — the guidelines list a modified mark as a mistake — and keep
  `NOTICE` in step with anything that changes about it.
- `LineLoginButtonDefaults`' ratios were measured from LINE's own 20/32/44 dp button images, not
  derived from the prose. `LineLoginButtonDefaultsTest` pins them; a rendered button was checked
  against the artwork to within a fraction of a dp.

## Commit messages

[Conventional Commits](https://www.conventionalcommits.org/en/v1.0.0/), one line, imperative mood,
no trailing period:

```
feat(compose): add login button built to LINE's design guidelines
fix(android): complete the login when the proxy Activity is destroyed
docs: explain why the iOS half needs no Swift bridge
```

Types: `feat`, `fix`, `docs`, `test`, `refactor`, `perf`, `build`, `ci`, `chore`.
Scopes, when one helps: `core`, `compose`, `android`, `ios`, `sample`.

Keep the subject under ~72 characters and let the pull request carry the reasoning — that is what
gets read during review and what ends up in the changelog. A `!` after the type, as in `feat!:`,
marks a change that breaks the public API; those need an `api/*.api` update in the same commit.

## Before you open a pull request

```bash
./gradlew spotlessApply     # formatting and licence headers
./gradlew allTests          # unit tests, including the iOS cinterop link test
./gradlew checkKotlinAbi    # public API is unchanged…
./gradlew updateKotlinAbi   # …or update the dumps deliberately, and say so in the PR
```

If you touched anything under `iosMain`, build the sample too — it is the only thing that proves a
real app can still link:

```bash
cd sample/iosApp
xcodebuild -project iosApp.xcodeproj -scheme iosApp -sdk iphonesimulator \
  -destination 'generic/platform=iOS Simulator' CODE_SIGNING_ALLOWED=NO build
```

CI runs all of the above on every pull request.

## What cannot be tested automatically

A real login needs a real channel and a real account, so these stay a manual checklist for releases:

- Login with the LINE app installed, and without it, on both platforms.
- Cancelling from the LINE app, and from the browser sheet.
- On iOS with LINE **not** installed: the browser finishes and control returns to the app. This is
  what `.onOpenURL` exists for, and the failure mode is silent.
- On Android: rotate the device mid-login, and turn on "Don't keep activities".
- A deliberately wrong signing certificate, to confirm it surfaces as `Authentication`.

## Upgrading the LINE SDKs

- **Android**: bump `linesdk-android` in `gradle/libs.versions.toml`.
- **iOS**: bump `linesdk-ios`, then update `lineSdkObjCSha256` in `lineloginkmp/build.gradle.kts` to
  the new release asset's checksum. The build fails loudly if you forget — that is the point.
  Re-run the iOS link test and check whether `mapIosError` needs new codes.

## Releasing

Not something a contribution needs to worry about — releases are cut by the maintainer, and the
procedure lives in [RELEASING.md](RELEASING.md).
