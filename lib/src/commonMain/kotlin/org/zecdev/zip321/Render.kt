package org.zecdev.zip321

import org.zecdev.zip321.extensions.qcharEncoded
import org.zecdev.zip321.model.MemoBytes
import org.zecdev.zip321.model.NonNegativeAmount
import org.zecdev.zip321.model.OtherParam
import org.zecdev.zip321.model.Payment
import org.zecdev.zip321.model.PaymentRequest
import org.zecdev.zip321.model.RecipientAddress

/**
 * The reserved ZIP-321 query-parameter names.
 */
enum class ParamName(val value: String) {
    ADDRESS("address"),
    AMOUNT("amount"),
    LABEL("label"),
    MEMO("memo"),
    MESSAGE("message"),
}

/**
 * The canonical ZIP-321 renderer.
 *
 * This mirrors the librustzcash `zip321` reference `mod render` and
 * `TransactionRequest::to_uri` (see `components/zip321/src/lib.rs`, lines ~380-589). Rendering is
 * driven by [PaymentRequest.indexedPayments], so the ACTUAL stored `paramindex` of every payment
 * is preserved (an empty paramindex — stored index `0` — renders with no `.n` suffix; a payment at
 * index `5` renders `address.5=…`).
 *
 * Suppression note: one small renderer per parameter kind, mirroring the reference `mod render`
 * (and the Swift blueprint) one function per grammar production.
 */
@Suppress("TooManyFunctions")
internal object Render {
    /**
     * Renders the `paramindex` suffix for a query key.
     *
     * Matches the reference `render::param_index`: only a POSITIVE index produces a `.n` suffix;
     * both `null` and the empty paramindex (`0`) render the empty string.
     */
    fun parameterIndex(idx: UInt?): String = if (idx != null && idx > 0u) ".$idx" else ""

    /**
     * Renders a `name[.index]=value` query parameter, qchar-encoding the provided (decoded) value.
     */
    fun parameter(
        name: String,
        decodedValue: String,
        index: UInt?,
    ): String = "$name${parameterIndex(index)}=${decodedValue.qcharEncoded()}"

    fun parameter(
        amount: NonNegativeAmount,
        index: UInt?,
    ): String = "${ParamName.AMOUNT.value}${parameterIndex(index)}=${amount.decimalString()}"

    fun parameter(
        memo: MemoBytes,
        index: UInt?,
    ): String = "${ParamName.MEMO.value}${parameterIndex(index)}=${memo.toBase64URL()}"

    fun parameter(
        address: RecipientAddress,
        index: UInt?,
        omittingAddressLabel: Boolean = false,
    ): String =
        if ((index == null || index == 0u) && omittingAddressLabel) {
            address.value
        } else {
            "${ParamName.ADDRESS.value}${parameterIndex(index)}=${address.value}"
        }

    fun parameterLabel(
        label: String,
        index: UInt?,
    ): String = parameter(ParamName.LABEL.value, label, index)

    fun parameterMessage(
        message: String,
        index: UInt?,
    ): String = parameter(ParamName.MESSAGE.value, message, index)

    /**
     * Renders an `otherparam` per the ZIP-321 grammar
     * `otherparam = paramname [ paramindex ] [ "=" *qchar ]`, matching the reference
     * `render::str_param`. The `=` separator is emitted whenever the parameter carries a value
     * (including an empty `""` value → `name=`); a value-less parameter renders as a bare `name`
     * with no `=`.
     */
    fun parameter(
        other: OtherParam,
        index: UInt?,
    ): String {
        val prefix = "${other.name}${parameterIndex(index)}"
        return other.value?.let { "$prefix=${it.qcharEncoded()}" } ?: prefix
    }

    /**
     * The ordered non-address query parameters for a payment, in the canonical ZIP-321 order:
     * `amount`, `memo`, `label`, `message`, then `otherParams` in stored order. Mirrors the
     * reference `payment_params` (`lib.rs` ~381-415).
     */
    fun paymentParams(
        payment: Payment,
        index: UInt?,
    ): List<String> {
        val params = ArrayList<String>()

        payment.amount?.let { params.add(parameter(it, index)) }
        payment.memo?.let { params.add(parameter(it, index)) }
        payment.label?.let { params.add(parameterLabel(it, index)) }
        payment.message?.let { params.add(parameterMessage(it, index)) }
        payment.otherParams.forEach { params.add(parameter(it, index)) }

        return params
    }

