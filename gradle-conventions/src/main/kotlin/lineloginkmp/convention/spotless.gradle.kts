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

/**
 * Formatting for the whole build. Applied once at the root, targeting every module, so a new
 * module is covered the moment it is created.
 *
 * `./gradlew spotlessApply` fixes; `./gradlew spotlessCheck` is what CI runs.
 */
plugins {
    id("com.diffplug.spotless")
}

configure<SpotlessExtension> {
    kotlin {
        target("**/src/**/*.kt")
        targetExclude("**/build/**")
        licenseHeader(LICENSE_HEADER)
        ktlint(libs.version("ktlint"))
    }
    format("kts") {
        target("**/*.gradle.kts")
        targetExclude("**/build/**")
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
