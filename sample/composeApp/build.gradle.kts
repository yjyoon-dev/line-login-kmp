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
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
}

kotlin {
    androidTarget {
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
    }

    @OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)
    wasmJs {
        browser {
            commonWebpackConfig {
                outputFileName = "sample.js"
            }
        }
        binaries.executable()
    }

    // No iosX64: Compose Multiplatform stopped publishing the Intel simulator target. The library
    // itself still supports it — only this sample cannot.
    listOf(
        iosArm64(),
        iosSimulatorArm64(),
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"

            // Required, and NOT the default — Kotlin/Native frameworks are dynamic unless you say
            // otherwise. A dynamic framework fails to link: LineSDKObjC's symbols would have to be
            // resolved while building the framework itself, before Xcode has fetched the package.
            isStatic = true

            // Without this, LineLoginUrlHandler is compiled but never exported, and the one line
            // of Swift in iosApp fails with "cannot find LineLoginUrlHandler in scope".
            export(project(":lineloginkmp"))
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            // Accessor on purpose: material3 has its own version line, which this resolves.
            implementation(compose.material3)

            // `api`, not `implementation`: required for export(...) above to have anything to
            // export. Gradle fails the build with an explicit message otherwise.
            api(project(":lineloginkmp"))
            implementation(project(":lineloginkmp-compose"))
        }
        androidMain.dependencies {
            implementation(libs.compose.ui.tooling)
            implementation(libs.androidx.activity.compose)
        }
    }
}

android {
    namespace = "dev.yjyoon.lineloginkmp.sample"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "dev.yjyoon.lineloginkmp.sample"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
