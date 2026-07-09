# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## Unreleased
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
