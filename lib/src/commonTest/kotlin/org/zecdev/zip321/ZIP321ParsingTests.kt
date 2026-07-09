package org.zecdev.zip321

import org.zecdev.zip321.model.OtherParam
import org.zecdev.zip321.support.ReferenceAddressValidator
import org.zecdev.zip321.support.validRecipient
import org.zecdev.zip321.util.TestVectors
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ZIP321ParsingTests {
    @Test
    fun `request String FormattingOptions parses single address with empty param index and address label omitted`() {
        val address = "tmEZhbWHTpdKMw5it8YDspUXSMGQyFwovpU"
        val url = "zcash:$address"

        val parserResult = ZIP321.parse(url, Network.TESTNET, ReferenceAddressValidator.TESTNET).getOrThrow()

        assertEquals(1, parserResult.payments.size)
        val recipientAddress = parserResult.payments[0].recipientAddress
        assertEquals(validRecipient(address), recipientAddress)
    }

    @Test
    fun `request String FormattingOptions parses single address with empty param index and address label not omitted`() {
        val address = "tmEZhbWHTpdKMw5it8YDspUXSMGQyFwovpU"
        val url = "zcash:?address=$address"

        val parserResult = ZIP321.parse(url, Network.TESTNET, ReferenceAddressValidator.TESTNET).getOrThrow()

        val paymentRequest = parserResult
        assertEquals(1, paymentRequest.payments.size)
        val payment = paymentRequest.payments[0]
        assertEquals(validRecipient(address), payment.recipientAddress)
        assertNull(payment.amount)
        assertNull(payment.memo)
        assertNull(payment.label)
        assertNull(payment.message)
        assertTrue(payment.otherParams.isEmpty())
    }

    @Test
    fun `request String FormattingOptions parses single address with all payments enumerated`() {
        val address = "tmEZhbWHTpdKMw5it8YDspUXSMGQyFwovpU"
        val url = "zcash:?address.1=$address"

        val parserResult = ZIP321.parse(url, Network.TESTNET, ReferenceAddressValidator.TESTNET).getOrThrow()

        val paymentRequest = parserResult
        assertEquals(1, paymentRequest.payments.size)
        val payment = paymentRequest.payments[0]
        assertEquals(validRecipient(address), payment.recipientAddress)
        assertNull(payment.amount)
        assertNull(payment.memo)
        assertNull(payment.label)
        assertNull(payment.message)
        assertTrue(payment.otherParams.isEmpty())
    }

    @Test
    fun `request String FormattingOptions parses single payment with label but no amount with empty param index and address label omitted`() {
        val address = "tmEZhbWHTpdKMw5it8YDspUXSMGQyFwovpU"
        val label = "apple"
        val url = "zcash:$address?label=$label"

        val parserResult = ZIP321.parse(url, Network.TESTNET, ReferenceAddressValidator.TESTNET).getOrThrow()

        val paymentRequest = parserResult
        assertEquals(1, paymentRequest.payments.size)
        val payment = paymentRequest.payments[0]
        assertEquals(validRecipient(address), payment.recipientAddress)
        assertEquals(label, payment.label)
        assertNull(payment.memo)
        assertNull(payment.message)
        assertTrue(payment.otherParams.isEmpty())
        assertNull(payment.amount)
    }

    @Test
    fun `request String FormattingOptions parses single payment with label but no amount with empty param index and address label not omitted`() {
        val address = "tmEZhbWHTpdKMw5it8YDspUXSMGQyFwovpU"
        val label = "apple"
        val url = "zcash:?address=$address&label=$label"

        val parserResult = ZIP321.parse(url, Network.TESTNET, ReferenceAddressValidator.TESTNET).getOrThrow()

        val paymentRequest = parserResult
        assertEquals(1, paymentRequest.payments.size)
        val payment = paymentRequest.payments[0]
        assertEquals(validRecipient(address), payment.recipientAddress)
        assertEquals(label, payment.label)
        assertNull(payment.memo)
        assertNull(payment.message)
        assertTrue(payment.otherParams.isEmpty())
        assertNull(payment.amount)
    }

    @Test
    fun `request String FormattingOptions parses single payment with label but no amount with all payments enumerated`() {
        val address = "tmEZhbWHTpdKMw5it8YDspUXSMGQyFwovpU"
        val label = "apple"
        val url = "zcash:?address.1=$address&label.1=$label"

        val parserResult = ZIP321.parse(url, Network.TESTNET, ReferenceAddressValidator.TESTNET).getOrThrow()
        val paymentRequest = parserResult
        assertEquals(1, paymentRequest.payments.size)
        val payment = paymentRequest.payments[0]
        assertEquals(validRecipient(address), payment.recipientAddress)
        assertEquals(label, payment.label)
        assertNull(payment.memo)
        assertNull(payment.message)
        assertTrue(payment.otherParams.isEmpty())
        assertNull(payment.amount)
    }

    @Test
    fun `request String FormattingOptions parses single payment with label and amount with empty param index and address label omitted`() {
        val address = "tmEZhbWHTpdKMw5it8YDspUXSMGQyFwovpU"
        val label = "apple"
        val amount = "123.45"
        val url = "zcash:$address?label=$label&amount=$amount"

        val parserResult = ZIP321.parse(url, Network.TESTNET, ReferenceAddressValidator.TESTNET).getOrThrow()

        val paymentRequest = parserResult
        assertEquals(1, paymentRequest.payments.size)
        val payment = paymentRequest.payments[0]
        assertEquals(validRecipient(address), payment.recipientAddress)
        assertEquals(label, payment.label)
        assertNull(payment.memo)
        assertNull(payment.message)
        assertTrue(payment.otherParams.isEmpty())
        assertEquals(amount, payment.amount?.decimalString())
    }

    @Test
    fun `request String FormattingOptions parses single payment with label and amount with empty param index and address label not omitted`() {
        val address = "tmEZhbWHTpdKMw5it8YDspUXSMGQyFwovpU"
        val label = "apple"
        val amount = "123.45"
        val url = "zcash:?address=$address&label=$label&amount=$amount"

        val parserResult = ZIP321.parse(url, Network.TESTNET, ReferenceAddressValidator.TESTNET).getOrThrow()

        val paymentRequest = parserResult
        assertEquals(1, paymentRequest.payments.size)
        val payment = paymentRequest.payments[0]
        assertEquals(validRecipient(address), payment.recipientAddress)
        assertEquals(label, payment.label)
        assertNull(payment.memo)
        assertNull(payment.message)
        assertTrue(payment.otherParams.isEmpty())
        assertEquals(amount, payment.amount?.decimalString())
    }

    @Test
    fun `request String FormattingOptions parses single payment with label and amount with all payments enumerated`() {
        val address = "tmEZhbWHTpdKMw5it8YDspUXSMGQyFwovpU"
        val label = "apple"
        val amount = "123.45"
        val url = "zcash:?address.1=$address&label.1=$label&amount.1=$amount"

        val parserResult = ZIP321.parse(url, Network.TESTNET, ReferenceAddressValidator.TESTNET).getOrThrow()
        val paymentRequest = parserResult
        assertEquals(1, paymentRequest.payments.size)
        val payment = paymentRequest.payments[0]
        assertEquals(validRecipient(address), payment.recipientAddress)
        assertEquals(label, payment.label)
        assertNull(payment.memo)
        assertNull(payment.message)
        assertTrue(payment.otherParams.isEmpty())
        assertEquals(amount, payment.amount?.decimalString())
    }

    @Test
    fun `request String FormattingOptions parses single payment with amount and unknown parameter with empty param index and address label omitted`() {
        val address = "tmEZhbWHTpdKMw5it8YDspUXSMGQyFwovpU"
        val amount = "123.45"
        val key = "foo"
        val value = "bar"
        val url = "zcash:$address?amount=$amount&$key=$value"

        val parserResult = ZIP321.parse(url, Network.TESTNET, ReferenceAddressValidator.TESTNET).getOrThrow()

        val paymentRequest = parserResult
        assertEquals(1, paymentRequest.payments.size)
        val payment = paymentRequest.payments[0]
        assertEquals(validRecipient(address), payment.recipientAddress)
        assertNull(payment.label)
        assertNull(payment.memo)
        assertNull(payment.message)
        assertEquals(listOf(OtherParam(key, value)), payment.otherParams)
        assertEquals(amount, payment.amount?.decimalString())
    }

    @Test
    fun `request String FormattingOptions parses single payment with amount and unknown parameter with empty param index and address label not omitted`() {
        val address = "tmEZhbWHTpdKMw5it8YDspUXSMGQyFwovpU"
        val amount = "123.45"
        val key = "foo"
        val value = "bar"
        val url = "zcash:?address=$address&amount=$amount&$key=$value"

        val parserResult = ZIP321.parse(url, Network.TESTNET, ReferenceAddressValidator.TESTNET).getOrThrow()

        val paymentRequest = parserResult
        assertEquals(1, paymentRequest.payments.size)
        val payment = paymentRequest.payments[0]
        assertEquals(validRecipient(address), payment.recipientAddress)
        assertNull(payment.label)
        assertNull(payment.memo)
        assertNull(payment.message)
        assertEquals(listOf(OtherParam(key, value)), payment.otherParams)
        assertEquals(amount, payment.amount?.decimalString())
    }

    @Test
    fun `request String FormattingOptions parses single payment with amount and unknown parameter with all payments enumerated`() {
        val address = "tmEZhbWHTpdKMw5it8YDspUXSMGQyFwovpU"
        val amount = "123.45"
        val key = "foo"
        val value = "bar"
        val url = "zcash:?address.1=$address&amount.1=$amount&$key.1=$value"

        val parserResult = ZIP321.parse(url, Network.TESTNET, ReferenceAddressValidator.TESTNET).getOrThrow()

        val paymentRequest = parserResult
        assertEquals(1, paymentRequest.payments.size)
        val payment = paymentRequest.payments[0]
        assertEquals(validRecipient(address), payment.recipientAddress)
        assertNull(payment.label)
        assertNull(payment.memo)
        assertNull(payment.message)
        assertEquals(listOf(OtherParam(key, value)), payment.otherParams)
        assertEquals(amount, payment.amount?.decimalString())
    }

    @Test
    fun `request String FormattingOptions parses single payment with amount and unknown parameter with empty value with empty param index and address label omitted`() {
        val address = "tmEZhbWHTpdKMw5it8YDspUXSMGQyFwovpU"
        val amount = "123.45"
        val key = "foo"
        val value = ""
        val url = "zcash:$address?amount=$amount&$key=$value"

        val parserResult = ZIP321.parse(url, Network.TESTNET, ReferenceAddressValidator.TESTNET).getOrThrow()

        val paymentRequest = parserResult
        assertEquals(1, paymentRequest.payments.size)
        val payment = paymentRequest.payments[0]
        assertEquals(validRecipient(address), payment.recipientAddress)
        assertNull(payment.label)
        assertNull(payment.memo)
        assertNull(payment.message)
        assertEquals(listOf(OtherParam(key, value)), payment.otherParams)
        assertEquals(amount, payment.amount?.decimalString())
    }

    @Test
    fun `request String FormattingOptions parses single payment with amount and unknown parameter with empty value with empty param index and address label not omitted`() {
        val address = "tmEZhbWHTpdKMw5it8YDspUXSMGQyFwovpU"
        val amount = "123.45"
        val key = "foo"
        val value = ""
        val url = "zcash:?address=$address&amount=$amount&$key=$value"

        val parserResult = ZIP321.parse(url, Network.TESTNET, ReferenceAddressValidator.TESTNET).getOrThrow()

        val paymentRequest = parserResult
        assertEquals(1, paymentRequest.payments.size)
        val payment = paymentRequest.payments[0]
        assertEquals(validRecipient(address), payment.recipientAddress)
        assertNull(payment.label)
        assertNull(payment.memo)
        assertNull(payment.message)
        assertEquals(listOf(OtherParam(key, value)), payment.otherParams)
        assertEquals(amount, payment.amount?.decimalString())
    }

    @Test
    fun `request String FormattingOptions parses single payment with amount and unknown parameter with empty value with all payments enumerated`() {
        val address = "tmEZhbWHTpdKMw5it8YDspUXSMGQyFwovpU"
        val amount = "123.45"
        val key = "foo"
        val value = ""
        val url = "zcash:?address.1=$address&amount.1=$amount&$key.1=$value"

        val parserResult = ZIP321.parse(url, Network.TESTNET, ReferenceAddressValidator.TESTNET).getOrThrow()

        val paymentRequest = parserResult
        assertEquals(1, paymentRequest.payments.size)
        val payment = paymentRequest.payments[0]
        assertEquals(validRecipient(address), payment.recipientAddress)
        assertNull(payment.label)
        assertNull(payment.memo)
        assertNull(payment.message)
        assertEquals(listOf(OtherParam(key, value)), payment.otherParams)
        assertEquals(amount, payment.amount?.decimalString())
    }

    @Test
    fun `request String FormattingOptions parses single payment with amount and unknown parameter with no value with empty param index and address label omitted`() {
        val address = "tmEZhbWHTpdKMw5it8YDspUXSMGQyFwovpU"
        val amount = "123.45"
        val param = "foo"
        val url = "zcash:$address?amount=$amount&$param"

        val parserResult = ZIP321.parse(url, Network.TESTNET, ReferenceAddressValidator.TESTNET).getOrThrow()

        val paymentRequest = parserResult
        assertEquals(1, paymentRequest.payments.size)
        val payment = paymentRequest.payments[0]
        assertEquals(validRecipient(address), payment.recipientAddress)
        assertNull(payment.label)
        assertNull(payment.memo)
        assertNull(payment.message)
        assertEquals(amount, payment.amount?.decimalString())
        assertEquals(listOf(OtherParam(param, null)), payment.otherParams)
    }

    @Test
    fun `request String FormattingOptions fails to parse payment URI with an amount param that has no value`() {
        val url = "zcash:tmEZhbWHTpdKMw5it8YDspUXSMGQyFwovpU?amount&foo"

        assertFailsWith<ZIP321Error.AmountInvalid> { ZIP321.parse(url, Network.TESTNET, ReferenceAddressValidator.TESTNET).getOrThrow() }
    }

    @Test
    fun `request String FormattingOptions fails to parse payment URI with an address param that has no value`() {
        val amount = "123.45"
        val param = "foo"
        val url = "zcash:?address&amount=$amount&$param"

        assertFailsWith<ZIP321Error.ParseError> { ZIP321.parse(url, Network.TESTNET, ReferenceAddressValidator.TESTNET).getOrThrow() }
    }

    @Test
    fun `request String FormattingOptions fails to parse payment URI with a memo param that has no value`() {
        val address = "tmEZhbWHTpdKMw5it8YDspUXSMGQyFwovpU"
        val amount = "123.45"
        val param = "foo"
        val url = "zcash:$address?amount=$amount&$param&memo"

        assertFailsWith<ZIP321Error.ParseError> { ZIP321.parse(url, Network.TESTNET, ReferenceAddressValidator.TESTNET).getOrThrow() }
    }

    @Test
    fun `request String FormattingOptions fails to parse payment URI with a label param that has no value`() {
        val address = "tmEZhbWHTpdKMw5it8YDspUXSMGQyFwovpU"
        val amount = "123.45"
        val param = "foo"
        val url = "zcash:$address?amount=$amount&$param&label"

        assertFailsWith<ZIP321Error.ParseError> { ZIP321.parse(url, Network.TESTNET, ReferenceAddressValidator.TESTNET).getOrThrow() }
    }

    @Test
    fun `request String FormattingOptions fails to parse payment URI with a message param that has no value`() {
        val address = "tmEZhbWHTpdKMw5it8YDspUXSMGQyFwovpU"
        val amount = "123.45"
        val param = "foo"
        val url = "zcash:$address?amount=$amount&$param&message"

        assertFailsWith<ZIP321Error.ParseError> { ZIP321.parse(url, Network.TESTNET, ReferenceAddressValidator.TESTNET).getOrThrow() }
    }

    @Test
    fun `request String FormattingOptions parses single payment with amount and unknown parameter with no value with empty param index and address label not omitted`() {
        val address = "tmEZhbWHTpdKMw5it8YDspUXSMGQyFwovpU"
        val amount = "123.45"
        val param = "foo"
        val url = "zcash:?address=$address&amount=$amount&$param"

        val parserResult = ZIP321.parse(url, Network.TESTNET, ReferenceAddressValidator.TESTNET).getOrThrow()

        val paymentRequest = parserResult
        assertEquals(1, paymentRequest.payments.size)
        val payment = paymentRequest.payments[0]
        assertEquals(validRecipient(address), payment.recipientAddress)
        assertNull(payment.label)
        assertNull(payment.memo)
        assertNull(payment.message)
        assertEquals(listOf(OtherParam(param, null)), payment.otherParams)
        assertEquals(amount, payment.amount?.decimalString())
    }

    @Test
    fun `request String FormattingOptions parses single payment with amount and unknown parameter with no value with all payments enumerated`() {
        val address = "tmEZhbWHTpdKMw5it8YDspUXSMGQyFwovpU"
        val amount = "123.45"
        val param = "foo"
        val url = "zcash:?address.1=$address&amount.1=$amount&$param.1"

        val parserResult = ZIP321.parse(url, Network.TESTNET, ReferenceAddressValidator.TESTNET).getOrThrow()

        val paymentRequest = parserResult
        assertEquals(1, paymentRequest.payments.size)
        val payment = paymentRequest.payments[0]
        assertEquals(validRecipient(address), payment.recipientAddress)
        assertNull(payment.label)
        assertNull(payment.memo)
        assertNull(payment.message)
        assertEquals(listOf(OtherParam(param, null)), payment.otherParams)
        assertEquals(amount, payment.amount?.decimalString())
    }

    @Test
    fun `request String FormattingOptions fails to parse address with wrong characters`() {
        val url =
            "zcash:tmEZhbWHTpdKMw5it8YDspUXSMGQyFwovpUʔamount 1ꓸ234?message=Thanks%20for%20your%20payment%20for%20the%20correct%20&amount=20&Have=%20a%20nice%20day"

        // K11: the lead address is everything before the first ASCII '?' ("tmEZ…ʔamount 1ꓸ234"),
        // which fails validation and is now rejected as InvalidAddress (was a downstream
        // ParseError under the pre-K11 ascii-run leading-address heuristic).
        assertFailsWith<ZIP321Error.InvalidAddress> {
            ZIP321.parse(url, Network.TESTNET, ReferenceAddressValidator.TESTNET).getOrThrow()
        }
    }

    @Test
    fun `request String FormattingOptions fails when empty string is provided`() {
        assertFailsWith<ZIP321Error.ParseError> {
            ZIP321.parse("", Network.TESTNET, ReferenceAddressValidator.TESTNET).getOrThrow()
        }
    }

    @Test
    fun `request String FormattingOptions fails when no URI Scheme string is detected`() {
        assertFailsWith<ZIP321Error.InvalidURI> {
            ZIP321.parse("bitcoin:asdfasdfasdfasdfasdfasdfa", Network.TESTNET, ReferenceAddressValidator.TESTNET).getOrThrow()
        }
    }

    @Test
    fun `request String FormattingOptions succeeds when tested against UA test vectors`() {
        // shouldNotThrow<ZIP321.Errors>: any exception fails the test.
        for (ua in TestVectors.unifiedAddresses) {
            ZIP321.parse("zcash:$ua", Network.MAINNET, ReferenceAddressValidator.MAINNET).getOrThrow()
        }
    }

    @Test
    fun `request String FormattingOptions succeeds parsing Sapling address`() {
        ZIP321.parse("zcash:zs1z7rejlpsa98s2rrrfkwmaxu53e4ue0ulcrw0h4x5g8jl04tak0d3mm47vdtahatqrlkngh9slya", Network.MAINNET, ReferenceAddressValidator.MAINNET).getOrThrow()
    }

    @Test
    fun `request String FormattingOptions succeeds parsing Sapling-only usified address`() {
        ZIP321.parse(
            "zcash:u187vrwl4ampyxd5m6aj38n4ndkmj8v6gs97hkt23aps3sn5k89a0gk2smluexgdprcrtm56ezc5c7tjwlrnnl79tjtrxmqd42c5mpyz7g",
            Network.MAINNET,
            ReferenceAddressValidator.MAINNET,
        ).getOrThrow()
    }

    @Test
    fun `request String FormattingOptions succeeds parsing orchard only unified address`() {
        ZIP321.parse(
            "zcash:u1ddnjsdcpm36r6aq79n3s68shjweksnmwtdltrh046s8m6xcws9ygyawalxx8n6hg6vegk0wh8zjnafxgh6msppjsljvyt0ynece3lvm0",
            Network.MAINNET,
            ReferenceAddressValidator.MAINNET,
        ).getOrThrow()
    }
}
