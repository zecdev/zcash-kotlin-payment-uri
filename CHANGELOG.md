# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## Unreleased
### Added (v2/K9)
- **Reference-exact ZIP-321 `qchar` codec** (`encodings/QcharCodec.kt`): the `QCharCodec`
  `encode`/`decode` pair is rewritten to mirror the librustzcash `zip321` reference. `encode`
  percent-encodes exactly the *complement* of the raw `qchar` set (space, `"`, `#`, `%`, `&`,
  `/`, `<`, `=`, `>`, `?`, `[`, `\`, `]`, `^`, `` ` ``, `{`, `|`, `}`, the C0 controls, DEL, and
  every non-ASCII byte as uppercase `%XX`). `decode` is now **strict** and returns `null` on
  failure: each `%XX` must be two hex digits (either case), each raw byte must be a `qchar`
  byte, and the decoded bytes must be valid UTF-8 (overlong sequences, lone continuation bytes,
  unpaired surrogates and truncated sequences are rejected — the previous `URLDecoder`-derived
  decoder produced U+FFFD replacements instead). New `isQcharByte`/`isValueByte` predicates are
  exposed for the K10/K11 scanner. The `String.qcharDecode()` extension delegates to the codec
  and throws `IllegalArgumentException` on a strict-decode failure (call `QCharCodec.decode`
  for a nullable result).

### Fixed (v2/K9)
- **Empty `qchar` values are valid**: `QcharString.from("")` now succeeds (a zero-length
  `*qchar` value round-trips through both the encoded and decoded views), matching the
  reference which accepts an empty `message=`/`label=`. (Kotlin's parse path already decoded
  empty values via `qcharDecode`, so no conformance vector changed class.)