    /**
     * Renders a single [Payment] as a query-parameter fragment. This is not aware of the
     * surrounding request; forming a valid ZIP-321 URI from many payments is the caller's
     * ([request]) responsibility.
     *
     * The parameter order is: address, amount, memo, label, message, then otherParams in stored
     * order.
     *
     * When [index] is `null`/`0` and [omittingAddressLabel] is `true`, the fragment uses the
     * leading-address form (`<addr>?amount=…`), i.e. the bare address followed by a `?` and the
     * query params. Otherwise the address is rendered as an `address[.n]=…` query key alongside
     * the rest.
     *
     * @param payment a valid [Payment].
     * @param index the `paramindex` for this payment (`null`/`0` for the empty index).
     * @param omittingAddressLabel when [index] is `null`/`0` and this is `true`, renders the
     * address without the leading `address=` label; ignored when `index > 0`.
     */
    fun payment(
        payment: Payment,
        index: UInt?,
        omittingAddressLabel: Boolean = false,
    ): String {
        val params = paymentParams(payment, index)

        if ((index == null || index == 0u) && omittingAddressLabel) {
            // Leading-address form: `<addr>[?param&param…]`. No trailing `?` when there are no
            // query params (matching the reference).
            val query = if (params.isEmpty()) "" else "?" + params.joinToString("&")
            return payment.recipientAddress.value + query
        }

        val addressParam = parameter(payment.recipientAddress, index, omittingAddressLabel = false)
        return (listOf(addressParam) + params).joinToString("&")
    }

    /**
     * Renders a whole [PaymentRequest] to its `zcash:` URI string according to
     * [formattingOptions].
     *
     * - [ZIP321.FormattingOptions.UseEmptyParamIndex] renders each payment at its ACTUAL stored
     *   `paramindex` (empty suffix for index `0`, `.n` otherwise). When `omitAddressLabel` is
     *   `true` AND the request holds exactly one payment AND that payment sits at index `0`, the
     *   canonical single-payment leading-address form is used (`zcash:<addr>?amount=…`); every
     *   other shape uses `zcash:?address[.n]=…&…`.
     * - [ZIP321.FormattingOptions.EnumerateAllPayments] is a NORMALIZATION mode: it discards the
     *   stored indices and re-numbers payments SEQUENTIALLY from `1` (`address.1`, `address.2`,
     *   …), always with explicit address labels under `zcash:?`.
     *
     * The empty request renders as the bare `zcash:` scheme in either mode.
     */
    fun request(
        paymentRequest: PaymentRequest,
        formattingOptions: ZIP321.FormattingOptions,
    ): String {
        val indexed = paymentRequest.indexedPayments

        if (indexed.isEmpty()) {
            return "zcash:"
        }

        return when (formattingOptions) {
            is ZIP321.FormattingOptions.EnumerateAllPayments -> {
                val segments =
                    indexed.mapIndexed { offset, pair ->
                        payment(pair.payment, (offset + 1).toUInt(), omittingAddressLabel = false)
                    }
                "zcash:?" + segments.joinToString("&")
            }

            is ZIP321.FormattingOptions.UseEmptyParamIndex -> {
                // Reference `to_uri` single-payment special case: exactly one payment at the
                // empty paramindex, rendered as the leading-address form when label omission is
                // requested.
                if (formattingOptions.omitAddressLabel && indexed.size == 1 && indexed[0].index == 0u) {
                    "zcash:" + payment(indexed[0].payment, index = null, omittingAddressLabel = true)
                } else {
                    val segments =
                        indexed.map { pair ->
                            payment(pair.payment, pair.index, omittingAddressLabel = false)
                        }
                    "zcash:?" + segments.joinToString("&")
                }
            }
        }
    }
}
