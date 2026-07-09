package org.zecdev.zip321.model

import org.zecdev.zip321.ZIP321Error

/**
 * A payment paired with its ZIP-321 `paramindex` (index `0` denotes the empty paramindex).
 *
 * @param index the `paramindex` (`0` denotes the empty paramindex).
 * @param payment the payment stored at [index].
 */
data class IndexedPayment(val index: UInt, val payment: Payment)

/**
 * A [ZIP-321](https://zips.z.cash/zip-0321) transaction request: an ordered collection of payments,
 * each addressed by its `paramindex`.
 *
 * Unlike v1, `PaymentRequest` **retains ZIP-321 paramindices**. Indices need not be contiguous or
 * start at zero (see ZIP-321 "URI Semantics"), so a request whose only payment sits at index 5
 * preserves that 5 in [indexedPayments]. An empty request (zero payments) is valid.
 */
class PaymentRequest internal constructor(
    private val paymentsByIndex: Map<UInt, Payment>,
) {
    /** The payments of this request, ordered by ascending `paramindex`. */
    val payments: List<Payment>
        get() = paymentsByIndex.keys.sorted().map { paymentsByIndex.getValue(it) }

    /**
     * The payments of this request paired with their ZIP-321 `paramindex`, ordered by ascending
     * index.
     */
    val indexedPayments: List<IndexedPayment>
        get() = paymentsByIndex.keys.sorted().map { IndexedPayment(it, paymentsByIndex.getValue(it)) }

    /** Namespace for validated [PaymentRequest] construction and the [MAX_PAYMENT_COUNT] bound. */
    companion object {
        /**
         * The maximum number of payments a single request may contain; ZIP-321 `paramindex` values
         * are limited to four digits.
         */
        const val MAX_PAYMENT_COUNT: UInt = 9999u

        /**
         * Create a Payment Request from a sequence of payments, assigning sequential paramindices
         * `0, 1, 2, ...` in order.
         *
         * @param payments a sequence of [Payment] structs (may be empty).
         * @throws ZIP321Error.TooManyPayments if more than [MAX_PAYMENT_COUNT] payments are provided.
         */
        @Throws(ZIP321Error::class)
        operator fun invoke(payments: List<Payment>): PaymentRequest {
            if (payments.size.toUInt() > MAX_PAYMENT_COUNT) {
                throw ZIP321Error.TooManyPayments(payments.size.toUInt())
            }

            val byIndex = LinkedHashMap<UInt, Payment>(payments.size)
            payments.forEachIndexed { offset, payment ->
                byIndex[offset.toUInt()] = payment
            }
            return PaymentRequest(byIndex)
        }

        /**
         * Create a Payment Request from payments paired with explicit ZIP-321 paramindices.
         *
         * @param indexedPayments `(index, payment)` pairs. Indices must be unique and each
         * `<= 9999`.
         * @throws ZIP321Error.DuplicateParameter if an index repeats.
         * @throws ZIP321Error.TooManyPayments if any index exceeds [MAX_PAYMENT_COUNT].
         */
        @Throws(ZIP321Error::class)
        fun fromIndexedPayments(indexedPayments: List<IndexedPayment>): PaymentRequest {
            val byIndex = LinkedHashMap<UInt, Payment>(indexedPayments.size)

            for (pair in indexedPayments) {
                if (pair.index > MAX_PAYMENT_COUNT) {
                    throw ZIP321Error.TooManyPayments(pair.index)
                }
                if (byIndex.containsKey(pair.index)) {
                    throw ZIP321Error.DuplicateParameter("address", if (pair.index == 0u) null else pair.index)
                }
                byIndex[pair.index] = pair.payment
            }

            return PaymentRequest(byIndex)
        }
    }

    /**
     * A fluent builder for a [PaymentRequest].
     *
     * Mirrors the cross-language v2 construction contract shared with the Swift library
     * (`PaymentRequest.Builder` there as well). [add] assigns sequential paramindices `0, 1, 2,
     * …` in call order; the `add(payment, at)` overload pins a payment to an explicit
     * `paramindex`. Validation is deferred to [build]: a repeated index fails with
     * [ZIP321Error.DuplicateParameter] and an index above [MAX_PAYMENT_COUNT] fails with
     * [ZIP321Error.TooManyPayments].
     *
     * ```kotlin
     * // (c) a multi-payment request
     * val request = PaymentRequest.Builder()
     *     .add(alicePayment)          // paramindex 0
     *     .add(bobPayment)            // paramindex 1
     *     .add(carolPayment, at = 7u) // paramindex 7
     *     .build()
     *     .getOrThrow()
     * ```
     */
    class Builder() {
        private val indexed = mutableListOf<IndexedPayment>()
        private var autoIndex: UInt = 0u

        /** Seeds the builder with [payments] auto-indexed sequentially from `0`. */
        constructor(payments: List<Payment>) : this() {
            payments.forEach { add(it) }
        }

        /** Adds a payment at the next sequential auto-assigned `paramindex`. */
        fun add(payment: Payment): Builder {
            indexed.add(IndexedPayment(autoIndex, payment))
            autoIndex += 1u
            return this
        }

        /** Adds a payment pinned to an explicit `paramindex` [at]. */
        fun add(
            payment: Payment,
            at: UInt,
        ): Builder {
            indexed.add(IndexedPayment(at, payment))
            return this
        }

        /**
         * Builds the [PaymentRequest], validating index uniqueness
         * ([ZIP321Error.DuplicateParameter]) and the [MAX_PAYMENT_COUNT] cap
         * ([ZIP321Error.TooManyPayments]).
         */
        fun build(): Result<PaymentRequest> = runCatching { fromIndexedPayments(indexed) }
    }

    /** Two [PaymentRequest]s are equal when they hold the same payments at the same indices. */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PaymentRequest) return false
        return paymentsByIndex == other.paymentsByIndex
    }

    /** Consistent with [equals]: derived from the index-to-payment mapping. */
    override fun hashCode(): Int = paymentsByIndex.hashCode()

    /** A debug string listing [indexedPayments]. */
    override fun toString(): String = "PaymentRequest(indexedPayments=$indexedPayments)"
}
