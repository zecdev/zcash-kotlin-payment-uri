# How to release this library

This library is released to Maven Central using [JReleaser](https://jreleaser.org/guide/latest/examples/maven/maven-central.html).
Since the v2/K0 Kotlin Multiplatform conversion, `./gradlew publish` produces **four**
publications — root Gradle-module, `jvm`, `iosArm64`, `iosSimulatorArm64` — under the coordinates
documented in `README.md`'s Install section.

**Note: Currently, the Maven Central Portal API does not support SNAPSHOT releases.**

## Pre-release checklist

Before tagging a release, confirm all of the following on the commit you intend to tag:

1. **All four CI jobs are green on `main`** for that commit — `test-jvm` (both JDK 17 / 21 matrix
   legs: unit tests, the Kover 100%-line/≥98%-branch coverage gate, ktlint, detekt), `test-apple`
   (iOS simulator tests + device-target link), `fuzz-smoke` (bounded Jazzer fuzzing), and `dokka`
   (zero-warning API doc generation, which also runs `scripts/check-readme-snippets.sh`). See
   `.github/workflows/ci.yml`.
2. **`test-vectors` is pinned to a tagged commit** of
   [zecdev/zcash-zip321-test-vectors](https://github.com/zecdev/zcash-zip321-test-vectors) — not a
   branch tip. Check with `git -C test-vectors describe --tags` (or `git submodule status` from
   the repo root) and update the submodule pointer first if it is not pinned to a tag.
3. **`CHANGELOG.md` is finalized**: the `## [<version>] - Unreleased` heading has its date filled
   in (`## [<version>] - YYYY-MM-DD`), and every breaking change, added/changed/removed/fixed item,
   and security note for the release is present under Keep-a-Changelog categories
   (Added/Changed/Removed/Fixed/Security).
4. **`gradle.properties`'s `LIBRARY_VERSION`** matches the tag you are about to create (without the
   `v` prefix) and `IS_SNAPSHOT=false`.
5. **Dokka builds clean**: `./gradlew :lib:dokkaGenerate` produces no warnings (this is also
   enforced by the `dokka` CI job, but re-check locally if you touched KDoc right before tagging).
6. **README's Quick start snippets still match their KDoc source**
   (`scripts/check-readme-snippets.sh`, also enforced by the `dokka` CI job).

## Release using CI

`.github/workflows/deploy-release.yml` runs on any `v*.*.*` tag push: it checks out the tag (with
submodules, on `macos-15` — required so the `iosArm64`/`iosSimulatorArm64` publications actually
get cross-compiled; Kotlin/Native can only cross-compile Apple targets on a macOS host), runs
`./gradlew publish` to stage all four publications, and then `./gradlew :lib:jreleaserRelease` to
deploy them to Maven Central and create the GitHub release, provided the required JReleaser/GPG
secrets are configured for the `deployment` environment.

**Example**

Creating `2.0.0`:

1. Set `LIBRARY_VERSION=2.0.0` in `gradle.properties` and finalize the CHANGELOG date, per the
   checklist above; commit that change.
2. Create and push an annotated tag matching the version:

```sh
git tag --annotate --cleanup=whitespace --edit --message "" v2.0.0
git push origin v2.0.0
```

The `--edit` flag opens your editor for the tag message. For a major version like `2.0.0`,
summarize the breaking-change highlights from `CHANGELOG.md` (the public API reshape, the KMP
conversion and artifact-coordinate change, dependency removal) in the tag message.

## Release manually from a local build

**Note:** we advise against doing manual releases, but sometimes they are necessary.

JReleaser depends on the following environment variables:

```
JRELEASER_MAVENCENTRAL_USERNAME:
JRELEASER_MAVENCENTRAL_PASSWORD:
JRELEASER_GPG_PASSPHRASE:
JRELEASER_GPG_SECRET_KEY:
JRELEASER_GPG_PUBLIC_KEY:
JRELEASER_GITHUB_TOKEN:
```

Set them however you wish, as long as **they never end up checked in to the repository by
mistake**. We advise sourcing them from a file that lives outside this repository, so an
accidental `git add -A` can't pick them up. JReleaser will also look for these values in
`~/.jreleaser/config.toml`, but that probably won't work if you have more than one project using
JReleaser.

Once the environment variables are set, from a macOS host with a full Xcode install (required for
the `iosArm64`/`iosSimulatorArm64` publications):

```sh
# clean the environment
./gradlew clean

# stage all four publications locally
DEVELOPER_DIR=/Applications/Xcode.app/Contents/Developer ./gradlew publish

# release to Maven Central
./gradlew :lib:jreleaserRelease
```

## After releasing

Once `test-vectors` is re-pointed at the published `zecdev/zcash-zip321-test-vectors` repository
(see the note in `.gitmodules`), pin the submodule to the tagged corpus revision that this release
was verified against, rather than tracking a moving branch.
