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

import com.vanniktech.maven.publish.MavenPublishBaseExtension

/**
 * Publishing to Maven Central.
 *
 * Everything project-specific — coordinates, POM name, licence, developer, SCM — is read from the
 * `GROUP` / `VERSION_NAME` / `POM_*` keys in the root `gradle.properties` by the plugin itself, so
 * releasing a new version never means editing Kotlin. See `CONTRIBUTING.md` for the credentials
 * the `publish` task expects.
 */
plugins {
    id("com.vanniktech.maven.publish")
}

configure<MavenPublishBaseExtension> {
    // Staged, not auto-released: the Central Portal deployment is promoted by hand after the
    // release workflow has verified the artifacts.
    publishToMavenCentral()

    // Signing keys arrive as ORG_GRADLE_PROJECT_signingInMemoryKey* environment variables in CI.
    // Locally, an unsigned `publishToMavenLocal` still works because the plugin skips signing when
    // no key is present.
    signAllPublications()
}
