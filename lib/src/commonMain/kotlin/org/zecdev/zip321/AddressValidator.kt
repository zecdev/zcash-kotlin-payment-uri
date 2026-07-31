package org.zecdev.zip321

/**
 * The caller-supplied authority on Zcash recipient addresses.
 *
 * This library implements the [ZIP-321](https://zips.z.cash/zip-0321) URI
 * **grammar** and nothing else. It does not know how Zcash addresses are
 * encoded, which encodings exist, or which of them a given deployment considers
 * acceptable. Every one of those questions is answered here, by the caller, and
 * this library treats the answer as final:
 *
 * - a `null` return means the address is invalid and the payment request is
 *   rejected with an invalid-address error;
 * - a non-`null` [AddressDescriptor] is trusted verbatim — its
 *   [AddressDescriptor.network], [AddressDescriptor.isTransparent] and
 *   [AddressDescriptor.canReceiveMemos] drive the ZIP-321 payment rules with no
 *   second opinion from this library.
 *
 * There is no built-in fallback and no "and also" composition: a validator is a
 * REQUIRED argument of [ZIP321.request] precisely so that address validity can
 * never silently come from a structural approximation baked into a URI parser.
 *
 * Wallets should implement this by delegating to their Zcash SDK's own address
 * support (for example librustzcash's `ZcashAddress` via the mobile SDKs'
 * FFI/JNI bindings), which is the only place that can answer these questions
 * correctly — including Unified Address receiver decoding, and which address
 * kinds the wallet is willing to pay.
 *
 * Because this is a `fun interface`, a caller that already has an
 * address-checking function can pass a lambda directly instead of declaring a
 * type:
 *
 * ```kotlin
 * val validator = AddressValidator { address ->
 *     val parsed = sdk.parseAddress(address) ?: return@AddressValidator null
 *     // ZIP-321 forbids Sprout recipients.
 *     if (parsed.isSprout) return@AddressValidator null
 *     AddressDescriptor(
 *         network = if (parsed.isTestnet) Network.TESTNET else Network.MAINNET,
 *         isTransparent = parsed.isTransparent,
 *         canReceiveMemos = parsed.hasShieldedReceiver,
 *     )
 * }
 * ```
 */
fun interface AddressValidator {
    /**
     * Decides whether [address] is a recipient this caller accepts, and
     * describes it.
     *
     * @param address the raw, still-undecoded address string exactly as it
     * appeared in the URI.
     * @return an [AddressDescriptor] when the address is valid and acceptable,
     * or `null` to reject it. The return value is AUTHORITATIVE; this library
     * does not second-guess it.
     */
    fun validate(address: String): AddressDescriptor?
}
