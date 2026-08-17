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
package lineloginkmp.convention

import com.diffplug.gradle.spotless.SpotlessExtension
import extension.libs
import extension.version

// Formatting for the whole build. Applied once at the root, targeting every module, so a new module
// is covered the moment it is created.
//
// `./gradlew spotlessApply` fixes; `./gradlew spotlessCheck` is what CI runs.

plugins {
    id("com.diffplug.spotless")
}

// Build output is pruned on the file tree itself, not through targetExclude. That distinction is
// load-bearing: spotless resolves target() into a file collection first, so an exclude added
// afterwards filters the result while Gradle has already walked into build directories — and a
// single broken symlink in there (Xcode leaves several under sample/iosApp/build/DerivedData) fails
// the whole task with an error that has nothing to do with formatting. Excluding on the tree makes
// Gradle skip those directories instead of descending into them.
//
// Note for editors: these glob patterns cannot go in a KDoc block. A slash-star-star pattern
// contains the comment terminator, which silently ends the comment and turns the rest into code.
private val prunedDirectories =
    listOf(
        "**/build",
        "**/build/**",
        "**/.gradle",
        "**/.gradle/**",
        "**/.kotlin",
        "**/.kotlin/**",
    )

private val kotlinSources =
    fileTree(rootDir) {
        include("**/src/**/*.kt")
        exclude(prunedDirectories)
    }

private val gradleScripts =
    fileTree(rootDir) {
        include("**/*.gradle.kts")
        exclude(prunedDirectories)
    }

configure<SpotlessExtension> {
    kotlin {
        target(kotlinSources)
        licenseHeader(LICENSE_HEADER)
        ktlint(libs.version("ktlint"))
    }
    format("kts") {
        target(gradleScripts)
        // Keep the header above the `plugins {}` block rather than above an import or a comment.
        licenseHeader(LICENSE_HEADER, "(^(?![\\/ ]\\*).*$)")
    }
}

private val LICENSE_HEADER =
    """
    /*
     * Copyright ${'$'}YEAR yjyoon
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

    """.trimIndent()
