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
sample/                       Compose Multiplatform app; also the iOS integration gate
gradle-conventions/           publishing and formatting convention plugins
```

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

## Before you open a pull request

```bash
./gradlew spotlessApply                  # formatting and licence headers
./gradlew :lineloginkmp:allTests         # unit tests, including the iOS link test
./gradlew :lineloginkmp:checkKotlinAbi   # public API is unchanged…
./gradlew :lineloginkmp:updateKotlinAbi  # …or update the dump deliberately, and say so in the PR
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

1. Update `CHANGELOG.md`.
2. Tag `vX.Y.Z` and publish a GitHub release. The `Publish` workflow stages the artifacts to Maven
   Central; promote the deployment by hand at <https://central.sonatype.com/publishing>.

Publishing must happen on macOS — the iOS klibs cannot be produced anywhere else.
