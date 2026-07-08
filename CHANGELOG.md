# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## Unreleased
### Added (v2/K3)
- **Internal strict unpadded base64url codec (RFC 4648 §5)**
  (`lib/src/commonMain/kotlin/org/zecdev/zip321/parser/Base64URL.kt`):
  `encode(ByteArray): String` and `decode(String): ByteArray?`, pure Kotlin
  common code. Encoding uses the RFC 4648 §5 url-safe alphabet with NO
  padding, exactly like the reference implementation's
  `BASE64_URL_SAFE_NO_PAD`. Decoding strictly rejects: `+`, `/`, `=`
  (padding included), whitespace, any character outside the base64url
  alphabet (including non-ASCII), impossible lengths (`length % 4 == 1`),
  and non-canonical encodings whose trailing bits are nonzero (e.g. `"QR"`).
  The empty string round-trips to zero bytes.

### Changed (v2/K3)
- **`MemoBytes` now encodes/decodes through the strict `Base64URL` codec**,
  replacing the K0 hand-rolled translate-and-pad decoder (which mapped
  `-`/`_` to `+`/`/`, right-padded with `=`, then decoded classic base64).
  Decoding is stricter than before: classic-alphabet `+`/`/`, `=` padding,
  and non-canonical encodings with nonzero trailing bits are now rejected
  (the old path silently accepted them). No conformance vector and no unit
  test exercised the lenient forms — the expected-failure map is unchanged.
  The lenient `String.decodeBase64URL()` public extension is removed along
  with the old implementation.

### Added (v2/K2)
- **New public `NonNegativeAmount` value type**
  (`lib/src/commonMain/kotlin/org/zecdev/zip321/model/NonNegativeAmount.kt`):
  a `Comparable` `@JvmInline value class` wrapping an **unsigned** `ULong`
  count of zatoshi with `NonNegativeAmount.MAX_MONEY`
  (`2_100_000_000_000_000u`) as the upper bound. `Result`-based factories
  `NonNegativeAmount.zatoshi(ULong)` (raw zatoshi) and
  `NonNegativeAmount.zec(String)` (decimal ZEC string) enforce the **strict**
  ZIP-321 `amountparam` grammar (`1*DIGIT [ "." 1*8DIGIT ]`): leading zeros in
  the whole part are accepted, while `"123."`, `".5"`, empty strings, signs,
  whitespace, and scientific notation are rejected, using checked integer
  arithmetic only (failures carry the `NonNegativeAmount.AmountException`
  sealed hierarchy: `NegativeAmount`, `ExceededSupply`,
  `TooManyFractionalDigits`, `InvalidDecimalString`). `decimalString()`
  renders exactly like the reference `amount_str` (librustzcash `zip321`:
  whole part always, fraction only when nonzero, trailing zeros trimmed) — it
  does NOT inherit the v1 8-significant-digit rounding bug. The type is
  amount-agnostic: zero is representable; zero-amount policy (e.g.
  zero-valued transparent outputs) belongs to `Payment`-level validation.
- **Non-negativity is now structural.** The backing type is `ULong` (not a
  checked `Long`), mirroring the `u64`-backed `Zatoshis` of the librustzcash
  `zip321` reference and the `UInt64`-backed `NonNegativeAmount` of
  `zcash-swift-payment-uri`: a negative amount is unrepresentable rather than
  rejected at runtime. `AmountException.NegativeAmount` is therefore
  unreachable from either factory — `zatoshi()` takes an unsigned count, and
  `zec()` reports a leading sign as `InvalidDecimalString` — and is retained
  only so the error taxonomy stays identical to the Swift library's
  `AmountError`.
- **Java interop, accepted v2 break:** because the public API exposes `ULong`,
  Kotlin mangles the names of the `value` accessor and of every function with
  an unsigned parameter or return type in the JVM artifact (`zip321-jvm`), so
  they are not callable from plain Java under their Kotlin names. Kotlin
  consumers (including Android) are unaffected: they see
  `NonNegativeAmount.zatoshi(ULong)`, `value: ULong`, and `MAX_MONEY: ULong`
  normally. This is a deliberate v2 API break, taken so the Kotlin, Swift, and
  Rust representations of a ZIP-321 amount agree exactly; Java callers that
  need a raw zatoshi count should parse via `zec(String)` / render via
  `decimalString()`, or add a thin Kotlin shim.

