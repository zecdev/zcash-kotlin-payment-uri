package org.zecdev.zip321

import org.zecdev.zip321.model.MemoBytes
import org.zecdev.zip321.model.NonNegativeAmount
import org.zecdev.zip321.model.Payment
import org.zecdev.zip321.model.PaymentRequest
import org.zecdev.zip321.model.RecipientAddress

/**
 * Scope marker for the ZIP-321 [paymentRequest] DSL, preventing accidental cross-scope receiver
 * access (e.g. calling [PaymentRequestScope.payment] from inside a [PaymentScope] block).
 */
@DslMarker
annotation class Zip321Dsl

/**
 * Builds a [PaymentRequest] from a declarative block, collecting payments in source order and
 * auto-indexing them sequentially from `0`. This is a thin layer over [PaymentRequest.Builder] /
 * [Payment.Builder] and shares their totality contract: the block evaluates to a
 * `Result<PaymentRequest>`, never a thrown error, and the FIRST construction error (in source
 * order; within a payment: amount → memo → other params → structural rules) wins.
 *
 * Cross-language mapping: this free function is the idiomatic Kotlin equivalent of the Swift
 * library's `@resultBuilder` entry point `PaymentRequest.build { … }` (Swift cannot name a free
 * function after a type in the same module; Kotlin can, so the bare `paymentRequest { }` form is
 * used here). Both produce the same `Result`-wrapped request as their explicit `Builder`
 * counterparts.
 *
 * The four canonical construction scenarios:
 *
 * ```kotlin
 * // (a) a simple single-address request
 * val simple = paymentRequest {
 *     payment(recipient)
 * }.getOrThrow()
 *
 * // (b) an amount + memo payment
 * val withMemo = paymentRequest {
 *     payment(sapling) {
 *         amount(zec = "1.2345")
 *         memo(utf8 = "Thanks!")
 *         message("Invoice #42")
 *     }
 * }.getOrThrow()
 *
 * // (c) a multi-payment request
 * val multi = paymentRequest {
 *     payment(alice) { amount(zec = "123.456") }
 *     payment(bob) {
 *         amount(zec = "0.789")
 *         memo(utf8 = "hi bob")
 *     }
 * }.getOrThrow()
 *
 * // (d) parsing, with the wallet's own address support as the validator
 * val validator = AddressValidator { address ->
 *     val parsed = walletSdk.parseAddress(address) ?: return@AddressValidator null
 *     AddressDescriptor(
 *         network = if (parsed.isTestnet) Network.TESTNET else Network.MAINNET,
 *         isTransparent = parsed.isTransparent,
 *         canReceiveMemos = parsed.hasShieldedReceiver,
 *     )
 * }
 *
 * val parsed = ZIP321.parse(
 *     ZIP321.uriString(from = multi),
 *     expecting = Network.TESTNET,
 *     validator = validator,
 * ).getOrThrow()
 * ```
 *
 * @param block the DSL block declaring the request's payments.
 * @return [Result.success] with the assembled [PaymentRequest], or [Result.failure] with the
 * first [ZIP321Error] encountered.
 */
fun paymentRequest(block: PaymentRequestScope.() -> Unit): Result<PaymentRequest> {
    return PaymentRequestScope().apply(block).build()
}

/**
 * Receiver scope of the [paymentRequest] DSL: each [payment] call appends one payment to the
 * request, auto-indexed sequentially from `0` in source order (matching
 * [PaymentRequest.Builder.add]).
 */
@Zip321Dsl
class PaymentRequestScope internal constructor() {
    private val builder = PaymentRequest.Builder()
    private var firstError: Throwable? = null

    /**
     * Declares a payment to [recipient], configured by [block] (empty by default: a bare
     * single-address payment). Construction errors are deferred: the first failing payment
     * determines the [paymentRequest] result.
     */
    fun payment(
        recipient: RecipientAddress,
        block: PaymentScope.() -> Unit = {},
    ) {
        PaymentScope(recipient).apply(block).build().fold(
            onSuccess = { builder.add(it) },
            onFailure = { if (firstError == null) firstError = it },
        )
    }

    /**
     * Appends an already-built [Payment] (e.g. from an explicit [Payment.Builder]). Mirrors the
     * Swift result-builder's single-`Payment` expression statement.
     */
    fun payment(payment: Payment) {
        builder.add(payment)
    }

    /**
     * Appends a list of already-built [Payment]s in order. Mirrors the Swift result-builder's
     * `[Payment]` expression statement.
     */
    fun payments(payments: List<Payment>) {
        payments.forEach { builder.add(it) }
    }

    internal fun build(): Result<PaymentRequest> = firstError?.let { Result.failure(it) } ?: builder.build()
}

/**
 * Receiver scope configuring a single payment inside a [paymentRequest] block. Every function
 * delegates to the equivalent [Payment.Builder] chain link, including the deferred-error
 * semantics (the first invalid field wins in the order amount → memo → other params →
 * structural rules).
 */
@Zip321Dsl
class PaymentScope internal constructor(recipient: RecipientAddress) {
    private val builder = Payment.Builder(recipient)

    /** Sets the payment amount from a [NonNegativeAmount] count. See [Payment.Builder.amount]. */
    fun amount(amount: NonNegativeAmount) {
        builder.amount(amount)
    }

    /**
     * Sets the payment amount from a decimal ZEC string (strict ZIP-321 `amountparam` grammar).
     * See [Payment.Builder.amount].
     */
    fun amount(zec: String) {
        builder.amount(zec)
    }

    /** Attaches a [MemoBytes] memo. See [Payment.Builder.memo]. */
    fun memo(memo: MemoBytes) {
        builder.memo(memo)
    }

    /** Attaches a memo from a UTF-8 string. See [Payment.Builder.memo]. */
    fun memo(utf8: String) {
        builder.memo(utf8)
    }

    /** Sets the (plain, decoded) label. See [Payment.Builder.label]. */
    fun label(label: String) {
        builder.label(label)
    }

    /** Sets the (plain, decoded) message. See [Payment.Builder.message]. */
    fun message(message: String) {
        builder.message(message)
    }

    /**
     * Appends an arbitrary (non-reserved) `otherparam`. See [Payment.Builder.otherParam].
     */
    fun otherParam(
        name: String,
        value: String? = null,
    ) {
        builder.otherParam(name, value)
    }

    internal fun build(): Result<Payment> = builder.build()
}
