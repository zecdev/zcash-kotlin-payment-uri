# How to release this library
This Library is released to Maven Central using [Jreleaser](https://jreleaser.org/guide/latest/examples/maven/maven-central.html)

**Note: Currently, the Maven Central Portal API does not support SNAPSHOT releases.**

## Pre-requisites
- set the release version on `gradle.properties` 
- update the CHANGELOG.md file with latest changes that are to be released with this version

## Release using CI
- create a tag matching the version present on `gradle.properties`
- push the tag to origin.

## Release Manually from a local build
**Note:** we advise against doing manual releases. But sometimes they are necessary so,
we provide instructions on how to do so.

* We use SDKMAN as Java Environment manager. But is not mandatory for you to do so. 

JReleaser depends on the following environment variables:
```
JRELEASER_MAVENCENTRAL_USERNAME:
JRELEASER_MAVENCENTRAL_PASSWORD: 
JRELEASER_GPG_PASSPHRASE:
JRELEASER_GPG_SECRET_KEY: 
JRELEASER_GPG_PUBLIC_KEY: 
JRELEASER_GITHUB_TOKEN: 
```

you can set them us as you wish as long as **they never end up checked in the repository
by mistake**. We advise that you use files that set those environment variables that are
out of the scope of the local repository. This will help you not `git add -A` them.

JReleaser will look for these values on `~/.jreleaser/config.toml` but this probably won't 
work if you have more than one project using JReleaser.

Once you've set up the environment variables.

```
# clean the environment
./gradlew clean\

# publish to local
./gradlew publish

# release to Maven Central
./gradlew :lib:jreleaserRelease
```
