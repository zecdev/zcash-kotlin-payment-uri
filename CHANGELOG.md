# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

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
