package org.zecdev.zip321.model

import org.zecdev.zip321.support.validRecipient
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * Direct tests of [Payment]'s hand-written `equals`/`hashCode`, exercising each field
 * independently (the rest of the suite only ever compares two fully-matching or wholesale-built
 * payments, never two payments differing in exactly one field), plus the "different type" and
 * null-field `hashCode` elvis paths.
 */
class PaymentEqualityTests {
    private val recipient = validRecipient("tmEZhbWHTpdKMw5it8YDspUXSMGQyFwovpU")
    private val otherRecipient = validRecipient("t26YoyZ1iPgiMEWL4zGUm74eVWfhyDMXzY2")
    private val shieldedRecipient =
        validRecipient(
            "ztestsapling10yy2ex5dcqkclhc7z7yrnjq2z6feyjad56ptwlfgmy77dmaqqrl9gyhprdx59qgmsnyfska2kez",
        )

    private fun base(
        recipientAddress: RecipientAddress = recipient,
        amount: NonNegativeAmount? = NonNegativeAmount.zatoshi(1uL).getOrThrow(),
        memo: MemoBytes? = null,
        label: String? = "label",
        message: String? = "message",
        otherParams: List<OtherParam> = listOf(OtherParam.create("future", "x").getOrThrow()),
    ) = Payment.create(recipientAddress, amount, memo, label, message, otherParams).getOrThrow()

    @Test
    fun `equals is reflexive and rejects a different type`() {
        val payment = base()
        assertTrue(payment == payment)
        @Suppress("EqualsBetweenInconvertibleTypes")
        assertFalse(payment.equals("not a Payment"))
    }

    @Test
    fun `equals distinguishes a difference in any single field`() {
        val reference = base()

        assertNotEquals(reference, base(recipientAddress = otherRecipient))
        assertNotEquals(reference, base(amount = NonNegativeAmount.zatoshi(2uL).getOrThrow()))
        assertNotEquals(reference, base(label = "different label"))
        assertNotEquals(reference, base(message = "different message"))
        assertNotEquals(reference, base(otherParams = listOf(OtherParam.create("future", "different").getOrThrow())))

        // The memo comparison is only reached once recipientAddress/amount already match, so this
        // pair must share a (shielded, since memos aren't allowed to transparent) recipient.
        val shieldedReference = base(recipientAddress = shieldedRecipient, memo = null)
        assertNotEquals(shieldedReference, base(recipientAddress = shieldedRecipient, memo = MemoBytes("a memo")))
    }

    @Test
    fun `equals treats null and non-null optional fields as unequal`() {
        val withMemo = base(recipientAddress = shieldedRecipient, memo = MemoBytes("a memo"))
        val withoutMemo = base(recipientAddress = shieldedRecipient, memo = null)
        assertNotEquals(withMemo, withoutMemo)

        assertNotEquals(base(label = null), base(label = "label"))
        assertNotEquals(base(message = null), base(message = "message"))
        assertNotEquals(base(otherParams = emptyList()), base(otherParams = listOf(OtherParam.create("future", "x").getOrThrow())))
    }

    @Test
    fun `hashCode is consistent for equal payments and varies with each optional field`() {
        assertEquals(base().hashCode(), base().hashCode())

        // All-null optional fields (every `?: 0` elvis branch taken) vs. all-present.
        val allNull =
            Payment.create(recipient, amount = null, memo = null, label = null, message = null, otherParams = emptyList())
                .getOrThrow()
        val allNullAgain =
            Payment.create(recipient, amount = null, memo = null, label = null, message = null, otherParams = emptyList())
                .getOrThrow()
        assertEquals(allNull.hashCode(), allNullAgain.hashCode())

        val allPresent =
            Payment.create(
                shieldedRecipient,
                amount = NonNegativeAmount.zatoshi(5uL).getOrThrow(),
                memo = MemoBytes("hi"),
                label = "label",
                message = "message",
                otherParams = listOf(OtherParam.create("future", "x").getOrThrow()),
            ).getOrThrow()

        assertNotEquals(allNull.hashCode(), allPresent.hashCode())
    }
}
