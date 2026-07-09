# zcash-kotlin-payment-uri

| Job | Status |
| --- | --- |
| `test-jvm` (JDK 17 / 21) | [![test-jvm](https://github.com/zecdev/zcash-kotlin-payment-uri/actions/workflows/ci.yml/badge.svg?branch=main)](https://github.com/zecdev/zcash-kotlin-payment-uri/actions/workflows/ci.yml) |
| `test-apple` (iOS simulator + device-target link) | [![test-apple](https://github.com/zecdev/zcash-kotlin-payment-uri/actions/workflows/ci.yml/badge.svg?branch=main)](https://github.com/zecdev/zcash-kotlin-payment-uri/actions/workflows/ci.yml) |
| `fuzz-smoke` (Jazzer, bounded) | [![fuzz-smoke](https://github.com/zecdev/zcash-kotlin-payment-uri/actions/workflows/ci.yml/badge.svg?branch=main)](https://github.com/zecdev/zcash-kotlin-payment-uri/actions/workflows/ci.yml) |
| `dokka` (fail on warning) | [![dokka](https://github.com/zecdev/zcash-kotlin-payment-uri/actions/workflows/ci.yml/badge.svg?branch=main)](https://github.com/zecdev/zcash-kotlin-payment-uri/actions/workflows/ci.yml) |

All four jobs are required checks on `main` and on every pull request (see `.github/workflows/ci.yml`).
The badges above all point at the same workflow run; GitHub does not support per-job badges, so
check the [Actions tab](https://github.com/zecdev/zcash-kotlin-payment-uri/actions/workflows/ci.yml)
for the individual job's status.

![Platforms](https://img.shields.io/badge/platforms-JVM%2017%2B%20%7C%20Android%20(JVM%20artifact)%20%7C%20iOS%20arm64%20%2B%20simulator%20arm64-blue)
![Kotlin](https://img.shields.io/badge/kotlin-2.0%2B-orange)
![Maven Central](https://img.shields.io/badge/maven%20central-org.zecdev%3Azip321-blue)
![License](https://img.shields.io/badge/license-MIT-green)
![Dependencies](https://img.shields.io/badge/dependencies-zero-brightgreen)

A small, dependency-free Kotlin Multiplatform library for constructing, rendering, and parsing
[ZIP-321](https://zips.z.cash/zip-0321) Zcash payment request URIs.

## What is it?

Quote from [ZIP-321](https://zips.z.cash/zip-0321):
> [..] a standard format for payment request URIs. Wallets that recognize this format enable users
> to construct transactions simply by clicking links on webpages or scanning QR codes.

`zcash-kotlin-payment-uri` implements the construction, canonical rendering, and parsing sides of
that specification:

**Example**
`zcash:?address=tmEZhbWHTpdKMw5it8YDspUXSMGQyFwovpU&amount=123.456&address.1=ztestsapling10yy2ex5dcqkclhc7z7yrnjq2z6feyjad56ptwlfgmy77dmaqqrl9gyhprdx59qgmsnyfska2kez&amount.1=0.789&memo.1=VGhpcyBpcyBhIHVuaWNvZGUgbWVtbyDinKjwn6aE8J-PhvCfjok`

The implementation is conformant with the [librustzcash `zip321`](https://github.com/zcash/librustzcash/tree/main/components/zip321)
reference implementation **except for `req-asset` (ZIP-321 Custom Assets / ZSA), which is
intentionally rejected pending ecosystem support — matching the librustzcash reference, which does
not implement it either**. `req-asset` parameters therefore fail with `UnknownRequiredParameter`,
which is the ZIP-321 forward-compatibility rule for an unrecognised required parameter. Support is
tracked in [issue #68](https://github.com/zecdev/zcash-kotlin-payment-uri/issues/68). Everything
else — every parse decision, exact error discriminant, and canonical re-rendering — is verified
against the shared, oracle-verified conformance corpus,
[zecdev/zcash-zip321-test-vectors](https://github.com/zecdev/zcash-zip321-test-vectors) (consumed
as a test-only git submodule at `test-vectors/`). That same corpus is consumed identically by the
companion [Swift library](https://github.com/zecdev/zcash-swift-payment-uri), so both
implementations agree byte-for-byte on every vector.

## Install

This is a Kotlin Multiplatform library targeting `jvm`, `iosArm64`, and `iosSimulatorArm64` with
**zero runtime dependencies**. There is no dedicated Android target yet; Android consumers depend
on the JVM artifact (see below). Building the library requires JDK 17+ (Gradle's toolchain support
provisions whatever JDK the `jvm` target itself needs); the `jvm` target compiles to JVM 8
bytecode, so consumers are not bound to JDK 17+ at runtime.

**Kotlin Multiplatform consumers** (a `commonMain` source set resolves the right per-target
artifact automatically via Gradle metadata):

```kotlin
// build.gradle.kts
kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation("org.zecdev:zip321:2.0.0")
        }
    }
}
```

**Plain-JVM or Android consumers** (non-KMP Gradle modules, or anything resolving a single JVM
jar):

```kotlin
// build.gradle.kts
dependencies {
    implementation("org.zecdev:zip321-jvm:2.0.0")
}
```

> **Kotlin-only amount API.** `NonNegativeAmount`, the v2 amount type, wraps an unsigned `ULong`
> (matching the `u64`-backed `Zatoshis` of the librustzcash reference and the `UInt64`-backed
> amount type of the companion Swift library, so a ZIP-321 amount means exactly the same thing in
> all three). Kotlin's unsigned types are name-mangled on the JVM, so `NonNegativeAmount.value`,
> `NonNegativeAmount.zatoshi(ULong)` and `MAX_MONEY` are **not callable from plain Java** under
> their Kotlin names in `zip321-jvm`. Kotlin consumers, including Android, are unaffected. From
> Java, go through the string API — `NonNegativeAmount.zec("1.2345")` and `decimalString()` — or
> add a thin Kotlin shim. This is a deliberate, documented v2 break.

## Quick start

The snippets below are copied verbatim from this library's canonical KDoc samples — the
[`paymentRequest`](lib/src/commonMain/kotlin/org/zecdev/zip321/PaymentRequestDsl.kt) DSL entry
point and [`Payment.Builder`](lib/src/commonMain/kotlin/org/zecdev/zip321/model/Payment.kt) — so
they can never silently drift from the real API; `scripts/check-readme-snippets.sh` checks that in
CI. Each scenario is mirrored, concept-for-concept, in the
[companion Swift library](https://github.com/zecdev/zcash-swift-payment-uri)'s own README. See the
[Dokka API reference](lib/Module.md) (`./gradlew :lib:dokkaGenerate`) for full details, including
how to construct the `recipient`/`sapling`/`alice`/`bob` addresses used below (via the throwing
`RecipientAddress(value, network, validating)` constructor).

### 1. An amount + memo payment, via the explicit `Payment.Builder`

Fallible inputs (`amount(zec = ...)`, `memo(utf8 = ...)`) are validated lazily, at `build()`:

```kotlin
// (b) an amount + memo payment
val payment = Payment.Builder(recipient = sapling)
    .amount(zec = "1.2345")
    .memo(utf8 = "Thanks!")
    .message("Invoice #42")
    .build()
    .getOrThrow()
```

### 2. A single address, a multi-payment request, and parsing — via the `paymentRequest` DSL

`paymentRequest { }` is the idiomatic Kotlin equivalent of the Swift library's `@resultBuilder`
entry point. It is a thin layer over `Payment.Builder` / `PaymentRequest.Builder`: the block
evaluates to a `Result<PaymentRequest>`, and the first construction error (in source order) wins.
`ZIP321.parse(uri, expecting, validator, maxInputBytes)` is the total parsing entry point: it always
returns a `Result`, never throws. `validator` is **required**: this library implements the ZIP-321
URI grammar and delegates every question about recipient addresses to the caller (see
[Security](#security) below):

```kotlin
// (a) a simple single-address request
val simple = paymentRequest {
    payment(recipient)
}.getOrThrow()

// (b) an amount + memo payment
val withMemo = paymentRequest {
    payment(sapling) {
        amount(zec = "1.2345")
        memo(utf8 = "Thanks!")
        message("Invoice #42")
    }
}.getOrThrow()

// (c) a multi-payment request
val multi = paymentRequest {
    payment(alice) { amount(zec = "123.456") }
    payment(bob) {
        amount(zec = "0.789")
        memo(utf8 = "hi bob")
    }
}.getOrThrow()

// (d) parsing, with the wallet's own address support as the validator
val validator = AddressValidator { address ->
    val parsed = walletSdk.parseAddress(address) ?: return@AddressValidator null
    AddressDescriptor(
        network = if (parsed.isTestnet) Network.TESTNET else Network.MAINNET,
        isTransparent = parsed.isTransparent,
        canReceiveMemos = parsed.hasShieldedReceiver,
    )
}

val parsed = ZIP321.parse(
    ZIP321.uriString(from = multi),
    expecting = Network.TESTNET,
    validator = validator,
).getOrThrow()
```

`ZIP321.parse` returns `Result<PaymentRequest>`, whose `indexedPayments` preserves every payment's
stored ZIP-321 `paramindex`. Both spellings of a single recipient — `zcash:<addr>` and
`zcash:?address=<addr>` — parse to the **same** `PaymentRequest`: ZIP-321 URI Semantics says they
denote the same request, so which one a URI used is not observable in the parsed model (this is why
v2 has no `ParsedRequest`/`SingleAddress` sealed type). The empty request `zcash:` parses to a
`PaymentRequest` with zero payments.

## Security

- **Address validation is fully delegated, and the delegate is authoritative.** This library
  implements the ZIP-321 URI **grammar** and nothing else. It does not decode, classify or checksum
  Zcash addresses — it ships no Bech32, no Base58Check and no hash. A caller-supplied
  `AddressValidator` is a **required** argument of `ZIP321.parse`: returning `null` rejects the
  address, and the `AddressDescriptor` it returns (`network`, `isTransparent`, `canReceiveMemos`) is
  trusted verbatim and is what drives the ZIP-321 payment rules. This is deliberate: address
  validity must never silently come from a structural approximation baked into a URI parser.
  Implement it by delegating to your Zcash SDK's own address support (librustzcash's `ZcashAddress`
  via the mobile SDKs' FFI/JNI bindings) — the only place that can answer these questions correctly,
  including Unified Address receiver decoding and which address kinds your wallet is willing to pay.
  The single rule applied on top of the validator's verdict is a *comparison*, not a validation: the
  accepted address must belong to the `Network` named by `expecting`.
- **Data-leakage-free errors.** The sealed `ZIP321Error` taxonomy never carries an address, memo
  bytes, an amount, or a raw slice of the input URI — every case carries only parameter names,
  payment indices, counts, or a fixed `StaticReason` value (the one bounded exception is
  `InvalidParamIndex`'s raw index token, capped at a few characters by the ZIP-321 grammar itself).
  It's safe to log a `ZIP321Error` directly.
- **A bounded input size.** `ZIP321.parse` rejects any input longer than `maxInputBytes`
  (`ZIP321.DEFAULT_MAX_INPUT_BYTES`, 8 KiB by default) before any grammar or address-validation
  work begins.
- **No cryptography at all, and no third-party runtime dependencies.** Because validation is
  delegated, `commonMain` contains no hash, no Bech32 and no Base58Check: there is nothing
  crypto-shaped in this library to get wrong or to audit. (The test suite carries its own reference
  Bech32/Base58Check checkers, over a platform SHA-256, purely so the shared conformance corpus's
  checksum-corruption vectors are executable — they are `commonTest` sources and are never
  shipped.) The runtime surface is the Kotlin standard library and nothing else.
- **100% line coverage** (and ≥98% branch coverage) on `commonMain`/`jvmMain`, deterministic
  property-style round-trip tests, conformance against the shared oracle-verified test-vector
  corpus, and bounded Jazzer mutation-based fuzzing (`fuzz-smoke`), all enforced by CI on every
  pull request.

## v1 -> v2 migration

v2.0.0 is a deliberate breaking-change release. Short version:

| v1 | v2 |
| --- | --- |
| `NonNegativeAmount` (class; lenient `String`/`BigDecimal` constructors, checked `Long`) | `NonNegativeAmount` (**same name, entirely new type**: `@JvmInline value class` over an unsigned `ULong`, with `Result` factories `zec(String)` / `zatoshi(ULong)` and strict ZIP-321 grammar). There is deliberately no alias for the v1 class — it was renamed away and then deleted, so every v1 call site must be rewritten. `value` is now a `ULong`, which is name-mangled for plain-Java callers (see [Install](#install)) |
| `ParserContext` (network **and** built-in address validator) | `Network` (the network only) + a **required**, caller-supplied `AddressValidator` returning `AddressDescriptor?` |
| `RecipientAddress(value, context, validating)` (throwing; `RecipientAddressError`) | `RecipientAddress(value, descriptor)` for an already-validated address, or `RecipientAddress.create(value, validator): RecipientAddress?` |
| `ZIP321.ParserResult` / `ParsedRequest` (`SingleAddress` vs `Request`) | **removed** — `parse` returns `Result<PaymentRequest>`; both single-recipient spellings produce the same request |
| `Payment.otherParams: List<OtherParam>?` | `Payment.otherParams: List<OtherParam>` (never null; duplicate names rejected at construction with `DuplicateParameter`) |
| `ZIP321.Errors` (public) | `ZIP321Error` (sealed, data-leakage-free) |
| throwing `ZIP321.request(uriString, context, validatingRecipients)` | `ZIP321.parse(uri, expecting, validator, maxInputBytes) -> Result<PaymentRequest>` (the throwing parse shim is **removed**; `request(...)` now names only the rendering overloads) |
| throwing `Payment(...)` constructor | `Payment.create(...) -> Result<Payment>`, or `Payment.Builder` |
| `OtherParam(key: ParamNameString, value: QcharString?)` | `OtherParam(name: String, value: String?)`, constructed via `OtherParam.create(name, value) -> Result<OtherParam>` |
| `ZIP321.maxPaymentsAllowed = 2109` (v1 remnant, wrongly rejected valid `paramindex`s) | `PaymentRequest.MAX_PAYMENT_COUNT = 9999` (the ZIP-321 `paramindex` grammar's actual bound) |
| **The v1 `NonNegativeAmount.zatoshiToZEC`'s 8-significant-digit rendering bug — large amounts like `20999999.99999999` ZEC were silently corrupted to `21000000` on re-render** | Fixed: `NonNegativeAmount.decimalString()` renders byte-exact (see [Security](#security) and `CHANGELOG.md`) |

### The three changes that will actually break your build

1. **`ParserContext` -> `Network` + a required `AddressValidator`.** Every `ParserContext.MAINNET`
   becomes `Network.MAINNET`, and every parse call gains a validator argument. There is no
   "default" or "built-in" validator to fall back on: this library performs no address validation
   at all, so `parse` cannot be called without one. Implement `AddressValidator` on top of your
   Zcash SDK's address support and return an `AddressDescriptor(network, isTransparent,
   canReceiveMemos)`; return `null` to reject. Anywhere v1 passed a `validatingRecipients: ((String)
   -> Boolean)?` closure, that closure's *decision* now lives inside the validator — it is no longer
   an extra filter on top of a built-in check, it IS the check.

   ```kotlin
   // v1
   ZIP321.request(uri, ParserContext.MAINNET, validatingRecipients = { sdk.isValid(it) })

   // v2
   ZIP321.parse(uri, expecting = Network.MAINNET, validator = myValidator).getOrThrow()
   ```

2. **`ParsedRequest` is gone; `parse` returns `Result<PaymentRequest>`.** Every
   `when (parsed) { is SingleAddress -> …; is Request -> … }` collapses to just using the
   `PaymentRequest`. A bare `zcash:<addr>` now arrives as an ordinary one-payment request, equal to
   what `zcash:?address=<addr>` produces.

   ```kotlin
   // v1
   when (val parsed = ZIP321.parse(uri, ctx).getOrThrow()) {
       is ParsedRequest.SingleAddress -> pay(parsed.recipient)
       is ParsedRequest.Request -> pay(parsed.request.payments)
   }

   // v2
   pay(ZIP321.parse(uri, Network.MAINNET, validator).getOrThrow().payments)
   ```

3. **`Payment.otherParams` is never null.** `payment.otherParams?.forEach { … }` becomes
   `payment.otherParams.forEach { … }`, and `otherParams = null` becomes `otherParams =
   emptyList()` (or is simply omitted — `Payment.create` defaults it). Passing a list with a
   repeated name now fails with `DuplicateParameter` instead of constructing a `Payment` that
   renders to a URI the parser would reject.

**Java-interop note (unchanged from the amount rework):** `NonNegativeAmount` is backed by a
`ULong`, and Kotlin name-mangles every member that takes or returns an unsigned type. Those members
are not callable from plain Java under their Kotlin names in the `zip321-jvm` artifact. Kotlin
consumers (including Android) are unaffected; Java callers should go through `zec(String)` /
`decimalString()`, or add a thin Kotlin shim.

See `CHANGELOG.md`'s `[2.0.0]` entry for the complete list of breaking changes.

## Development

Clone with the test-vector submodule:

```sh
git clone --recurse-submodules git@github.com:zecdev/zcash-kotlin-payment-uri.git
# or, if already cloned:
git submodule update --init --recursive
```

`test-vectors/` tracks [zecdev/zcash-zip321-test-vectors](https://github.com/zecdev/zcash-zip321-test-vectors)
(the URL goes live in `.gitmodules` once that repository is published).

Run the JVM test suite, coverage gate, and lint checks:

```sh
./gradlew jvmTest koverVerify ktlintCheck detekt
```

Run the iOS simulator test suite (requires a full Xcode install):

```sh
DEVELOPER_DIR=/Applications/Xcode.app/Contents/Developer ./gradlew iosSimulatorArm64Test
```

Generate the API reference (fails the build on any undocumented public symbol or broken doc
link):

```sh
./gradlew :lib:dokkaGenerate
```

Run the bounded Jazzer fuzz smoke test locally (45s; see `ZIP321FuzzTest`):

```sh
JAZZER_FUZZ=1 ./gradlew :lib:jvmTest --tests "org.zecdev.zip321.ZIP321FuzzTest"
```

All of the above, plus a check that this README's Quick start snippets still match their KDoc
source (`scripts/check-readme-snippets.sh`), run as required checks in
`.github/workflows/ci.yml` on every pull request.

## License
This project is under the MIT License. See [LICENSE](LICENSE) for more details.
