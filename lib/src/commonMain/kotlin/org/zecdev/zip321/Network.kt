package org.zecdev.zip321

/**
 * A Zcash consensus network.
 *
 * A [ZIP-321](https://zips.z.cash/zip-0321) request is always parsed against
 * ONE expected network: every recipient address the URI carries must be an
 * address for that network, or the request is rejected. Which network an
 * address belongs to is decided by the caller-supplied [AddressValidator], not
 * by this library — see [AddressDescriptor.network].
 */
enum class Network {
    /** The Zcash production consensus network. */
    MAINNET,

    /** The public Zcash test network. */
    TESTNET,

    /** A local/private regression-testing network. */
    REGTEST,
}