### Changed (v2/K2)
- **The v1 `NonNegativeAmount` class is renamed `LegacyAmount`** and is
  `@Deprecated("Use NonNegativeAmount")`, freeing its name for the new value
  type (`zcash-swift-payment-uri` renamed the equivalent struct the same way).
  There is deliberately **no** typealias for the old name — it now denotes the
  replacement type — so any reference to `NonNegativeAmount` in v1 code is a
  hard break, which is acceptable while v2 is unreleased. The legacy behavior
  itself is unchanged (including its lenient `"123."`/`".5"` parsing and the
  known 8-significant-digit render rounding bug, both preserved until the v2
  parser rewrite); its JVM `BigDecimal` interop moved to
  `lib/src/jvmMain/kotlin/org/zecdev/zip321/model/LegacyAmountBigDecimal.kt`.
  Internal use sites (parser, renderer, model, tests) suppress the deprecation
  warning file-wide with `@file:Suppress("DEPRECATION")` until the parser
  adopts the new type, keeping the build warning-free.

### Changed — test suite on all targets (v2/K1)
- The test suite moved from jvm-only **kotest** to **`kotlin.test`** in
  `commonTest`, so the same tests now compile and run on every KMP target
  (`jvmTest`, `iosSimulatorArm64Test`; `iosArm64` links). Every case and
  expected outcome was preserved 1:1. Exceptions: `AmountTests` stays in
  `jvmTest` because it exercises the `java.math.BigDecimal` interop that only
  exists in `jvmMain`, and the Jazzer fuzz harnesses (`ZIP321FuzzTest`,
  `ZIP321Fuzzer`) remain JVM-only.
- The shared conformance corpus is now **embedded into `commonTest` sources at
  build time**: the `generateConformanceVectors` Gradle task reads
  `test-vectors/vectors/**/*.json` and generates `GeneratedVectors.kt` (raw
  JSON as string constants) plus `GeneratedZip321ConformanceTest.kt` (one
  `kotlin.test` function per vector), regenerating whenever the submodule
  updates. This removes all classloader/filesystem resource loading from the
  tests; the 13-entry expected-failure registry (`ExpectedFailures.kt`) is
  unchanged.
- `kotlinx-serialization-json` (test-only, multiplatform) moved from `jvmTest`
  to `commonTest`.
- iOS test link/run tasks are enabled again. Building them locally requires a
  full Xcode install (`DEVELOPER_DIR=/Applications/Xcode.app/Contents/Developer`
  when `xcode-select` points at the CommandLineTools).

### Removed (v2/K1)
- All **kotest** dependencies (`kotest-runner-junit5`, `kotest-property`,
  `kotest-assertions-core-jvm`, `kotest-framework-engine-jvm`). Nothing uses
  kotest after the migration. NOTE: `kotest-property` comes back together with
  the property-based tests in a later v2 PR.

### Changed (BREAKING — Kotlin Multiplatform conversion, v2/K0)
- The library is now **Kotlin Multiplatform** (`kotlin("multiplatform")`,
  Kotlin 2.0.20) instead of a JVM-only `java-library`. Targets: `jvm()`,
  `iosArm64()`, `iosSimulatorArm64()`. No `androidTarget()` yet — Android
  consumers use the JVM variant (`org.zecdev:zip321-jvm`) meanwhile; a dedicated
  Android target is a follow-up.
- **Publication / artifact layout changed (breaking for build files).** The KMP
  plugin publishes a root Gradle-module publication plus one per target instead
  of a single JVM jar. Coordinates:
  - `org.zecdev:zip321` — root module (Gradle-metadata aware consumers)
  - `org.zecdev:zip321-jvm` — JVM artifact (what plain-Maven / Android consumers
    should depend on)
  - `org.zecdev:zip321-iosarm64`, `org.zecdev:zip321-iossimulatorarm64`
  Gradle consumers that depend on `org.zecdev:zip321` keep working; consumers
  that resolved the raw JVM jar must switch to `org.zecdev:zip321-jvm`.
- Production sources moved to `commonMain` and now compile for all targets with
  **zero runtime dependencies**.

### Removed
- `io.github.copper-leaf:kudzu-core` (parser combinators) — the ZIP-321 parser
  is now a hand-rolled, dependency-free state machine in `commonMain` that
  preserves v1 accept/reject behavior and error types exactly (verified against
  the conformance corpus and the full test suite).
