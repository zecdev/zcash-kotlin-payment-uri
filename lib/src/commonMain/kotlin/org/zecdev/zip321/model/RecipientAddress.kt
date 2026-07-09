package org.zecdev.zip321.model

import org.zecdev.zip321.AddressDescriptor
import org.zecdev.zip321.AddressValidator
import org.zecdev.zip321.Network

/** A raw `(name, value)` query-parameter pair, as it appears in a ZIP-321 URI's query string. */
typealias RequestParams = Pair<String, String>

/**
 * A Zcash recipient address that some [AddressValidator] has accepted, carried
 * together with what that validator said about it.
 *
 * A [RecipientAddress] is an opaque string plus a [descriptor]. This library
 * never inspects [value] — it does not decode, re-check or classify the address
 * in any way. Every capability question it needs to answer while applying the
 * [ZIP-321](https://zips.z.cash/zip-0321) payment rules is read off the
 * descriptor the validator produced.
 *
 * The primary constructor wraps an address a caller has ALREADY validated,
 * together with its description. Use it when the address came from somewhere
 * other than a URI — a wallet's own address book, a QR scan the wallet has
 * already resolved, a test fixture. The descriptor is trusted as-is. To
 * validate a raw string instead, use [RecipientAddress.create].
 *
 * @property value the string-encoded address, verbatim as the validator saw it.
 * @property descriptor what the [AddressValidator] reported about [value].
 */
data class RecipientAddress(
    val value: String,
    val descriptor: AddressDescriptor,
) {
    /**
     * The consensus network this address belongs to, as reported by the
     * validator that accepted it.
     */
    val network: Network get() = descriptor.network

    /**
     * Whether funds sent here land in the transparent pool. Read straight off
     * the validator's descriptor.
     */
    internal val isTransparent: Boolean get() = descriptor.isTransparent

    /**
     * Whether this recipient can receive a ZIP-302 memo. Read straight off the
     * validator's descriptor.
     */
    internal val canReceiveMemos: Boolean get() = descriptor.canReceiveMemos

    /** Namespace for validated [RecipientAddress] construction. */
    companion object {
        /**
         * Validates [value] with [validator] and, if accepted, wraps it.
         *
         * @param value the string-encoded address.
         * @param validator the authority on this address. Returning `null` from
         * [AddressValidator.validate] rejects the address.
         * @return the wrapped address, or `null` when the validator rejects
         * [value].
         */
        fun create(
            value: String,
            validator: AddressValidator,
        ): RecipientAddress? = validator.validate(value)?.let { RecipientAddress(value, it) }
    }
}
