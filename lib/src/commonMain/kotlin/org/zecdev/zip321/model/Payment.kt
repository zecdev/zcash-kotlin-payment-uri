package org.zecdev.zip321.model

import org.zecdev.zip321.ZIP321Error

/**
 * A single payment that will be requested.
 *
 * @property recipientAddress recipient of the payment.
 * @property amount the amount of the payment, as a [NonNegativeAmount] count, or `null` if unspecified.
 * @property memo bytes of the ZIP-302 Memo if present.
 * @property label a human-readable (already decoded) label for this payment.
 * @property message a human-readable (already decoded) message describing this payment.
 * @property otherParams the additional, non-reserved `otherparam` entries of this payment, in the
 * order they appeared (or were added).
 *
 * This is ALWAYS a list: there is NO distinction between "no other params" and "an empty list of
 * other params", because ZIP-321 has no way to spell the difference and the reference
 * implementation does not model one either. An absent list and an empty list would render
 * identically, so representing both would make two distinct [Payment] values with the same URI —
 * breaking the round-trip law. Names are unique within a payment; [create] rejects duplicates.
 */
class Payment internal constructor(
    val recipientAddress: RecipientAddress,
    val amount: NonNegativeAmount?,
    val memo: MemoBytes?,
    val label: String?,
    val message: String?,
    val otherParams: List<OtherParam>,
) {
    fun isSingleAddress(): Boolean {
        return amount == null &&
            memo == null &&
            label == null &&
            message == null &&
            otherParams.isEmpty()
    }

    companion object {
        /**
         * Creates a validated [Payment], enforcing the ZIP-321 structural rules that apply to an
         * individual payment (matching the reference `to_payment`):
         *
         * - a `memo` may not be attached to a recipient that cannot receive memos (a transparent
         *   address) — [ZIP321Error.TransparentMemo];
         * - a zero-valued `amount` may not be sent to a transparent recipient —
         *   [ZIP321Error.ZeroValuedTransparentOutput];
         * - `otherparam` names must be unique within a payment — a repeated name fails with
         *   [ZIP321Error.DuplicateParameter].
         *
         * Both capability questions are answered by the recipient's [AddressDescriptor], i.e. by
         * the validator that accepted the address, never by re-inspecting the address string.
         *
         * Errors are produced index-agnostically (`index = null`); the parser tags them with the
         * concrete payment index via [ZIP321Error.withIndex].
         *
         * @param label a plain (decoded) label, or `null`. Not included in the blockchain.
         * @param message a plain (decoded) message, or `null`. Not included in the blockchain.
         * @return [Result.success] with the [Payment], or [Result.failure] with the [ZIP321Error].
         */
        @Suppress("ReturnCount", "LongParameterList")
        fun create(
            recipientAddress: RecipientAddress,
            amount: NonNegativeAmount?,
            memo: MemoBytes?,
            label: String?,
            message: String?,
            otherParams: List<OtherParam> = emptyList(),
        ): Result<Payment> {
            firstDuplicateName(otherParams)?.let { duplicate ->
                return Result.failure(ZIP321Error.DuplicateParameter(duplicate, null))
            }

            if (memo != null && !recipientAddress.canReceiveMemos) {
                return Result.failure(ZIP321Error.TransparentMemo(null))
            }

            if (amount != null && amount.value == 0uL && recipientAddress.isTransparent) {
                return Result.failure(ZIP321Error.ZeroValuedTransparentOutput(null))
            }

            return Result.success(
                Payment(recipientAddress, amount, memo, label, message, otherParams),
            )
        }

        /**
         * The first `otherparam` name that appears more than once in [params], or `null` when
         * every name is unique.
         */
        internal fun firstDuplicateName(params: List<OtherParam>): String? {
            val seen = mutableSetOf<String>()
            for (param in params) {
                if (!seen.add(param.name)) return param.name
            }
            return null
        }

        /**
         * Deprecated throwing factory kept one migration cycle for source compatibility. Prefer
         * [create], which returns a `Result<Payment>` consistent with v2 totality. Invoking
         * `Payment(...)` resolves here (the primary constructor is internal).
         *
         * @throws ZIP321Error if the payment violates a structural rule (see [create]).
         */
        @Deprecated(
            "Use Payment.create(...) which returns a Result<Payment>.",
            ReplaceWith("Payment.create(recipientAddress, amount, memo, label, message, otherParams)"),
        )
        @Throws(ZIP321Error::class)
        @Suppress("LongParameterList")
        operator fun invoke(
            recipientAddress: RecipientAddress,
            amount: NonNegativeAmount?,
            memo: MemoBytes?,
            label: String?,
            message: String?,
            otherParams: List<OtherParam> = emptyList(),
        ): Payment = create(recipientAddress, amount, memo, label, message, otherParams).getOrThrow()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Payment) return false

        if (recipientAddress != other.recipientAddress) return false
        if (amount != other.amount) return false
        if (memo != other.memo) return false
        if (label != other.label) return false
        if (message != other.message) return false
        if (otherParams != other.otherParams) return false

        return true
    }

    override fun hashCode(): Int {
        var result = recipientAddress.hashCode()
        result = 31 * result + (amount?.hashCode() ?: 0)
        result = 31 * result + (memo?.hashCode() ?: 0)
        result = 31 * result + (label?.hashCode() ?: 0)
        result = 31 * result + (message?.hashCode() ?: 0)
        result = 31 * result + otherParams.hashCode()
        return result
    }

    override fun toString(): String =
        "Payment(recipientAddress=$recipientAddress, amount=$amount, memo=$memo, " +
            "label=$label, message=$message, otherParams=$otherParams)"
}

/**
 * A ZIP-321 `otherparam`:
 * ```
 *   otherparam = paramname [ paramindex ] [ "=" *qchar ]
 * ```
 * Both fields carry plain, already-decoded values: [name] is the `paramname`, and [value] is the
 * percent-decoded `*qchar` value (or `null` when the parameter had no `= value`).
 */
data class OtherParam(val name: String, val value: String?)
