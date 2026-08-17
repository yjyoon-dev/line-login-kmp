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
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.multiplatform.library)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.dokka)
    id("lineloginkmp.convention.publish")
}

// A separate artifact on purpose. The core library has no UI dependency at all, and an app using
// Android Views or SwiftUI should not have to take Compose along with LINE Login.

kotlin {
    explicitApi()

    @OptIn(org.jetbrains.kotlin.gradle.dsl.abi.ExperimentalAbiValidation::class)
    abiValidation {
        keepLocallyUnsupportedTargets.set(true)
    }

    androidLibrary {
        namespace = "dev.yjyoon.lineloginkmp.compose"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        withHostTestBuilder {}.configure {}

        compilations.configureEach {
            compileTaskProvider.configure {
                compilerOptions {
                    jvmTarget.set(JvmTarget.JVM_17)
                }
            }
        }
    }

    // No iosX64: Compose Multiplatform does not publish the Intel simulator target. The core
    // library still supports it — only this module cannot.
    listOf(
        iosArm64(),
        iosSimulatorArm64(),
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "LineLoginKMPCompose"
            isStatic = true
        }

        // A test binary is a real executable, so it has to resolve everything the core library's
        // klib references — including LineSDKObjC, which arrives here through the auto-link record
        // baked into that klib's cinterop. Without these it links today only because nothing in
        // these tests reaches LINE's SDK, and it would break the moment one did.
        iosTarget.binaries.getTest("DEBUG").linkerOpts(
            "-F",
            lineSdkObjCSliceFor(iosTarget.name).absolutePath,
            "-framework",
            "LineSDKObjC",
            "-rpath",
            lineSdkObjCSliceFor(iosTarget.name).absolutePath,
        )
    }

    sourceSets {
        commonMain.dependencies {
            // `api`: a consumer holding a LineLoginButton inevitably touches LineLogin itself.
            api(project(":lineloginkmp"))

            // foundation only, deliberately. The button draws itself from LINE's colour table, so
            // Material would add weight without adding anything.
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.ui)

            // For decodeToImageBitmap only — see LineIcon. Compose Multiplatform's *resource
            // packaging* is deliberately not used: it does not reach the AAR this plugin builds.
            implementation(libs.compose.components.resources)
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}

// The xcframework itself is downloaded by :lineloginkmp, which is where the version, the checksum
// and the cinterop live. This only needs to know where that put it.
private fun lineSdkObjCSliceFor(targetName: String): File {
    val slice =
        when (targetName) {
            "iosArm64" -> "ios-arm64"
            "iosSimulatorArm64", "iosX64" -> "ios-arm64_x86_64-simulator"
            else -> error("No LineSDKObjC slice is published for $targetName.")
        }
    val version = libs.versions.linesdk.ios.get()
    return gradle.gradleUserHomeDir
        .resolve("caches/lineloginkmp/linesdk-objc/$version/LineSDKObjC.xcframework/$slice")
}
