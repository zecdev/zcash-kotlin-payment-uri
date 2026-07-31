package org.zecdev.zip321

import org.zecdev.zip321.model.NonNegativeAmount
import org.zecdev.zip321.model.OtherParam
import org.zecdev.zip321.model.Payment
import org.zecdev.zip321.model.PaymentRequest
import org.zecdev.zip321.support.ReferenceAddressValidator
import org.zecdev.zip321.support.validRecipient
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * `Payment.otherParams` is an ALWAYS-PRESENT list, and `otherparam` names are
 * unique within a payment.
 *
 * There is no distinction between "no other params" and "an empty list of other
 * params": ZIP-321 has no way to spell the difference, and the reference
 * implementation does not model one either. Representing both would make two
 * distinct `Payment` values render to the same URI, breaking the round-trip law
 * — so the nullable `List<OtherParam>?` is gone.
 */
class OtherParamsTests {
    private val recipient = validRecipient("ztestsapling10yy2ex5dcqkclhc7z7yrnjq2z6feyjad56ptwlfgmy77dmaqqrl9gyhprdx59qgmsnyfska2kez")

    private fun payment(otherParams: List<OtherParam>): Payment =
        Payment.create(
            recipientAddress = recipient,
            amount = NonNegativeAmount.zec("1.2345").getOrThrow(),
            memo = null,
            label = null,
            message = null,
            otherParams = otherParams,
        ).getOrThrow()

    // -- `otherParams` is an always-present list -----------------------------

    @Test
    fun `a payment built without other params has an empty list`() {
        val built =
            Payment.create(
                recipientAddress = recipient,
                amount = null,
                memo = null,
                label = null,
                message = null,
            ).getOrThrow()

        assertTrue(built.otherParams.isEmpty())
    }

    /**
     * The construct → render → parse invariant for the EMPTY case: a payment
     * with an empty `otherParams` renders without any extra query parameter and
     * parses back to an equal payment (whose `otherParams` is, again, empty).
     */
    @Test
    fun `empty other params survive the round trip`() {
        val request = PaymentRequest(payments = listOf(payment(emptyList())))
        // The canonical reference form preserves the stored paramindex (K13 makes it the
        // default; at this point it must be requested explicitly).
        val uri = ZIP321.uriString(request, ZIP321.FormattingOptions.UseEmptyParamIndex(omitAddressLabel = true))

        val parsed = ZIP321.parse(uri, Network.TESTNET, ReferenceAddressValidator.TESTNET).getOrThrow()

        assertEquals(request, parsed)
        assertTrue(parsed.payments[0].otherParams.isEmpty())
    }

    /** The same invariant with a non-empty list: order and values are preserved. */
    @Test
    fun `non-empty other params survive the round trip`() {
        val params =
            listOf(
                OtherParam("future", "value"),
                OtherParam("flag", null),
            )
        val request = PaymentRequest(payments = listOf(payment(params)))
        // The canonical reference form preserves the stored paramindex (K13 makes it the
        // default; at this point it must be requested explicitly).
        val uri = ZIP321.uriString(request, ZIP321.FormattingOptions.UseEmptyParamIndex(omitAddressLabel = true))

        val parsed = ZIP321.parse(uri, Network.TESTNET, ReferenceAddressValidator.TESTNET).getOrThrow()

        assertEquals(request, parsed)
        assertEquals(params, parsed.payments[0].otherParams)
    }

    /** Every parsed payment reports a list, whether or not the URI carried any. */
    @Test
    fun `parsed other params are always a list`() {
        val withNone = ZIP321.parse("zcash:${recipient.value}?amount=1", Network.TESTNET, ReferenceAddressValidator.TESTNET).getOrThrow()
        assertTrue(withNone.payments[0].otherParams.isEmpty())

        val withSome =
            ZIP321.parse(
                "zcash:${recipient.value}?amount=1&future=value",
                Network.TESTNET,
                ReferenceAddressValidator.TESTNET,
            ).getOrThrow()
        assertEquals(listOf(OtherParam("future", "value")), withSome.payments[0].otherParams)
    }

    // -- Duplicate other-param names are rejected at construction ------------

    @Test
    fun `duplicate other param names are rejected`() {
        val result =
            Payment.create(
                recipientAddress = recipient,
                amount = null,
                memo = null,
                label = null,
                message = null,
                otherParams =
                    listOf(
                        OtherParam("future", "one"),
                        OtherParam("future", "two"),
                    ),
            )

        val error = assertIs<ZIP321Error.DuplicateParameter>(result.exceptionOrNull())
        assertEquals("future", error.name)
        assertEquals(null, error.index)
    }

    /** The value plays no part: a repeated NAME is what is rejected. */
    @Test
    fun `duplicate other param names are rejected regardless of value`() {
        val result =
            Payment.create(
                recipientAddress = recipient,
                amount = null,
                memo = null,
                label = null,
                message = null,
                otherParams =
                    listOf(
                        OtherParam("future", "same"),
                        OtherParam("future", "same"),
                    ),
            )

        assertIs<ZIP321Error.DuplicateParameter>(result.exceptionOrNull())
    }

    @Test
    fun `distinct other param names are accepted`() {
        val built =
            payment(
                listOf(
                    OtherParam("future", "one"),
                    OtherParam("other", "two"),
                ),
            )

        assertEquals(2, built.otherParams.size)
    }
}