- `com.google.guava:guava` and `org.apache.commons:commons-math3` — both were
  verified unused in production and tests and dropped. NOTE: `commons-math3` was
  previously exported via `api(...)`, so this removes it from consumers'
  compile classpath (breaking only for consumers that were relying on the
  transitive export, which the library itself never used).

### Changed — `NonNegativeAmount`
- Internally reimplemented as checked `Long` zatoshi fixed-point (no
  `java.math.BigDecimal` in the shared code). The `Long` and `String`
  constructors and the public API are unchanged and behavior is preserved,
  including v1 leniency (`"123."` and `".5"` still accepted) and the known
  8-significant-digit rounding bug in the render path (large amounts such as
  `20999999.99999999` still render as `21000000`; fixed in a later PR).
- The `BigDecimal` constructor and the `zecToZatoshi` / `zatoshiToZEC` /
  `BigDecimal.roundZec` helpers remain available to **JVM** consumers as
  `jvmMain` extensions (source-compatible: `NonNegativeAmount(BigDecimal(...))`
  still compiles on the JVM), but are not available on iOS/common.
- Minor: the `commonMain` decimal-string parser accepts only plain decimal
  notation. Scientific/exponent amount strings (e.g. `"1e2"`), which the old
  `BigDecimal(String)` path would have accepted but which are unreachable from
  the ZIP-321 amount grammar and untested, now throw. `BigDecimal`-typed inputs
  on the JVM (including exponents) are unaffected.

### Added
- `test-vectors` git submodule pointing at the shared ZIP-321 conformance
  vector corpus (`zcash-zip321-test-vectors`): 22 valid and 28 invalid
  vectors verified against the librustzcash `zip321` reference oracle.
- Conformance runner (`org.zecdev.zip321.conformance.Zip321ConformanceSpec`,
  test-only) that exercises every corpus vector against the v1 parser and
  renderer. Known divergences from the reference semantics are documented as
  expected failures in `ExpectedFailures.kt` (13 entries: 3 valid vectors v1
  rejects, 7 invalid vectors v1 accepts, 3 canonical-URI render mismatches
  including an amount-corrupting rounding bug in
  `NonNegativeAmount.zatoshiToZEC`); fixed vectors fail loudly as XPASS until
  their entry is removed. Adds `kotlinx-serialization-json` 1.7.3 as a
  test-only dependency and registers `test-vectors/vectors` as a test
  resources root.

## 1.0.1
This version fixes issues with Orchard-only UAs and Sapling addresses URIs

### Fixes
- [#44] Orchard-only UAs failed to be parsed as valid addresses
- [#45] Payment request to Sapling Address fails to be parsed
## 1.0.0

This version was audited by Least Authority. You can find the report [here](Docs/Least Authority -ZCG Kotlin and Swift Payment URI Prototypes Final Audit Report.pdf)

### Added
- `ZIP321` object now has a `SproutRecipientsNotAllowed` error 
- `OtherParam` has to be used to define `otherparams`
### Changed
- `ZIP321` parser object now takes a `ParserContext` for `request()` for network
  specific validations and logic
- Parser will always validate addresses with the `ParserContext.isValid()` function and
  then will evaluate whatever validations the caller passed as argument.
- `Payment` no can take optional amounts
## 0.0.6
### Added 
 - Open `NonNegativeAmount.value`
## 0.0.5
### Bugfix
- [#35] Issue with `removeFirst` on Android 27 targets 
## 0.0.4
same code but released was automated through Github Actions.
## 0.0.3
Changed package name and prepared for Maven Central publishing.

## 0.0.2

### Added 
- ZIP321 enum now has `ParserResult`
```
sealed class ParserResult {
        data class SingleAddress(val singleRecipient: RecipientAddress): ParserResult()

        data class Request(val paymentRequest: PaymentRequest): ParserResult()
    }
```

- `fun request(uriString: String, validatingRecipients: ((String) -> Boolean)?): ParserResult`
- `MemoBytes` now supports `fun fromBase64URL(string: String): MemoBytes`

### modified
- `Amount` was changed to `NonNegativeAmount`


## [0.0.1] - 2023-11-27

First version of Zcash Kotlin Payment URI library

This project should be considered as "under development". Although we respect Semantic
Versioning, things might break.

Made ZIP321 API public and all the related types. 