### Changed (v2/K8) — BREAKING: address validation is fully delegated
- **The library no longer validates Zcash addresses.** It implements the
  [ZIP-321](https://zips.z.cash/zip-0321) URI **grammar** and nothing else.
  Whether a recipient string is a valid, payable Zcash address — and what that
  recipient can receive — is now answered exclusively by a caller-supplied
  validator, whose verdict is AUTHORITATIVE and is never second-guessed.
- **New public API** (`org.zecdev.zip321`):
  - `enum class Network { MAINNET, TESTNET, REGTEST }` — the consensus network a
    request is parsed against.
  - `data class AddressDescriptor(network, isTransparent, canReceiveMemos)` —
    what a validator reports about an address it accepts. These are exactly the
    three facts ZIP-321 semantics depend on: the network the request is checked
    against, whether a `memo` may accompany the recipient, and whether a
    zero-valued output to it is permitted. `isTransparent` and
    `canReceiveMemos` are independent on purpose, so kinds that are neither
    plainly transparent nor plainly shielded (TEX; a UA whose receiver set the
    caller resolves) are describable without this library enumerating address
    kinds it deliberately does not model.
  - `fun interface AddressValidator { fun validate(address: String): AddressDescriptor? }`
    — `null` rejects the address; anything else is trusted verbatim. Being a
    `fun interface`, a lambda can be passed directly.
- **`ParserContext` is DELETED.** It was both "the network" and "the built-in
  structural validator"; those roles are now `Network` and `AddressValidator`
  respectively. Its checksum/prefix machinery does not move into the library —
  it is gone from `commonMain` entirely (the equivalent checkers live in
  `commonTest` as test support, v2/K5–K7).
- **`RecipientAddress` is now `value` + `descriptor`.** The public constructor
  wraps an address a caller has ALREADY validated; `RecipientAddress.create(value,
  validator)` validates and returns `null` on rejection. There is no throwing
  constructor and no `RecipientAddressError` any more. `isTransparent()`/memo
  capability are read off the descriptor rather than re-derived from the string.
- **`ZIP321.request(uriString, expecting: Network, validator: AddressValidator)`**
  replaces `request(uriString, context: ParserContext, validatingRecipients:
  ((String) -> Boolean)?)`. The validator is REQUIRED, precisely so that address
  validity can never silently come from a structural approximation baked into a
  URI parser. The v1 "AND-composed optional closure" model is gone: there is no
  built-in check for an injected one to compose with.
- Wallets should implement `AddressValidator` by delegating to their Zcash SDK's
  own address support (librustzcash's `ZcashAddress` via the mobile SDKs'
  FFI/JNI bindings), which is the only place that can answer these questions
  correctly — including Unified Address receiver decoding and which address
  kinds the wallet is willing to pay.
- Conformance xfail burn-down: `invalid_address_sapling_bad_checksum`,
  `invalid_address_unified_mainnet_bad_checksum`,
  `invalid_address_transparent_bad_checksum` and
  `invalid_address_sapling_mixed_case` now pass and were removed from the
  expected-failure map. They pass because the test suite injects a validator
  (`ReferenceAddressValidator`) that really verifies encodings — which is
  exactly the point: the library rejects what the VALIDATOR rejects.
- Removed the now-unused `CharsetValidations` Base58/Bech32 character sets and
  their `isValidBase58Char`/`isValidBech32Char` helpers.

### Added (v2/K8) — the expected network is enforced
- **A recipient whose network is not the expected one is rejected.** A ZIP-321
  request is parsed against exactly one consensus network, named by
  `expecting`. When the validator accepts an address but reports a different
  `AddressDescriptor.network`, the request is rejected with an invalid-address
  error carrying the payment's `paramindex`.

  This is a COMPARISON, not a validation: the library still learns an address's
  network only from the validator, and has no way of its own to tell. ZIP-321
  itself is network-agnostic — the librustzcash reference parses addresses
  without a network at all — so enforcing it is a consumer-library requirement,
  made explicit and documented at the parse boundary.

### Added (v2/K7) — test support only
- **Base58Check reference checker**
  (`lib/src/commonTest/kotlin/org/zecdev/zip321/support/Base58Check.kt`) —
  **test support, not part of the shipped library**: `decode(String):
  ByteArray?` (base58 big-integer decode via `ByteArray`/`MutableList<Byte>`
  arithmetic — no `BigInteger` dependency — 4-byte SHA-256d checksum
  verification using the v2/K5 `Sha256`, leading-`'1'` zero-byte preservation)
  and `verify(String, expectedVersionBytes: List<ByteArray>): Boolean`. Port of
  the Swift reference's test-support `Base58Check.swift`, including its table
  citing the mainnet/testnet/regtest transparent-address version-byte prefixes
  from librustzcash `zcash_protocol`. `Base58CheckTests` ports
  testnet/mainnet P2PKH decode, corpus corrupted-checksum rejection,
  wrong-version-bytes rejection, leading-zero-byte preservation,
  invalid-alphabet-character rejection (`0`/`O`/`I`/`l`), and too-short-input
  rejection.

### Added (v2/K6) — test support only
- **Bech32 / Bech32m reference checker**
  (`lib/src/commonTest/kotlin/org/zecdev/zip321/support/Bech32.kt`), per
  BIP-173 / BIP-350 — **test support, not part of the shipped library**:
  `Variant` enum (`BECH32` checksum constant `1`, `BECH32M` checksum constant
  `0x2bc830a3`), `decode(String): Decoded?` (mixed-case rejected *before*
  lowercasing, 1023-char limit, printable-ASCII 33..126 charset, separator is
  the last `'1'`, HRP 1..83 chars, >= 6 checksum chars, BCH polymod), and
  `verify(String, expectedHrp, variant): Boolean`. Port of the Swift
  reference's test-support `Bech32.swift`, including its citation of why the
  length limit is 1023 (the `bech32` Rust crate's per-checksum `CODE_LENGTH`)
  rather than BIP-173's 90-char segwit cap. `Bech32Tests` ports the
  BIP-173/BIP-350 known-answer vectors, real Sapling/Unified/regtest Zcash
  addresses, and their corpus-corrupted (checksum-broken) variants.

### Added (v2/K5) — test support only
- **SHA-256 wrapper for the reference address checkers**
  (`lib/src/commonTest/kotlin/org/zecdev/zip321/support/Sha256.kt`), which is
  **test support and not part of the shipped library**. `hash(ByteArray)` and
  `doubleHash(ByteArray)` delegate to an `internal expect fun
  sha256(ByteArray): ByteArray` whose actuals are each platform's **own**
  cryptographic library — `java.security.MessageDigest.getInstance("SHA-256")`
  in `jvmTest` (a fresh, therefore thread-safe, digest instance per call) and
  CommonCrypto's `CC_SHA256` via Kotlin/Native's bundled `platform.CoreCrypto`
  interop in `iosTest`. `expect`/`actual` behaves in test source sets exactly
  as it does in main source sets; `iosTest` is materialised for both iOS
  targets by Kotlin's default hierarchy template.

  It lives in `commonTest` because the library **performs no address
  validation of its own** and therefore needs no cryptography: `commonMain`
  ships zero hash, Bech32 or Base58Check code. This digest exists solely so the
  test-only address-encoding checkers (v2/K6, v2/K7) can verify the SHA-256d
  checksums of transparent addresses, which is what makes the shared
  conformance corpus's checksum-corruption vectors executable.

  `Sha256Tests` pins the *wiring* — expect/actual plumbing, digest byte order,
  `doubleHash` composition — with four known-answer vectors: `SHA-256("")`,
  FIPS 180-4 B.1 `"abc"`, B.2's two-block message, and SHA-256d(`"hello"`).
  Living in `commonTest`, they run under both `jvmTest` and
  `iosSimulatorArm64Test`, exercising each actual in turn.

### Fixed (v2/K4)
- **Zero-length memos are now valid** (conformance fix): `MemoBytes` accepts
  0 to 512 bytes, matching the reference implementation (consensus zero-pads
  memos to 512 bytes, so an empty memo is well-defined). A URI containing
  `memo=` now parses to a payment with an empty (not absent) memo instead of
  being rejected, and the conformance vector `structure_empty_memo_on_sapling`
  now passes — its entry has been removed from the expected-failure map
  (now 12 entries). The `MemoBytes.MemoError.MemoEmpty` case has been removed
  accordingly.
- **`MemoBytes(String)` now bounds the UTF-8 byte count, not the char count**:
  the v1 check `string.length <= 512` could accept multi-byte strings whose
  UTF-8 encoding exceeds 512 bytes (an invalid memo). The constructor now
  encodes first and checks the byte length, matching the Swift implementation
  and the ZIP-302 limit.

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
