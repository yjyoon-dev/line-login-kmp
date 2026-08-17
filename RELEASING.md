# Releasing

Maintainer notes. Contributors do not need any of this — see [CONTRIBUTING.md](CONTRIBUTING.md).

## Cutting a release

1. Update `CHANGELOG.md`.
2. Publish a GitHub Release tagged `vX.Y.Z`.

That is the whole release. The `Publish` workflow re-runs the checks, signs, uploads, and releases
to Maven Central once Central has validated the deployment. A rejected deployment fails the workflow
and nothing is published.

The version comes from the tag, so `VERSION_NAME` in `gradle.properties` is only a default for local
builds.

Publishing runs on macOS — the iOS klibs cannot be produced anywhere else.

## Repository secrets

| Secret | What it is |
|---|---|
| `MAVEN_CENTRAL_USERNAME` / `MAVEN_CENTRAL_PASSWORD` | A Central Portal **user token**, from the account that owns the `dev.yjyoon` namespace. Not the account's login. Generate it at [central.sonatype.com](https://central.sonatype.com) → View Account → Generate User Token. |
| `SIGNING_KEY` | `gpg --export-secret-keys --armor <key id>`. The armor header and footer are optional — the plugin adds them if they are missing. |
| `SIGNING_KEY_ID` | The last 8 characters of that key's id |
| `SIGNING_KEY_PASSWORD` | That key's passphrase |

The signing key's **public** half must be on a keyserver, or Central rejects the signatures:

```bash
gpg --keyserver keyserver.ubuntu.com --send-keys <fingerprint>
curl -s "https://keyserver.ubuntu.com/pks/lookup?op=index&options=mr&search=0x<fingerprint>"
```

## Trying the signing path before you tag

`publishToMavenLocal` demands a signature for any version that does not end in `-SNAPSHOT`, which
makes it a full rehearsal of what the workflow does — without needing Central credentials:

```bash
printf 'passphrase: '; read -rs PP; echo
ORG_GRADLE_PROJECT_signingInMemoryKey="$(gpg --batch --pinentry-mode loopback --passphrase "$PP" --export-secret-keys --armor <key id>)" \
ORG_GRADLE_PROJECT_signingInMemoryKeyId='<last 8 chars>' \
ORG_GRADLE_PROJECT_signingInMemoryKeyPassword="$PP" \
./gradlew publishToMavenLocal -PVERSION_NAME=1.0.0-verify
unset PP
```

Then check that the signatures exist and came from the key you expected:

```bash
gpg --list-packets ~/.m2/repository/dev/yjyoon/lineloginkmp/lineloginkmp/1.0.0-verify/*.pom.asc | grep keyid
rm -rf ~/.m2/repository/dev/yjyoon/lineloginkmp/*/1.0.0-verify
```

`--pinentry-mode loopback` is not optional in a non-interactive shell: without it, and without
`GPG_TTY` set, the export fails with `Inappropriate ioctl for device` and writes nothing at all —
which looks like success if you piped the output somewhere.

## Publishing by hand

If the workflow is unavailable, the same release can be cut locally on macOS:

```bash
ORG_GRADLE_PROJECT_signingInMemoryKey=… \
ORG_GRADLE_PROJECT_signingInMemoryKeyId=… \
ORG_GRADLE_PROJECT_signingInMemoryKeyPassword=… \
ORG_GRADLE_PROJECT_mavenCentralUsername=… \
ORG_GRADLE_PROJECT_mavenCentralPassword=… \
./gradlew publishAndReleaseToMavenCentral -PVERSION_NAME=X.Y.Z
```

Swap in `publishToMavenCentral` to stage the deployment instead and promote it by hand at
<https://central.sonatype.com/publishing>.
