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
import java.net.URI
import java.security.MessageDigest
import javax.inject.Inject
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.tasks.CInteropProcess

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.multiplatform.library)
    alias(libs.plugins.dokka)
    id("lineloginkmp.convention.publish")
}

// ─────────────────────────────────────────────────────────────────────────────────────────────
// LineSDKObjC.xcframework — the iOS cinterop input
//
// The LINE iOS SDK's main module is pure Swift, which Kotlin/Native cannot import. LINE also
// ships `LineSDKObjC`: the same SDK behind an Objective-C facade, which Kotlin/Native *can*
// import. Binding against it is what lets this library drive LINE Login from Kotlin with no Swift
// bridge in the consumer's app.
//
// It is a compile-time input only, and is downloaded rather than vendored — it is 12 MB of signed
// binary that has no business in git. The published klibs record the module name and a header
// hash, never a path; LineSDKObjC's symbols stay undefined in our static framework and are
// resolved when the consumer's app links its own copy (added through Swift Package Manager).
// ─────────────────────────────────────────────────────────────────────────────────────────────

private val lineSdkObjCVersion = libs.versions.linesdk.ios.get()

/**
 * SHA-256 of `LineSDKObjC-<version>.zip` as published on GitHub Releases. Bumping the version
 * without bumping this hash fails the build, which is the point: this is the one dependency that
 * does not arrive through a checksum-verified artifact repository.
 */
private val lineSdkObjCSha256 = "d4c7c8fb8e81da15c79b0e35df02f672d2b28aa77c0e3bd419d361832ac18125"

/** Kept in the Gradle user home, not in `build/`, so `clean` does not cost a 12 MB download. */
private val lineSdkObjCHome =
    gradle.gradleUserHomeDir.resolve("caches/lineloginkmp/linesdk-objc/$lineSdkObjCVersion")

@CacheableTask
abstract class DownloadLineSdkObjC
    @Inject
    constructor(
        private val archives: ArchiveOperations,
        private val fileSystem: FileSystemOperations,
    ) : DefaultTask() {
        @get:Input
        abstract val url: Property<String>

        @get:Input
        abstract val sha256: Property<String>

        @get:OutputDirectory
        abstract val destination: DirectoryProperty

        @TaskAction
        fun download() {
            val target = destination.get().asFile
            val archive = File(target.parentFile, "${target.name}.zip")
            target.parentFile.mkdirs()

            if (!archive.isFile || archive.sha256() != sha256.get()) {
                logger.lifecycle("Downloading ${url.get()}")
                // Timeouts matter here: without them a stalled connection hangs the build with no
                // output and no way for Gradle to interrupt it.
                val connection = URI(url.get()).toURL().openConnection()
                connection.connectTimeout = CONNECT_TIMEOUT_MILLISECONDS
                connection.readTimeout = READ_TIMEOUT_MILLISECONDS
                archive.outputStream().use { output ->
                    connection.getInputStream().use { it.copyTo(output) }
                }
            }

            val actual = archive.sha256()
            check(actual == sha256.get()) {
                """
                |Checksum mismatch for ${url.get()}
                |  expected: ${sha256.get()}
                |  actual:   $actual
                |Delete $archive and retry. If it keeps failing, the release asset itself changed
                |and the hash in lineloginkmp/build.gradle.kts must be updated deliberately.
                """.trimMargin()
            }

            fileSystem.delete { delete(target) }
            fileSystem.copy {
                from(archives.zipTree(archive))
                into(target)
            }
        }

        private companion object {
            const val CONNECT_TIMEOUT_MILLISECONDS = 30_000
            const val READ_TIMEOUT_MILLISECONDS = 60_000
        }

        private fun File.sha256(): String =
            inputStream().use { stream ->
                val digest = MessageDigest.getInstance("SHA-256")
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                while (true) {
                    val read = stream.read(buffer)
                    if (read <= 0) break
                    digest.update(buffer, 0, read)
                }
                digest.digest().joinToString("") { "%02x".format(it) }
            }
    }

