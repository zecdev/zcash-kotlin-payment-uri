package org.zecdev.zip321.model

import org.zecdev.zip321.ParamName
import org.zecdev.zip321.ZIP321Error
import org.zecdev.zip321.parser.ParamNameCharacterSet
import org.zecdev.zip321.parser.isAsciiLetter

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
    /** Namespace for validated [Payment] construction ([create]) and the deprecated throwing shim. */
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
         * Both capability questions are answered by the recipient's
         * [org.zecdev.zip321.AddressDescriptor], i.e. by the validator that accepted the address,
         * never by re-inspecting the address string.
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
            otherParams: List<OtherParam>,
        ): Payment = create(recipientAddress, amount, memo, label, message, otherParams).getOrThrow()
    }

    /**
     * A fluent builder for a single [Payment].
     *
     * Mirrors the cross-language v2 construction contract shared with the Swift library
     * (`Payment.Builder` there as well). The recipient is required up front; every other field is
     * optional and chainable. Inputs that can fail to convert ([amount] from a ZEC string, [memo]
     * from a UTF-8 string, [otherParam]) are validated LAZILY: the builder stores the conversion
     * outcome and the first failure surfaces at [build]. When several fields are invalid, the
     * first error wins in a FIXED field order (amount, then memo, then other params, then the
     * [Payment.create] structural rules — e.g. a memo on a transparent recipient surfaces as
     * [ZIP321Error.TransparentMemo]).
     *
     * ```kotlin
     * // (b) an amount + memo payment
     * val payment = Payment.Builder(recipient = sapling)
     *     .amount(zec = "1.2345")
     *     .memo(utf8 = "Thanks!")
     *     .message("Invoice #42")
     *     .build()
     *     .getOrThrow()
     * ```
     *
     * @param recipient the (already validated) recipient address of the payment being built.
     */
    class Builder(private val recipient: RecipientAddress) {
        private var deferredAmount: Result<NonNegativeAmount?> = Result.success(null)
        private var deferredMemo: Result<MemoBytes?> = Result.success(null)
        private var storedLabel: String? = null
        private var storedMessage: String? = null
        private var deferredOtherParams: Result<List<OtherParam>> = Result.success(emptyList())

        /** Sets the payment amount from a [NonNegativeAmount] count. */
        fun amount(amount: NonNegativeAmount): Builder {
            deferredAmount = Result.success(amount)
            return this
        }

        /**
         * Sets the payment amount from a decimal ZEC string (strict ZIP-321 `amountparam`
         * grammar). Invalid strings surface at [build] as [ZIP321Error.AmountInvalid] (or
         * [ZIP321Error.AmountExceededSupply] when the value is above `MAX_MONEY`).
         */
        fun amount(zec: String): Builder {
            deferredAmount =
                NonNegativeAmount.zec(zec).fold(
                    onSuccess = { Result.success(it) },
                    onFailure = { error ->
                        Result.failure(
                            when (error) {
                                is NonNegativeAmount.AmountException.ExceededSupply ->
                                    ZIP321Error.AmountExceededSupply(null)
                                else -> ZIP321Error.AmountInvalid(null)
                            },
                        )
                    },
                )
            return this
        }

        /** Attaches a [MemoBytes] memo. */
        fun memo(memo: MemoBytes): Builder {
            deferredMemo = Result.success(memo)
            return this
        }

        /**
         * Attaches a memo from a UTF-8 string. A string that encodes to more than 512 bytes
         * surfaces at [build] as [ZIP321Error.MemoBytesError].
         */
        fun memo(utf8: String): Builder {
            deferredMemo =
                runCatching { MemoBytes(utf8) }.fold(
                    onSuccess = { Result.success(it) },
                    onFailure = { Result.failure(ZIP321Error.MemoBytesError(null)) },
                )
            return this
        }

        /** Sets the (plain, decoded) label. It is qchar-encoded at render time. */
        fun label(label: String): Builder {
            storedLabel = label
            return this
        }

        /** Sets the (plain, decoded) message. It is qchar-encoded at render time. */
        fun message(message: String): Builder {
            storedMessage = message
            return this
        }

        /**
         * Appends an arbitrary (non-reserved) `otherparam` via [OtherParam.create]. An empty
         * name, a reserved key (including any `req-`-prefixed name), or a name that is not a
         * valid `paramname` surfaces at [build] as
         * [ZIP321Error.ParseError] ([ZIP321Error.StaticReason.INVALID_PARAMETER]).
         *
         * @param name the (plain) parameter name.
         * @param value the (plain, decoded) value, or `null` for a value-less parameter.
         */
        fun otherParam(
            name: String,
            value: String?,
        ): Builder {
            deferredOtherParams =
                deferredOtherParams.mapCatching { existing ->
                    existing + OtherParam.create(name, value).getOrThrow()
                }
            return this
        }

        /**
         * Builds the [Payment], surfacing the first deferred conversion error (amount → memo →
         * other params) and then the structural rules enforced by [Payment.create].
         */
        fun build(): Result<Payment> =
            runCatching {
                // Deferred errors surface here in FIXED field order.
                val amount = deferredAmount.getOrThrow()
                val memo = deferredMemo.getOrThrow()
                val otherParams = deferredOtherParams.getOrThrow()

                create(
                    recipientAddress = recipient,
                    amount = amount,
                    memo = memo,
                    label = storedLabel,
                    message = storedMessage,
                    otherParams = otherParams,
                ).getOrThrow()
            }
    }

    /** Two [Payment]s are equal when every property compares equal. */
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

    /** Consistent with [equals]: combines every property's hash code. */
    override fun hashCode(): Int {
        var result = recipientAddress.hashCode()
        result = 31 * result + (amount?.hashCode() ?: 0)
        result = 31 * result + (memo?.hashCode() ?: 0)
        result = 31 * result + (label?.hashCode() ?: 0)
        result = 31 * result + (message?.hashCode() ?: 0)
        result = 31 * result + otherParams.hashCode()
        return result
    }

    /** A debug string listing every property. */
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
 *
 * Construct instances via [create], which validates [name] against the ZIP-321 grammar and the
 * reserved-name rules; the bare constructor is internal (the parse path constructs instances from
 * already-validated grammar tokens).
 *
 * @param name the plain `paramname`.
 * @param value the plain, percent-decoded value, or `null` when the parameter had no `= value`.
 */
