# Module zip321

A concise, zero-runtime-dependency Kotlin Multiplatform implementation of
[ZIP-321](https://zips.z.cash/zip-0321), the Zcash payment-request URI standard, for the `jvm`, `iosArm64`, and
`iosSimulatorArm64` targets.

## Getting started

See [org.zecdev.zip321.paymentRequest] for the four canonical usage scenarios (each written as a runnable KDoc
sample on that entry point, so this overview links to them rather than duplicating the text): a single-address
request with no amount, an amount + memo + message request, a multi-recipient request built with the fluent
[org.zecdev.zip321.model.Payment.Builder] / [org.zecdev.zip321.model.PaymentRequest.Builder] chains or the
[org.zecdev.zip321.paymentRequest] DSL, and parsing a URI string with an injected address-validation delegate via
[org.zecdev.zip321.ZIP321.parse].

## Security

- **Address validation is fully DELEGATED, and the delegate is AUTHORITATIVE.** This library implements the
  ZIP-321 URI **grammar** and nothing else: it does not decode, classify or checksum Zcash addresses, and it
  ships no Bech32, no Base58Check and no hash. A caller-supplied [org.zecdev.zip321.AddressValidator] is a
  REQUIRED argument of [org.zecdev.zip321.ZIP321.parse]; returning `null` rejects the address, and the
  [org.zecdev.zip321.AddressDescriptor] it returns is trusted verbatim — its
  [org.zecdev.zip321.AddressDescriptor.network], [org.zecdev.zip321.AddressDescriptor.isTransparent] and
  [org.zecdev.zip321.AddressDescriptor.canReceiveMemos] drive the ZIP-321 payment rules with no second opinion.
  This is deliberate: address validity must never silently come from a structural approximation baked into a
  URI parser. Wallets should implement it by delegating to their Zcash SDK's own address support (librustzcash's
  `ZcashAddress` via the mobile SDKs' FFI/JNI bindings), which is the only place that can answer these questions
  correctly — including Unified Address receiver decoding and which address kinds the wallet is willing to pay.
  The single rule the library applies on top of the validator's verdict is a COMPARISON, not a validation: the
  accepted address must belong to the [org.zecdev.zip321.Network] the request is being parsed for.
- **Errors never leak sensitive data.** Every [org.zecdev.zip321.ZIP321Error] payload carries only parameter
  names, indices, counts, or fixed reason codes — never addresses, memo contents, amounts, or raw URI slices. The
  sole bounded exception is [org.zecdev.zip321.ZIP321Error.InvalidParamIndex], whose raw index token is at most 5
  characters by the ZIP-321 `paramindex` grammar.
- **Input size is bounded before any parsing work happens.** [org.zecdev.zip321.ZIP321.parse] rejects input
  larger than `maxInputBytes` (default [org.zecdev.zip321.ZIP321.DEFAULT_MAX_INPUT_BYTES], 8 KiB).