val downloadLineSdkObjC =
    tasks.register<DownloadLineSdkObjC>("downloadLineSdkObjC") {
        group = "build setup"
        description = "Downloads and verifies LineSDKObjC.xcframework $lineSdkObjCVersion, the iOS cinterop input."
        url =
            "https://github.com/line/line-sdk-ios-swift/releases/download/" +
            "$lineSdkObjCVersion/LineSDKObjC-$lineSdkObjCVersion.zip"
        sha256 = lineSdkObjCSha256
        destination = layout.dir(provider { lineSdkObjCHome })
    }

/**
 * The xcframework slice carrying [target]'s architecture. `ios-arm64` is device-only; the
 * simulator slice is one fat binary shared by both simulator targets.
 */
private fun lineSdkObjCSliceFor(target: KotlinNativeTarget): File {
    val slice =
        when (target.name) {
            "iosArm64" -> "ios-arm64"
            "iosSimulatorArm64", "iosX64" -> "ios-arm64_x86_64-simulator"
            else -> error("No LineSDKObjC slice is published for ${target.name}.")
        }
    return lineSdkObjCHome.resolve("LineSDKObjC.xcframework/$slice")
}

kotlin {
    // Every declaration is either explicitly public and documented, or internal. Anything that
    // reaches consumers is a deliberate decision.
    explicitApi()

    @OptIn(org.jetbrains.kotlin.gradle.dsl.abi.ExperimentalAbiValidation::class)
    abiValidation {
        // Contributors on Linux/Windows keep the Apple entries from the committed dump instead of
        // having the check fail over targets their machine cannot build.
        keepLocallyUnsupportedTargets.set(true)
    }

    androidLibrary {
        namespace = "dev.yjyoon.lineloginkmp"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        withHostTestBuilder {}.configure {}

        // Shipped inside the AAR and applied to every consumer's R8 run. `publish` is not on by
        // default: without it the rules are honoured only in this project's own tests, and the
        // published AAR contains no proguard.txt at all.
        optimization {
            consumerKeepRules.files.add(project.file("consumer-rules.pro"))
            consumerKeepRules.publish = true
        }

        compilations.configureEach {
            compileTaskProvider.configure {
                compilerOptions {
                    jvmTarget.set(JvmTarget.JVM_17)
                }
            }
        }
    }

    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64(),
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "LineLoginKMP"
            isStatic = true
        }

        val slice = lineSdkObjCSliceFor(iosTarget)

        iosTarget.compilations.getByName("main").cinterops.create("LineSDKObjC") {
            definitionFile = layout.projectDirectory.file("interop/LineSDKObjC.def")
            // -F is passed here rather than written into the .def so that no machine-specific
            // path is ever recorded in a file under version control.
            //
            // -fmodules is mandatory: LineSDKObjC's umbrella header is Swift-generated and pulls
            // its dependencies in as clang modules. Without it cinterop fails outright with
            // "It seems that library is using clang modules".
            compilerOpts("-F", slice.absolutePath, "-fmodules")
        }

        // A test binary is a real executable, so — unlike the static framework, which leaves them
        // for the consumer's app to resolve — it has to link LineSDKObjC's symbols itself, and
        // find the dynamic framework again at run time (hence -rpath). Absolute paths are fine
        // here: nothing about the test binary is published.
        iosTarget.binaries.getTest("DEBUG").linkerOpts(
            "-F",
            slice.absolutePath,
            "-framework",
            "LineSDKObjC",
            "-rpath",
            slice.absolutePath,
        )
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.coroutines.core)
        }

        androidMain.dependencies {
            // `implementation`, not `api`: no LINE type appears in this library's public API, so
            // consumers get the SDK on their runtime classpath without it becoming part of ours.
            implementation(libs.line.sdk)
            implementation(libs.androidx.startup.runtime)
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}

tasks.withType<CInteropProcess>().configureEach {
    dependsOn(downloadLineSdkObjC)
}
