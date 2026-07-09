package org.zecdev.zip321

import org.zecdev.zip321.model.RecipientAddress
import org.zecdev.zip321.support.ReferenceAddressValidator
import org.zecdev.zip321.support.validRecipient
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RecipientTests {
    // -- The validator is the sole authority ---------------------------------

    @Test
    fun `Recipient create is null when the validator rejects`() {
        assertNull(RecipientAddress.create("asdf", AddressValidator { null }))
    }

    @Test
    fun `Recipient takes the descriptor from the validator verbatim`() {
        // The string looks transparent; the validator says it is shielded and
        // memo-capable. The library believes the validator, not the string.
        val descriptor = AddressDescriptor(Network.MAINNET, isTransparent = false, canReceiveMemos = true)
        val recipient =
            assertNotNull(
                RecipientAddress.create("t1Hsc1LR8yKnbbe3twRp88p6vFfC5t7DLbs", AddressValidator { descriptor }),
            )

        assertEquals("t1Hsc1LR8yKnbbe3twRp88p6vFfC5t7DLbs", recipient.value)
        assertEquals(descriptor, recipient.descriptor)
        assertEquals(Network.MAINNET, recipient.network)
    }

    @Test
    fun `Recipient can be built from an already validated address`() {
        val descriptor = AddressDescriptor(Network.TESTNET, isTransparent = true, canReceiveMemos = false)
        val recipient = RecipientAddress("tmEZhbWHTpdKMw5it8YDspUXSMGQyFwovpU", descriptor)

        assertEquals(descriptor, recipient.descriptor)
        assertEquals(Network.TESTNET, recipient.network)
    }

    @Test
    fun `Recipients are equal when value and descriptor agree`() {
        val descriptor = AddressDescriptor(Network.TESTNET, isTransparent = true, canReceiveMemos = false)
        val other = AddressDescriptor(Network.TESTNET, isTransparent = false, canReceiveMemos = true)

        assertEquals(RecipientAddress("tm1", descriptor), RecipientAddress("tm1", descriptor))
        assertNotEquals(RecipientAddress("tm1", descriptor), RecipientAddress("tm2", descriptor))
        assertNotEquals(RecipientAddress("tm1", descriptor), RecipientAddress("tm1", other))
    }

    // -- Sanity checks against the reference (test-only) validator ------------

    @Test
    fun `Recipient create is null when the reference validator rejects garbage`() {
        assertNull(RecipientAddress.create("asdf", ReferenceAddressValidator.TESTNET))
    }

    @Test
    fun `RecipientAddress detects invalid characters`() {
        assertNull(
            RecipientAddress.create(
                "ztestsapling10yy2ex5dcqkclhc7z7yrnjq2z6feyjad56ptwlfgmy77dmaqqrl9gyhprdx59qgmsnyfska2ke_",
                ReferenceAddressValidator.TESTNET,
            ),
        )
    }

    @Test
    fun `RecipientAddress accepts an Orchard-only unified address`() {
        assertNotNull(
            RecipientAddress.create(
                "u1ddnjsdcpm36r6aq79n3s68shjweksnmwtdltrh046s8m6xcws9ygyawalxx8n6hg6vegk0wh8zjnafxgh6msppjsljvyt0ynece3lvm0",
                ReferenceAddressValidator.MAINNET,
            ),
        )
    }

    @Test
    fun `RecipientAddress accepts a mainnet Sapling address`() {
        assertNotNull(
            RecipientAddress.create(
                "zs1z7rejlpsa98s2rrrfkwmaxu53e4ue0ulcrw0h4x5g8jl04tak0d3mm47vdtahatqrlkngh9slya",
                ReferenceAddressValidator.MAINNET,
            ),
        )
    }

    // MARK: - equals() / hashCode() / capabilities

    @Test
    fun `equals compares value and descriptor and rejects a different type`() {
        val address = "tmEZhbWHTpdKMw5it8YDspUXSMGQyFwovpU"
        val a = validRecipient(address)
        val b = validRecipient(address)
        val different = validRecipient("t26YoyZ1iPgiMEWL4zGUm74eVWfhyDMXzY2")

        assertTrue(a == a)
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
        assertFalse(a == different)
        @Suppress("EqualsBetweenInconvertibleTypes")
        assertFalse(a.equals("not a RecipientAddress"))
    }

    @Test
    fun `capabilities are read off the validator's descriptor`() {
        val transparent = validRecipient("tmEZhbWHTpdKMw5it8YDspUXSMGQyFwovpU")
        val shielded =
            validRecipient(
                "ztestsapling10yy2ex5dcqkclhc7z7yrnjq2z6feyjad56ptwlfgmy77dmaqqrl9gyhprdx59qgmsnyfska2kez",
            )

        assertTrue(transparent.isTransparent)
        assertFalse(transparent.canReceiveMemos)
        assertFalse(shielded.isTransparent)
        assertTrue(shielded.canReceiveMemos)
    }
}
