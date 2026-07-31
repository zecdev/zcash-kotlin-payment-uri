# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [2.0.0] - Unreleased

A deliberate breaking-change release: the public API was reshaped to match the cross-language v2
contract shared with the companion [Kotlin sibling's Swift counterpart](https://github.com/zecdev/zcash-swift-payment-uri),
the library became Kotlin Multiplatform, parsing became a total (non-throwing) operation, every
runtime dependency was removed, and recipient-address validation was **delegated in full** to the
caller. See the migration table in `README.md` for a caller-focused summary.

### Added

- **`NonNegativeAmount`, rebuilt** (`lib/src/commonMain/kotlin/org/zecdev/zip321/model/NonNegativeAmount.kt`):
  the v2 amount type keeps the v1 type's name but is an entirely new, public, `Comparable`
  `@JvmInline value class` wrapping an **unsigned `ULong`** zatoshi count
  (`NonNegativeAmount.MAX_MONEY` = `2_100_000_000_000_000u`). `Result`-based factories
  `NonNegativeAmount.zatoshi(ULong)` (raw zatoshi) and `NonNegativeAmount.zec(String)` (decimal ZEC
  string) enforce the **strict** ZIP-321 `amountparam` grammar (`1*DIGIT [ "." 1*8DIGIT ]`) using
  checked integer arithmetic only; `decimalString()` renders exactly like the reference
  `amount_str` (whole part always, fraction only when nonzero, trailing zeros trimmed) — it does
  not inherit the v1 8-significant-digit rounding bug (see Fixed/Security). Failures carry the
  sealed `NonNegativeAmount.AmountException` (`NegativeAmount`, `ExceededSupply`,
  `TooManyFractionalDigits`, `InvalidDecimalString`), kept 1:1 with the Swift library's
  `AmountError`; `NegativeAmount` is unreachable and retained only for taxonomy parity.
  The `ULong` backing makes non-negativity **structural** rather than checked, matching the
  `u64`-backed `Zatoshis` of the librustzcash `zip321` reference and the `UInt64`-backed
  `NonNegativeAmount` of `zcash-swift-payment-uri`. **Java-interop consequence (accepted v2
  break):** Kotlin name-mangles the `value` accessor and every function that takes or returns an
  unsigned type, so those members are not callable from plain Java under their Kotlin names in the
  `zip321-jvm` artifact. Kotlin consumers (including Android) are unaffected; Java callers should
  go through `zec(String)` / `decimalString()` or add a thin Kotlin shim.
- **`ZIP321Error`**: a sealed, data-leakage-free error taxonomy (`InvalidBase64`,
  `MemoBytesError`, `TransparentMemo`, `ZeroValuedTransparentOutput`, `TooManyPayments`,
  `DuplicateParameter`, `RecipientMissing`, `InvalidAddress`, `UnknownRequiredParameter`,
  `InvalidParamIndex`, `AmountExceededSupply`, `AmountInvalid`, `InvalidURI`, `ParseError`)
  mirroring the shared cross-language conformance-corpus discriminants. See Security below.
- **`Network`, `AddressDescriptor` and `AddressValidator`** (package `org.zecdev.zip321`): the
  delegation contract. `Network` names the consensus network a request is parsed against;
  `AddressValidator` is a `fun interface` returning `AddressDescriptor?` (so a lambda works
  directly); `AddressDescriptor(network, isTransparent, canReceiveMemos)` is exactly the three
  facts ZIP-321 semantics depend on. `isTransparent` and `canReceiveMemos` are independent on
  purpose, so address kinds that are neither plainly transparent nor plainly shielded (TEX; a
  Unified Address whose receiver set the caller resolves) are describable without this library
  enumerating address kinds it deliberately does not model.
- **`ZIP321.parse(uri, expecting, validator, maxInputBytes)`**: the *total* — and only — parsing
  entry point, returning `Result<PaymentRequest>`. Input guards run before any grammar work: input
  above `maxInputBytes` (default `ZIP321.DEFAULT_MAX_INPUT_BYTES` = 8 KiB) fails with
  `InvalidURI(INPUT_TOO_LARGE)`; empty input fails with `ParseError(EMPTY_INPUT)`; a non-`zcash:`
  scheme fails with `InvalidURI(NOT_ZCASH_SCHEME)`; a `//` authority component fails with
  `InvalidURI(INVALID_AUTHORITY)`. `validator` is REQUIRED (see Security). The empty request
  `zcash:` parses to a `PaymentRequest` with zero payments.
- **`Payment.create(recipientAddress, amount, memo, label, message, otherParams)`**: a
  `Result`-based construction factory enforcing the reference `to_payment` rules at construction
  time — a memo to a transparent recipient fails with `TransparentMemo`, and a zero-valued amount
  to a transparent recipient fails with `ZeroValuedTransparentOutput` (a new consensus check,
  enforced on both the construction and parse paths).
- **`Payment.Builder`** (`Builder(recipient)` + chainable `amount(NonNegativeAmount)`,
  `amount(zec: String)`,
  `memo(MemoBytes)`, `memo(utf8: String)`, `label(...)`, `message(...)`,
  `otherParam(name, value)`, terminal `build(): Result<Payment>`) and **`PaymentRequest.Builder`**
  (`add(payment)` auto-indexing from `0`, `add(payment, at = n)` for an explicit `paramindex`,
  terminal `build(): Result<PaymentRequest>`), plus the **`paymentRequest { }` DSL** — the
  idiomatic Kotlin equivalent of the Swift library's `@resultBuilder` entry point
  `PaymentRequest.build { }` — with `payment(recipient) { ... }`, `payment(prebuilt)`, and
  `payments(list)` forms. Fallible inputs are validated lazily at `build()`; when several fields
  are invalid, the first error wins in fixed field order (amount → memo → other params →
  structural rules), matching the Swift library byte-for-byte.
- **`PaymentRequest.indexedPayments: List<IndexedPayment>`** and
  **`PaymentRequest.fromIndexedPayments(...)`**, exposing and accepting explicit, non-contiguous
  ZIP-321 `paramindex` values (validating index uniqueness via `DuplicateParameter` and the ≤ 9999
  index bound).
- Dependency-free primitives backing amount/memo/URI parsing, under
  `lib/src/commonMain/kotlin/org/zecdev/zip321/{parser,encodings}/`: **`Base64URL`** (unpadded
  RFC 4648 §5 base64url, matching the reference `BASE64_URL_SAFE_NO_PAD`), a single-pass
  **`Scanner`**, and a strict **`AmountParser`** / **`QCharCodec`** (reference-exact `qchar`
  percent-encoding) underpinning the URI grammar rewrite. Note what is NOT here: no hash, no
  Bech32, no Base58Check. Because address validation is delegated, this library needs no
  cryptography at all. The equivalent reference checkers live in `commonTest`
  (`support/{Sha256,Bech32,Base58Check}.kt`, over a platform SHA-256 via an `expect`/`actual` in
  the test source sets) purely so the shared conformance corpus's checksum-corruption, mixed-case,
  Sprout and wrong-network vectors are executable against a `ReferenceAddressValidator` — they are
  test sources and are never shipped.
- The shared, oracle-verified ZIP-321 conformance corpus
  ([zecdev/zcash-zip321-test-vectors](https://github.com/zecdev/zcash-zip321-test-vectors)),
  consumed as a test-only git submodule at `test-vectors/` and embedded into `commonTest` at build
  time (`generateConformanceVectors` reads `test-vectors/vectors/**/*.json` and generates
  `GeneratedVectors.kt` / `GeneratedZip321ConformanceTest.kt`, removing all
  classloader/filesystem resource loading from the tests), plus a conformance test runner that
  asserts the **exact** `ZIP321Error` discriminant and byte-identical canonical re-rendering
  against the librustzcash `zip321` reference. Bumped through several adjudicated corpus revisions
  to `53911fb` (51 vectors); the expected-failure ledger, which started at 13 documented
  divergences, is now **empty**.
- Deterministic property-style round-trip tests (`PropertyGenerators.kt`/`PropertyTests.kt` in
  `commonTest`): a seeded `SplitMix64` PRNG driving five laws — full parse/render round trip,
  `NonNegativeAmount` decimal round trip, `MemoBytes` base64url round trip, `QCharCodec` round trip, and
  `paramindex` preservation — over 1,400 fixed-seed cases across every KMP target (this supersedes
  the earlier note that `kotest-property` would return; it does not, in favor of a from-scratch
  deterministic PRNG that runs on `jvm` and `iOS` alike, not just `jvm`), plus three more laws
  added with the delegation redesign: the two single-recipient spellings always parse equal,
  `otherParams` survives every round trip as a list, and — adversarially — a `Payment` whose
  other-param names repeat never constructs, through either `Payment.create` or `Payment.Builder`.
- **Kover 100% line / ≥98% branch coverage gate** on `commonMain`/`jvmMain` (`koverVerify`, wired
  into `check`/`build`), with a bounded (3-site) `@KoverExcludeWithRationale` exemption annotation
  for genuinely unreachable defensive guards. Coverage progressed from 87.7% line / 75.7% branch to
  100.0% line / 98.6% branch (850/850 and 713/723 as measured on the final tree) via ~35
  new/expanded test functions, dead-code deletion (see Removed), and restructuring for testability.
  Kover does not instrument Kotlin/Native, so the iOS `sha256` actual is outside its scope by
  construction and is proven instead by `iosSimulatorArm64Test` running the same `commonTest`
  suite; the JVM actual is in scope and fully covered.
- API documentation via **Dokka Gradle Plugin v2** (`./gradlew :lib:dokkaGenerate`, HTML output at
  `lib/build/dokka/html`), with `reportUndocumented`/`failOnWarning` wired project-wide so an
  undocumented public symbol or a broken `[Foo]`-style doc link fails the build. `lib/Module.md` is
  the module-level landing page: it links to (rather than duplicates) the four canonical usage
  scenarios written as a runnable KDoc sample on `org.zecdev.zip321.paymentRequest`, plus a security
  section.
- **`.github/workflows/ci.yml`**, a 4-required-job workflow replacing the stale single-job
  `basic-test.yml`: `test-jvm` (`ubuntu-latest` × JDK 17/21, runs
  `jvmTest koverVerify ktlintCheck detekt`), `test-apple` (`macos-15`, runs
  `iosSimulatorArm64Test` + `linkDebugTestIosArm64`), `fuzz-smoke` (`ubuntu-latest`, runs the
  existing Jazzer JUnit `@FuzzTest` with `JAZZER_FUZZ=1` for a bounded 45-second mutation-based
  smoke run), and `dokka` (`ubuntu-latest`, `./gradlew :lib:dokkaGenerate`, and — as of this
  release — `scripts/check-readme-snippets.sh`). Every job checks out `submodules: recursive`.
- **`scripts/check-readme-snippets.sh`**: verifies that every `` ```kotlin `` code block in the
  canonical KDoc samples (the `paymentRequest` DSL entry point and `Payment.Builder`) appears
  verbatim in `README.md`, wired into the `dokka` CI job, so the README's Quick Start snippets can
  never silently drift from the KDoc they were copied from.

### Changed

- **Breaking: converted to Kotlin Multiplatform** (`kotlin("multiplatform")`, Kotlin 2.0.20)
  instead of a JVM-only `java-library`. Targets: `jvm()` (JVM 8 bytecode), `iosArm64()`,
  `iosSimulatorArm64()`. There is no `androidTarget()` yet; Android consumers use the JVM variant
  meanwhile. Production sources moved to `commonMain` and compile for every target with **zero
  runtime dependencies**.
- **Breaking: publication/artifact layout changed.** The KMP plugin publishes a root
  Gradle-module publication plus one per target instead of a single JVM jar. Coordinates:
  `org.zecdev:zip321` (root module, Gradle-metadata-aware consumers), `org.zecdev:zip321-jvm`
  (the JVM artifact; what plain-Maven/Android consumers should depend on),
  `org.zecdev:zip321-iosarm64`, `org.zecdev:zip321-iossimulatorarm64`. Gradle consumers depending
  on `org.zecdev:zip321` keep working; consumers that resolved the raw JVM jar must switch to
  `org.zecdev:zip321-jvm`.
- **Breaking: address validation is fully delegated; `ParserContext` is DELETED.** `ParserContext`
  conflated "the consensus network" with "the built-in structural address validator"; the first
  becomes `Network`, and the second ceases to exist in this library. `RecipientAddress` is now an
  opaque `value` plus the `descriptor` its validator produced — construct it directly from an
  already-validated address, or via `RecipientAddress.create(value, validator): RecipientAddress?`.
  The throwing constructor and `RecipientAddressError` are gone, as is the v1 "AND-composed
  optional closure" model: there is no built-in check left for an injected one to compose with.
  A recipient the validator places on a network other than `expecting` is rejected with
  `InvalidAddress` — a COMPARISON, not a validation.
- **Breaking: `ParsedRequest` / `ZIP321.ParserResult` are DELETED.** They existed to distinguish
  `zcash:<addr>` from `zcash:?address=<addr>`, but ZIP-321 URI Semantics says those denote the SAME
  request — the reference `TransactionRequest` has no notion of the difference either. Modelling it
  made a purely syntactic accident observable and forced every caller to branch on it. `parse` now
  returns `Result<PaymentRequest>`, and both spellings construct EQUAL `PaymentRequest` values
  (asserted explicitly and by property test).
- **Breaking: `Payment.otherParams` is a non-null `List<OtherParam>`** (default `emptyList()`).
  ZIP-321 has no way to spell the difference between "absent" and "empty", and neither does the
  reference: an absent list and an empty list render identically, so representing both would make
  two distinct `Payment` values share a URI and break the round-trip law. **Duplicate `otherparam`
  names are now rejected at construction** with `DuplicateParameter(name, null)`, regardless of the
  values — so no `Payment` can exist that renders to a URI the parser would reject.
- **Breaking: public API reshape.** `ZIP321.parse(...)` is the parsing entry point (see
  Added); the throwing `ZIP321.request(uriString, …)` parse shim is **removed**, and `request(...)`
  now names only the rendering overloads. The public
  `ZIP321.Errors` grab-bag is now **internal**, replaced by the sealed `ZIP321Error` taxonomy;
  Sprout rejection now surfaces as `InvalidAddress`. **The v1 `NonNegativeAmount` class was removed
  entirely** (see Removed) — `Payment.amount` is now the rebuilt `NonNegativeAmount?`, whose `value`
  is a `ULong`. The throwing `Payment(...)` constructor
  remains as a deprecated shim over `Payment.create`; `label`/`message` are now plain decoded
  `String?`. **`OtherParam`'s constructor is now internal**; construct via
  `OtherParam.create(name, value): Result<OtherParam>`, which validates the name against the
  ZIP-321 grammar and reserved-name rules (empty name, reserved query key, `req-`-prefixed name, or
  a name that isn't a valid `paramname` all fail with `ParseError(INVALID_PARAMETER)`).
- **Breaking: `PaymentRequest` now preserves ZIP-321 paramindices**, stored by `paramindex`
  (`payments: List<Payment>` returns them ordered by ascending index; see `indexedPayments` under
  Added). `PaymentRequest(payments)` still auto-indexes sequentially from `0` and enforces the
  9999-payment cap (`TooManyPayments`). **Empty requests are now valid**: `zcash:` and `zcash:?`
  parse to `PaymentRequest(emptyList())` (previously rejected) and render back to `zcash:`; a
  single payment at the empty paramindex with no query parameters renders as the bare
  `zcash:<addr>`, not `zcash:<addr>?`. The v1 construction-time network-coherence check was
  removed — the recipient's network is reported by the caller's validator and compared against
  `expecting` at parse time. **The v1 `ZIP321.maxPaymentsAllowed = 2109`
  constant was removed**: it was a v1 remnant with no basis in ZIP-321 and wrongly rejected any
  `paramindex` in `[2108, 9999]`; the only limits are the `paramindex` grammar (≤ 9999) and
  `PaymentRequest.MAX_PAYMENT_COUNT` (9999).
- **Breaking: canonical renderer rewritten.** The renderer now renders from
  `PaymentRequest.indexedPayments`, preserving each payment's actual stored `paramindex` (a request
  whose only payment sits at index `5` renders `zcash:?address.5=…&amount.5=1`, previously
  collapsed onto the empty index; per-payment parameter order is address, amount, memo, label,
  message, then `otherParams` in stored order). The default `formattingOptions` of
  `ZIP321.uriString`/`ZIP321.request` changed to `FormattingOptions.UseEmptyParamIndex(omitAddressLabel = true)`
  (the canonical reference form); the round-trip law `parse(uriString(from = r)) == success(r)`
  holds under it for every corpus request. `FormattingOptions.EnumerateAllPayments` is now a
  documented **normalization** mode that discards stored paramindices and re-numbers payments
  sequentially from `1`. `Render` (a v1 public implementation detail) is now `internal`.
- **The parser now enforces the strict `amountparam` grammar** via the new
  `AmountParser`/`NonNegativeAmount.zec` path: a leading/trailing decimal point, a sign, whitespace,
  scientific notation, or a percent-escape in an amount value are all rejected.
- **The ZIP-321 URI grammar was rewritten onto an internal single-pass `Scanner`**, following the
  reference `nom` pipeline (`zcash:` scheme, `take_till('?')` lead address, `&`-separated query
  segments parsed as `name [ "." index ] [ "=" value ]`). Parameter names must be
  `ALPHA *( ALPHA / DIGIT / "+" / "-" )`; indices are `NONZERO 0*3DIGIT`.
  `label`/`message`/`otherparam` values are percent-decoded via the rewritten reference-exact
  `QCharCodec` (strict decode: malformed `%XX`, non-`qchar` raw bytes, and invalid UTF-8 all fail);
  `address`/`amount`/`memo` values are parsed by their own grammars verbatim (a `%` in them is
  rejected). A rejected leading address now uniformly maps to `InvalidAddress`.
- **The payment rules read capabilities off the validator's descriptor**, never off the address
  string: a `memo` is rejected when the descriptor says the recipient cannot receive one, and a
  zero-valued amount is rejected when it says the recipient is transparent. A validator that calls
  a `t1…`-looking string memo-capable is believed.
- **`MemoBytes` rewritten on the strict `Base64URL` codec** and now bounds the UTF-8 **byte**
  count, not the char count (a v1 check on `string.length` could accept a memo whose UTF-8 encoding
  exceeds 512 bytes). Accepts 0 to 512 bytes (consensus zero-pads memos to 512 bytes, so an empty
  memo is well-defined; `MemoError.MemoEmpty` was removed accordingly — see Fixed).
- **Test suite migrated from jvm-only kotest to `kotlin.test` in `commonTest`**, so the same suite
  now compiles and runs on every KMP target (`jvmTest`, `iosSimulatorArm64Test`; `iosArm64` links).
  `AmountTests` stays JVM-only (`java.math.BigDecimal` interop); the Jazzer fuzz harness remains
  JVM-only.
- **CI**: `.github/workflows/deploy-release.yml` now runs on `macos-15` (not `ubuntu-latest` —
  required for the `iosArm64`/`iosSimulatorArm64` publications to actually get built), checks out
  `submodules: recursive` (previously missing entirely), had a duplicated checkout step and a dead
  `ORG_GRADLE_PROJECT_NATIVE_TARGETS_ENABLED` env var removed, replaced the archived
  `gradle/gradle-build-action` with `gradle/actions/setup-gradle`, and bumped
  `actions/checkout`/`actions/setup-java`/`actions/upload-artifact` SHA pins. Both ktlint and
  detekt baselines were regenerated: ~74 of 76 ktlint-baseline entries and ~22 of 30
  detekt-baseline entries were dead (referencing code deleted or rewritten earlier in this
  release) and were removed; zero new findings were introduced.

### Removed

- **All non-test-only runtime dependencies**: `io.github.copper-leaf:kudzu-core` (parser
  combinators — the ZIP-321 parser is now the hand-rolled, dependency-free grammar described
  above), `com.google.guava:guava`, and `org.apache.commons:commons-math3` (both verified unused;
  `commons-math3` was previously exported via `api(...)`, so this also removes it from consumers'
  transitive compile classpath). All **kotest** test dependencies (`kotest-runner-junit5`,
  `kotest-property`, `kotest-assertions-core-jvm`, `kotest-framework-engine-jvm`).
- **The v1 `NonNegativeAmount` class removed entirely**, including its `jvmMain` `BigDecimal`
  interop shim. Its name was reused for the new value type described under Added, so this is a
  hard source break rather than a rename with an alias. Migration: replace
  `NonNegativeAmount("1.5")` with `NonNegativeAmount.zec("1.5").getOrThrow()` (strict grammar) or
  `NonNegativeAmount.zatoshi(150_000_000uL).getOrThrow()` for raw zatoshi counts; note the `uL`
  suffix — raw counts are now `ULong`.
- **`QcharString` and `ParamNameString` removed from the public surface** (see `OtherParam` under
  Changed).
- Dead code identified while driving coverage to 100% (mirroring several of the same findings
  independently made in the Swift library's own coverage drive): `Payment.isSingleAddress()`
  (zero callers); hand-written `Param`/`IndexedParameter` `equals`/`hashCode` (provably dead —
  every concrete `Param` subtype is a `data class` that synthesizes its own, which always wins over
  virtual dispatch); `AmountParser.mapError`'s unreachable `NegativeAmount` branch;
  `Parser.leadingAddress`'s always-`.address` double-dispatch (the exact same shape of bug the
  Swift library's own coverage drive independently found and fixed); `Parser.parse`'s redundant
  `IllegalArgumentException` catch; `Bech32.decode`'s redundant second ASCII re-guard; the
  two-level `CharsetValidations` pure-namespacing wrapper around `ParamNameCharacterSet`.
- **`lib/src/jvmTest/java/ZIP321Fuzzer.java`**: a dead, v1-era standalone Jazzer entry point,
  wired into no Gradle task and referencing the internal v1 `ZIP321.Errors`/`request(...)` API
  removed above. The still-live Jazzer harness is the JUnit `@FuzzTest` `ZIP321FuzzTest`, exercised
  by `test-jvm`'s `jvmTest` (regression mode) and `fuzz-smoke` (fuzzing mode).

### Fixed

- **Amount-rendering corruption, fixed by rebuilding the amount type**: the v1
  `zatoshiToZEC` 8-significant-digit rounding bug silently corrupted large amounts on re-render
  (e.g. `20999999.99999999` ZEC rendered as `21000000`, `3768769.02796286` as `3768769`).
  `NonNegativeAmount.decimalString()` now renders byte-exact for every representable amount (also listed
  under Security — this could silently alter a payment amount on re-render).
- **A single payment at a non-zero `paramindex` now re-renders faithfully** instead of being
  collapsed onto the empty index, fixing the `structure_index_gap_only_address_5` conformance
  divergence.
- **The empty request renders as `zcash:` under every `FormattingOptions`** (previously only the
  default path handled it).
- **Zero-length memos are now valid**: `MemoBytes` accepts an empty memo (`memo=` parses to a
  payment with an empty, not rejected, memo), matching the reference (consensus zero-pads memos to
  512 bytes). `MemoBytes.MemoError.MemoEmpty` was removed accordingly.
- **Empty `qchar` values are valid**: a URI containing an empty `message=`/`label=` now parses
  successfully, matching the reference.
- **Regtest transparent P2SH prefix corrected** (`t2`, not `t3`): the confirmed
  `B58_SCRIPT_ADDRESS_PREFIX` table shows regtest reuses the testnet script version bytes.

### Security

- **Address validation is fully DELEGATED, and the delegate is AUTHORITATIVE.** This library
  implements the ZIP-321 URI **grammar** and nothing else: it does not decode, classify or checksum
  Zcash addresses, and it ships no Bech32, no Base58Check and no hash. A caller-supplied
  `AddressValidator` is a **required** argument of `ZIP321.parse`; returning `null` rejects the
  address, and the `AddressDescriptor` it returns is trusted verbatim and drives the ZIP-321
  payment rules. This is a deliberate security decision, not an omission: a URI parser's structural
  approximation of "is this a valid Zcash address" is exactly the kind of check that looks
  authoritative while being subtly wrong — it cannot decode Unified Address receivers, and it
  cannot know which address kinds a given wallet is willing to pay. Making the validator required
  means address validity can never silently come from such an approximation. Integrators SHOULD
  implement it by delegating to their Zcash SDK's own address support (librustzcash's
  `ZcashAddress` via the mobile SDKs' FFI/JNI bindings). The one rule applied on top of the
  validator's verdict is a COMPARISON, not a validation: the accepted address must belong to the
  `Network` named by `expecting`.
- **No cryptography, and no third-party runtime dependencies.** Because validation is delegated,
  there is nothing crypto-shaped in this library to get wrong or to audit. The test suite carries
  its own reference Bech32/Base58Check checkers over a platform SHA-256, purely so the shared
  conformance corpus's checksum-corruption vectors are executable; those are `commonTest` sources
  and are never shipped.
- **Errors never carry sensitive input.** The sealed `ZIP321Error` taxonomy is constructed so that
  no case can carry an address, memo contents, an amount, or a raw URI slice — only parameter
  names, indices, counts, or fixed `StaticReason` values (the one bounded exception,
  `InvalidParamIndex`'s raw index token, is capped at a few characters by the ZIP-321 grammar).
- **A bounded input size.** `ZIP321.parse` rejects input above `maxInputBytes` (8 KiB by default)
  before any grammar or address-validation work runs, bounding the cost of parsing adversarial
  input.
- **SHA-256 is the platform's, not ours.** The library ships no hash implementation of its own:
  `java.security.MessageDigest` on JVM/Android and CommonCrypto's `CC_SHA256` on iOS sit behind a
  single `expect`/`actual`, matching the Swift library's CryptoKit switch. Both are first-party to
  their platform — audited, maintained, hardware-accelerated — so this buys the audited code paths
  at no supply-chain cost.
- **Zero third-party runtime dependencies**, minimizing supply-chain surface.
- **100% line coverage (≥98% branch coverage)**, deterministic property-based round-trip tests,
  full agreement with the shared, oracle-verified cross-language conformance corpus, and bounded
  Jazzer mutation-based fuzzing (`fuzz-smoke`), all enforced as required CI gates on every pull
  request.
- **Fixed a silent amount-corruption bug** (the v1 `zatoshiToZEC` 8-significant-digit rounding bug,
  see Fixed): a large payment amount could be altered without error on re-render, which is a
  correctness issue with direct security relevance for a payment-request library.

## [1.0.2] - 2026-01-26

Tagged and released (`v1.0.2`, GitHub release "Latest" as of this writing) with no CHANGELOG entry
at the time — this entry documents that release after the fact, verified against
`git diff v1.0.1 v1.0.2` and `gh release list`. No library API or behavior changed.

### Changed
- Bumped the `org.jreleaser` Gradle plugin from `1.14.0` to `1.22.0`.
- Tuned JReleaser's Maven Central deployment retry policy (`maxRetries` / `retryDelay`) to work
  around JReleaser failing after its default retry budget even though the artifact had, in
  practice, already published successfully.
- Removed dead, commented-out `tasks.jar { ... }` configuration from `lib/build.gradle.kts`.

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
