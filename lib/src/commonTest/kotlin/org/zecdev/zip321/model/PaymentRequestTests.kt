package org.zecdev.zip321.model

import org.zecdev.zip321.ZIP321Error
import org.zecdev.zip321.support.validRecipient
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Direct tests of [PaymentRequest] paths not otherwise exercised via [org.zecdev.zip321.BuilderTests]
 * or the conformance/round-trip suites: the plain `List<Payment>` constructor's own
 * [PaymentRequest.MAX_PAYMENT_COUNT] guard (as opposed to [PaymentRequest.fromIndexedPayments]'s,
 * exercised via `PaymentRequest.Builder`), a duplicate at the EMPTY paramindex specifically, and
 * `equals`/`hashCode`/`toString`.
 */
class PaymentRequestTests {
    private fun payment(): Payment =
        Payment.Builder(validRecipient("tmEZhbWHTpdKMw5it8YDspUXSMGQyFwovpU"))
            .build()
            .getOrThrow()

    @Test
    fun `plain list constructor rejects more than MAX_PAYMENT_COUNT payments`() {
        // Reuse a single built Payment: only the COUNT is under test here, and re-validating a
        // fresh RecipientAddress ~10,000 times would needlessly slow the suite down.
        val p = payment()
        val tooMany = List(PaymentRequest.MAX_PAYMENT_COUNT.toInt() + 1) { p }

        val error =
            assertFailsWith<ZIP321Error.TooManyPayments> {
                PaymentRequest(tooMany)
            }
        assertEquals(PaymentRequest.MAX_PAYMENT_COUNT + 1u, error.count)
    }

    @Test
    fun `plain list constructor accepts exactly MAX_PAYMENT_COUNT payments`() {
        val p = payment()
        val exactlyMax = List(PaymentRequest.MAX_PAYMENT_COUNT.toInt()) { p }
        val request = PaymentRequest(exactlyMax)
        assertEquals(PaymentRequest.MAX_PAYMENT_COUNT.toInt(), request.payments.size)
    }

    @Test
    fun `fromIndexedPayments reports a duplicate at the empty paramindex as a null index`() {
        val error =
            assertFailsWith<ZIP321Error.DuplicateParameter> {
                PaymentRequest.fromIndexedPayments(
                    listOf(
                        IndexedPayment(0u, payment()),
                        IndexedPayment(0u, payment()),
                    ),
                )
            }
        assertEquals(ZIP321Error.DuplicateParameter("address", null), error)
    }

    @Test
    fun `fromIndexedPayments reports a duplicate at a non-zero paramindex with that index`() {
        val error =
            assertFailsWith<ZIP321Error.DuplicateParameter> {
                PaymentRequest.fromIndexedPayments(
                    listOf(
                        IndexedPayment(3u, payment()),
                        IndexedPayment(3u, payment()),
                    ),
                )
            }
        assertEquals(ZIP321Error.DuplicateParameter("address", 3u), error)
    }

    @Test
    fun `equals handles identity a different type and unequal content and hashCode agrees`() {
        val a = PaymentRequest(listOf(payment()))
        val b = PaymentRequest(listOf(payment()))
        val empty = PaymentRequest(emptyList())

        assertTrue(a == a)
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
        assertFalse(a == empty)
        @Suppress("EqualsBetweenInconvertibleTypes")
        assertFalse(a.equals("not a PaymentRequest"))
    }

    @Test
    fun `toString renders the indexed payments`() {
        val request = PaymentRequest(listOf(payment()))
        assertEquals("PaymentRequest(indexedPayments=${request.indexedPayments})", request.toString())
    }
}
