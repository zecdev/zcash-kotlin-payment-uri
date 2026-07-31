This page is copyright ZecDev.org, 2024. It is posted in order to conform to this standard: https://github.com/RD-Crypto-Spec/Responsible-Disclosure/tree/d47a5a3dafa5942c8849a93441745fdd186731e6

# Security Disclosures

## Disclosure Principles

ZecDev's security disclosure process aims to achieve the following goals:
- protecting ZecDev's users and the wider ecosystem
- respecting the work of security researchers
- improving the ongoing health of the Zcash ecosystem

Specifically, we will:
- assume good faith from researchers and ecosystem partners
- operate a no fault process, focusing on the technical issues
- work with security researchers, regardless of how they choose to disclose issues

## Receiving Disclosures

ZecDev.org is committed to working with researchers who submit security vulnerability notifications to us to resolve those issues on an appropriate timeline and perform a coordinated release, giving credit to the reporter if they would like.

Our best contact for security issues is security@zecdev.org.

## Sending Disclosures

In the case where we become aware of security issues affecting other projects that has never affected ZecDev's projects, our intention is to inform those projects of security issues on a best effort basis.

In the case where we fix a security issue in our projects that also affects the following neighboring projects, our intention is to engage in responsible disclosures with them as described in https://github.com/RD-Crypto-Spec/Responsible-Disclosure, subject to the deviations described in the section at the bottom of this document.

## Scope note: address validation is delegated

As of v2.0.0, this library performs **no recipient-address validation of its own**. It implements
the [ZIP-321](https://zips.z.cash/zip-0321) URI **grammar** and nothing else: it does not decode,
classify or checksum Zcash addresses, and it ships no Bech32, no Base58Check and no hash.

Address validity is answered entirely by a caller-supplied `AddressValidator`, which is a
**required** argument of `ZIP321.parse`. Its verdict is AUTHORITATIVE and is never second-guessed:
returning `null` rejects the address, and the returned `AddressDescriptor` (`network`,
`isTransparent`, `canReceiveMemos`) is what drives the ZIP-321 payment rules — whether a `memo` may
accompany a recipient, and whether a zero-valued output to it is permitted.

This is a deliberate security decision, not an omission. A URI parser's structural approximation of
"is this a valid Zcash address" is exactly the kind of check that looks authoritative while being
subtly wrong (it cannot decode Unified Address receivers, and it cannot know which address kinds a
given wallet is willing to pay). Making the validator required means address validity can never
silently come from such an approximation. Integrators SHOULD implement `AddressValidator` by
delegating to their Zcash SDK's own address support — librustzcash's `ZcashAddress` via the mobile
SDKs' FFI/JNI bindings — which is the only place that can answer these questions correctly.

The one rule this library applies on top of the validator's verdict is a COMPARISON, not a
validation: the accepted address must belong to the `Network` the request is being parsed for.

## Deviations from the Standard

The standard describes reporters of vulnerabilities including full details of an issue, in order to reproduce it. This is necessary for instance in the case of an external researcher both demonstrating and proving that there really is a security issue, and that security issue really has the impact that they say it has - allowing the development team to accurately prioritize and resolve the issue.

For the case our assessment determines so, we might decide not to include those details with our reports to partners ahead of coordinated release, so long as we are sure that they are vulnerable.


Below you can find security@zecdev.org PGP pub key.
```
-----BEGIN PGP PUBLIC KEY BLOCK-----

xjMEZvruLhYJKwYBBAHaRw8BAQdAidX5sDkbrVGcRp3RIhhJoXPdsqBM5slk
8H3mgs+EhFXNKXNlY3VyaXR5QHplY2Rldi5vcmcgPHNlY3VyaXR5QHplY2Rl
di5vcmc+wowEEBYKAD4Fgmb67i4ECwkHCAmQ0hYruZ0SM+QDFQgKBBYAAgEC
GQECmwMCHgEWIQRdDkFAkPdo3dHRpRTSFiu5nRIz5AAAcFsBAIpCq9AGvFdc
M9MYKCkstRMrltnhKsdnVs97oegM8HCsAQDTEB3GZn3kJGG1kCa+Wy0C1zZO
FDTB0P3eBBLOr84oAM44BGb67i4SCisGAQQBl1UBBQEBB0C53DLo7aTs/6fC
j4Hvjr7l7993eKZhb6RPqGeWt4xdLwMBCAfCeAQYFgoAKgWCZvruLgmQ0hYr
uZ0SM+QCmwwWIQRdDkFAkPdo3dHRpRTSFiu5nRIz5AAANOYA+QGte85uZHxI
9o29GbPndaoSUo6+3+YS9m1oqzJjmg4tAQD2RvYflmx7vIQirGvfaCwumN3v
DzIvY8Qt3jfH4WJXBw==
=AQmT
-----END PGP PUBLIC KEY BLOCK-----
```

