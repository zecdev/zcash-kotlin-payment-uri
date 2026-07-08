import org.jreleaser.model.Active
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

/*
 * ZIP-321 Kotlin Multiplatform library.
 *
 * K0 (v2): converted from a JVM-only `java-library` to `kotlin("multiplatform")`
 * with ZERO runtime dependencies. Production sources live in `commonMain` and
 * compile for every target (jvm + iOS). The existing test suite (kotest +
 * JUnit5 + Jazzer) stays on the JVM in `jvmTest`.
 *
 * NOTE: no `androidTarget()` in this PR. Android consumers use the `jvm`
 * variant (`org.zecdev:zip321-jvm`) meanwhile; a dedicated Android library
 * target is a follow-up.
 */

plugins {
    // Kotlin Multiplatform replaces the previous `org.jetbrains.kotlin.jvm`
    // + `java-library` combination.
    kotlin("multiplatform") version "2.0.20"
    id("org.jlleitschuh.gradle.ktlint") version "12.1.1"
    id("io.gitlab.arturbosch.detekt") version "1.23.7"

    `maven-publish`
    id("org.jreleaser") version "1.22.0"
    signing
}

repositories {
    mavenCentral()
}

val myGroupId = "org.zecdev"
val myVersion = project.property("LIBRARY_VERSION").toString()
group = myGroupId
version = myVersion

kotlin {
    // JVM target keeps the toolchain the JVM-only build used.
    jvm {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_1_8)
        }
    }

    // iOS targets. This PR only requires that they COMPILE with zero
    // runtime dependencies (commonMain is pure Kotlin).
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        val commonMain by getting {
            // ZERO runtime dependencies. No kudzu, no guava, no commons-math3.
        }

        val jvmTest by getting {
            dependencies {
                // kotest (JVM only for now; moving to commonTest is a later PR).
                implementation("io.kotest:kotest-runner-junit5:5.9.0")
                implementation("io.kotest:kotest-property:5.9.0")
                implementation("io.kotest:kotest-assertions-core-jvm:5.9.0")
                implementation("io.kotest:kotest-framework-engine-jvm:5.9.0")

                // Fuzzing (Jazzer + JUnit5 platform).
                implementation(platform("org.junit:junit-bom:5.11.4"))
                implementation("org.junit.jupiter:junit-jupiter")
                runtimeOnly("org.junit.platform:junit-platform-launcher")
                implementation("com.code-intelligence:jazzer-junit:0.24.0")
                implementation("com.code-intelligence:jazzer-api:0.24.0")
                runtimeOnly("com.code-intelligence:jazzer:0.24.0")

                // ZIP-321 conformance corpus (TEST-ONLY): JSON tree parsing of
                // the shared vectors in the `test-vectors` git submodule. Only
                // the JsonElement tree API is used (no @Serializable classes),
                // so the serialization compiler plugin is intentionally NOT
                // applied and production code gains no new dependency.
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
            }
        }
    }
}

// Expose the shared ZIP-321 conformance corpus (git submodule at the repo
// root) to the JVM test runtime as classpath resources: valid/*.json and
// invalid/*.json.
kotlin.sourceSets.getByName("jvmTest").resources.srcDir(
    rootProject.projectDir.resolve("test-vectors/vectors")
)

dependencies {
    detektPlugins("io.gitlab.arturbosch.detekt:detekt-formatting:1.23.3")
}

detekt {
    buildUponDefaultConfig = true
    allRules = false
    config.setFrom("$rootDir/tools/detekt.yml")
    autoCorrect = true
    // KMP does not auto-wire detekt's default source set; point it at the
    // production Kotlin source directories explicitly.
    source.setFrom(
        "src/commonMain/kotlin",
        "src/jvmMain/kotlin",
        "src/iosMain/kotlin"
    )
}

tasks.withType<io.gitlab.arturbosch.detekt.Detekt>().configureEach {
    exclude("**/test/**", "**/*Test*")
    reports {
        html.required.set(true)
        xml.required.set(true)
        txt.required.set(true)
        sarif.required.set(true)
        md.required.set(true)
    }
}

// All JVM test tasks (unit + fuzz regression) run on the JUnit 5 platform.
tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
}

// ---------------------------------------------------------------------------
// Publication
//
// BREAKING (v2): the KMP plugin publishes one Gradle-module publication plus
// one per target instead of the single JVM jar. Coordinates become:
//   org.zecdev:zip321                    (Gradle metadata; root module)
//   org.zecdev:zip321-jvm                (JVM artifact; Android consumes this)
//   org.zecdev:zip321-iosarm64
//   org.zecdev:zip321-iossimulatorarm64
// The base artifactId is remapped from the Gradle project name ("lib") to
// "zip321" to preserve the historical coordinate for the root/JVM modules.
// ---------------------------------------------------------------------------

// KMP publications need a javadoc jar for Maven Central validation; provide an
// (empty) stub so the publication model is complete and compiles.
val javadocJar by tasks.registering(Jar::class) {
    archiveClassifier.set("javadoc")
}

val myArtifactId = "zip321"
val isSnapshot = project.property("IS_SNAPSHOT").toString().toBoolean()
val myDescription = "A concise implementation of ZIP-321 in Kotlin."
val myRepoUrl = "https://github.com/zecdev/zcash-kotlin-payment-uri"

publishing {
    publications.withType<MavenPublication>().configureEach {
        // Remap "lib" / "lib-<target>" -> "zip321" / "zip321-<target>".
        artifactId = artifactId.replace("lib", myArtifactId)
        artifact(javadocJar)

        pom {
            name.set("Zcash Kotlin Payment URI")
            description.set(myDescription)
            url.set(myRepoUrl)
            inceptionYear.set("2023")
            scm {
                url.set(myRepoUrl)
                connection.set("scm:git:git://github.com/zecdev/zcash-kotlin-payment-uri.git")
                developerConnection.set("scm:git:ssh://git@github.com/zecdev/zcash-kotlin-payment-uri.git")
            }
            developers {
                developer {
                    id.set("ZecDev")
                    name.set("ZecDev")
                    url.set("https://github.com/zecdev")
                }
            }
            licenses {
                license {
                    name.set("The MIT License")
                    url.set("https://opensource.org/licenses/MIT")
                    distribution.set("repo")
                }
            }
        }
    }
    repositories {
        maven {
            url = layout.buildDirectory.dir("staging-deploy").get().asFile.toURI()
        }
    }
}

jreleaser {
    gitRootSearch.set(true)
    version = myVersion

    signing {
        active.set(Active.ALWAYS)
        armored.set(true)
    }

    deploy {
        active.set(Active.ALWAYS)
        maven {
            active.set(Active.ALWAYS)
            pomchecker {
                version.set("1.12.0")
                failOnWarning.set(false)
                failOnError.set(true)
            }
            project {
                description.set(myDescription)
                copyright.set("Copyright ZecDev.Org.")
                license.set("The MIT License.")
            }
            mavenCentral {
                active.set(Active.ALWAYS)
                create("maven-central") {
                    active.set(Active.ALWAYS)
                    url.set("https://central.sonatype.com/api/v1/publisher")
                    stagingRepository("build/staging-deploy")

                    // FIX: Jreleaser fails after attempt 61 but library is published anyway
                    maxRetries.set(180)
                    retryDelay.set(20)
                }
            }
        }
    }
}