@ConsistentCopyVisibility
data class OtherParam internal constructor(val name: String, val value: String?) {
    /** Namespace for validated [OtherParam] construction. */
    companion object {
        /**
         * Creates a validated [OtherParam] from a plain (decoded) name and optional (decoded)
         * value.
         *
         * The name is rejected with [ZIP321Error.ParseError]
         * ([ZIP321Error.StaticReason.INVALID_PARAMETER]) when it:
         * - is empty;
         * - collides with a reserved query key (`address`, `amount`, `label`, `memo`, `message`)
         *   or carries the `req-` required-parameter prefix (a library cannot mint required
         *   parameters it does not understand — see ZIP-321 "Forward compatibility");
         * - is not a valid `paramname` (`ALPHA *( ALPHA / DIGIT / "+" / "-" )`).
         *
         * @param name the (plain) parameter name.
         * @param value the (plain, decoded) value, or `null` for a value-less parameter.
         * @return [Result.success] with the [OtherParam], or [Result.failure] with the
         * [ZIP321Error].
         */
        fun create(
            name: String,
            value: String?,
        ): Result<OtherParam> =
            if (name.isEmpty() || isReservedName(name) || !isValidParamName(name)) {
                Result.failure(ZIP321Error.ParseError(ZIP321Error.StaticReason.INVALID_PARAMETER))
            } else {
                Result.success(OtherParam(name, value))
            }

        /**
         * Whether [name] is reserved: one of the five ZIP-321 parameter names, or any
         * `req-`-prefixed name.
         */
        private fun isReservedName(name: String): Boolean {
            return ParamName.entries.any { it.value == name } || name.startsWith("req-")
        }

        /** Whether [name] is a valid `paramname`: `ALPHA *( ALPHA / DIGIT / "+" / "-" )`. */
        private fun isValidParamName(name: String): Boolean =
            name.first().isAsciiLetter() &&
                name.all { ParamNameCharacterSet.characters.contains(it) }
    }
}
