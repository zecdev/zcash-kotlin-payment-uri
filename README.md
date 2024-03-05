# zcash-kotlin-payment-uri
Prototype of Zcash Payment URIs defined on ZIP-321 for Kotlin


## What are Zcash Payment URIs?

Quote from [ZIP-321](https://zips.z.cash/zip-0321)
> [..] a standard format for payment request URIs. Wallets that recognize this format enable users to construct transactions simply by clicking links on webpages or scanning QR codes.

Payment URIs let users express their payment intents in the form of "_standardized_" URIs that
can be parsed by several applications across the ecosystem


**Example**
`zcash:?address=tmEZhbWHTpdKMw5it8YDspUXSMGQyFwovpU&amount=123.456&address.1=ztestsapling10yy2ex5dcqkclhc7z7yrnjq2z6feyjad56ptwlfgmy77dmaqqrl9gyhprdx59qgmsnyfska2kez&amount.1=0.789&memo.1=VGhpcyBpcyBhIHVuaWNvZGUgbWVtbyDinKjwn6aE8J-PhvCfjok`

## Project Roadmap

### 1. ZIP-321 construction ✅

- Provide an API that lets users build a Payment Request URI from its bare-bone components. ✅
- There's a comprehensive set of tests that exercises the logic above according with what is defined on [ZIP-321](https://zips.z.cash/zip-0321) ✅
- (Optional) Mechanism for callers to provide logic for validating of Zcash addresses ✅

### 2. ZIP-321 parsing ✅
- Given a valid ZIP-321 Payment Request String, create a PaymentRequest Object ✅
- The result of the point above would have to be equivalent as if the given URI was generated programmatically with the same inputs using the API of `1.` of the roadmap
- The parser API uses `2.` of the roadmap to validate the provided ZIP-321 Request
- The parser checks the integrity of the provided URI as defined on [ZIP-321](https://zips.z.cash/zip-0321)
- There's a comprehensive set of Unit Tests that exercise the point above.

### 3. ZIP-321 built-in validation
- Built-in mechanism to validate the provided input. This will entail leveraging some sort of FFI calls to [`zcash_address` crate](https://crates.io/crates/zcash_address/0.1.0)

## Getting Started

### Requesting a payment to a Zcash address
Payments requests that do not specify any other information than recipient address.

````kotlin
 val recipient = RecipientAddress("ztestsapling10yy2ex5dcqkclhc7z7yrnjq2z6feyjad56ptwlfgmy77dmaqqrl9gyhprdx59qgmsnyfska2kez")
        
ZIP321.request(recipient)
````

### Requesting a payment specifying amount and other parameters.
Desired Payment URI
`zcash:ztestsapling10yy2ex5dcqkclhc7z7yrnjq2z6feyjad56ptwlfgmy77dmaqqrl9gyhprdx59qgmsnyfska2kez?amount=1&memo=VGhpcyBpcyBhIHNpbXBsZSBtZW1vLg&message=Thank%20you%20for%20your%20purchase`

````Kotlin
 val recipient =
            RecipientAddress("ztestsapling10yy2ex5dcqkclhc7z7yrnjq2z6feyjad56ptwlfgmy77dmaqqrl9gyhprdx59qgmsnyfska2kez")
val payment = Payment(
    recipientAddress = recipient,
    amount = Amount(BigDecimal(1)),
    memo = MemoBytes("This is a simple memo."),
    label = null,
    message = "Thank you for your purchase",
    otherParams = null
)

val paymentRequest = PaymentRequest(payments = listOf(payment))

ZIP321.uriString(
    paymentRequest,
    ZIP321.FormattingOptions.UseEmptyParamIndex(omitAddressLabel = true)
)

ZIP321.request(
    payment,
    ZIP321.FormattingOptions.UseEmptyParamIndex(omitAddressLabel = true)
) 
````


### Requesting Payments to multiple recipients
Desired Payment URI:
`zcash:?address=tmEZhbWHTpdKMw5it8YDspUXSMGQyFwovpU&amount=123.456&address.1=ztestsapling10yy2ex5dcqkclhc7z7yrnjq2z6feyjad56ptwlfgmy77dmaqqrl9gyhprdx59qgmsnyfska2kez&amount.1=0.789&memo.1=VGhpcyBpcyBhIHVuaWNvZGUgbWVtbyDinKjwn6aE8J-PhvCfjok`

This payment Request is using `paramlabel`s with empty `paramindex` and number indices. This Request String generation API allows callers to specify their format of choice for parameters and indices.

````kotlin
val recipient0 = RecipientAddress("tmEZhbWHTpdKMw5it8YDspUXSMGQyFwovpU")
val payment0 = Payment(
    recipientAddress = recipient0,
    amount = Amount.create(BigDecimal(123.456)),
    memo = null,
    label = null,
    message = null,
    otherParams = null
)

val recipient1 =
    RecipientAddress("ztestsapling10yy2ex5dcqkclhc7z7yrnjq2z6feyjad56ptwlfgmy77dmaqqrl9gyhprdx59qgmsnyfska2kez")
val payment1 = Payment(
    recipientAddress = recipient1,
    amount = Amount.create(BigDecimal(0.789)),
    memo = MemoBytes("This is a unicode memo ✨🦄🏆🎉"),
    label = null,
    message = null,
    otherParams = null
)

val paymentRequest = PaymentRequest(payments = listOf(payment0, payment1))

ZIP321.uriString(paymentRequest, ZIP321.FormattingOptions.UseEmptyParamIndex(omitAddressLabel = false))
````

### Parsing a ZIP-321 URI String 

```Kotlin
val validURI =
            "zcash:?address=tmEZhbWHTpdKMw5it8YDspUXSMGQyFwovpU&amount=123.456&address.1=ztestsapling10yy2ex5dcqkclhc7z7yrnjq2z6feyjad56ptwlfgmy77dmaqqrl9gyhprdx59qgmsnyfska2kez&amount.1=0.789&memo.1=VGhpcyBpcyBhIHVuaWNvZGUgbWVtbyDinKjwn6aE8J-PhvCfjok"

 val paymentRequest = ZIP321.request(validURI, null)
```

### Providing Address validation

Address validation is not provided by the library. The only thing it's bundled is a rudimentary way to
detect possible transparent addresses in recipients to actually throw an error to ZIP-321 specs.

if you have a zcash wallet app, you most likely have methods to validate that a given string is a 
valid Zcash address. In that case you should call the parsing method with a lambda 

```
val paymentRequest = ZIP321.request(validURI, { ZcashSDK.isValidAddress(it) })
```

Errors will be thrown if such validation fails.

# License
This project is under MIT License. See [LICENSE.md](LICENSE.md) for more details.
