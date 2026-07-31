package org.zecdev.zip321

import org.zecdev.zip321.ZIP321.FormattingOptions.EnumerateAllPayments
import org.zecdev.zip321.ZIP321.FormattingOptions.UseEmptyParamIndex
import org.zecdev.zip321.conformance.ConformanceRunner
import org.zecdev.zip321.conformance.networkOfVector
import org.zecdev.zip321.support.ReferenceAddressValidator
import kotlin.test.Test
import kotlin.test.assertEquals

class RoundTripTests {
    @Test
    fun `Round-trip parsing and encoding via request of single address with empty param index and address label omitted`() {
        val url = "zcash:tmEZhbWHTpdKMw5it8YDspUXSMGQyFwovpU"
        val parserResult = ZIP321.parse(url, Network.TESTNET, ReferenceAddressValidator.TESTNET).getOrThrow()
        assertEquals(1, parserResult.payments.size)
        val recipientAddress = parserResult.payments[0].recipientAddress
        val roundTrip = ZIP321.request(recipientAddress, UseEmptyParamIndex(true))
        assertEquals(url, roundTrip)
    }

    @Test
    fun `Round-trip parsing and encoding via request of single address with empty param index and address label not omitted`() {
        val url = "zcash:?address=tmEZhbWHTpdKMw5it8YDspUXSMGQyFwovpU"
        val parserResult = ZIP321.parse(url, Network.TESTNET, ReferenceAddressValidator.TESTNET).getOrThrow()
        val paymentRequest = parserResult
        val roundTrip = ZIP321.uriString(paymentRequest, UseEmptyParamIndex(false))
        assertEquals(url, roundTrip)
    }

    @Test
    fun `Round-trip parsing and encoding via request of single address with all payments enumerated`() {
        val url = "zcash:?address.1=tmEZhbWHTpdKMw5it8YDspUXSMGQyFwovpU"
        val parserResult = ZIP321.parse(url, Network.TESTNET, ReferenceAddressValidator.TESTNET).getOrThrow()
        val paymentRequest = parserResult
        val roundTrip = ZIP321.uriString(paymentRequest, EnumerateAllPayments)
        assertEquals(url, roundTrip)
    }

    @Test
    fun `Round-trip parsing and encoding via uriString of single payment with label and amount with empty param index and address label omitted`() {
        val url = "zcash:tmEZhbWHTpdKMw5it8YDspUXSMGQyFwovpU?amount=123.45&label=apple"
        val parserResult = ZIP321.parse(url, Network.TESTNET, ReferenceAddressValidator.TESTNET).getOrThrow()
        val paymentRequest = parserResult
        val roundTrip = ZIP321.uriString(paymentRequest, UseEmptyParamIndex(true))
        assertEquals(url, roundTrip)
    }

    @Test
    fun `Round-trip parsing and encoding via uriString of single payment with label and amount with empty param index and address label not omitted`() {
        val url = "zcash:?address=tmEZhbWHTpdKMw5it8YDspUXSMGQyFwovpU&amount=123.45&label=apple"
        val parserResult = ZIP321.parse(url, Network.TESTNET, ReferenceAddressValidator.TESTNET).getOrThrow()
        val paymentRequest = parserResult
        val roundTrip = ZIP321.uriString(paymentRequest, UseEmptyParamIndex(false))
        assertEquals(url, roundTrip)
    }

    @Test
    fun `Round-trip parsing and encoding via uriString of single payment with label and amount with all payments enumerated`() {
        val url = "zcash:?address.1=tmEZhbWHTpdKMw5it8YDspUXSMGQyFwovpU&amount.1=123.45&label.1=apple"
        val parserResult = ZIP321.parse(url, Network.TESTNET, ReferenceAddressValidator.TESTNET).getOrThrow()
        val paymentRequest = parserResult
        val roundTrip = ZIP321.uriString(paymentRequest, EnumerateAllPayments)
        assertEquals(url, roundTrip)
    }

    @Test
    fun `Round-trip parsing and encoding via uriString of single payment with amount and label containing delimiter with empty param index and address label omitted`() {
        val url = "zcash:tmEZhbWHTpdKMw5it8YDspUXSMGQyFwovpU?amount=123.45&label=apple+banana"
        val parserResult = ZIP321.parse(url, Network.TESTNET, ReferenceAddressValidator.TESTNET).getOrThrow()
        val paymentRequest = parserResult
        val roundTrip = ZIP321.uriString(paymentRequest, UseEmptyParamIndex(true))
        assertEquals(url, roundTrip)
    }

    @Test
    fun `Round-trip parsing and encoding via uriString of single payment with amount and label containing delimiter with empty param index and address label not omitted`() {
        val url = "zcash:?address=tmEZhbWHTpdKMw5it8YDspUXSMGQyFwovpU&amount=123.45&label=apple+banana"
        val parserResult = ZIP321.parse(url, Network.TESTNET, ReferenceAddressValidator.TESTNET).getOrThrow()
        val paymentRequest = parserResult
        val roundTrip = ZIP321.uriString(paymentRequest, UseEmptyParamIndex(false))
        assertEquals(url, roundTrip)
    }

    @Test
    fun `Round-trip parsing and encoding via uriString of single payment with amount and label containing delimiter with all payments enumerated`() {
        val url = "zcash:?address.1=tmEZhbWHTpdKMw5it8YDspUXSMGQyFwovpU&amount.1=123.45&label.1=apple+banana"
        val parserResult = ZIP321.parse(url, Network.TESTNET, ReferenceAddressValidator.TESTNET).getOrThrow()
        val paymentRequest = parserResult
        val roundTrip = ZIP321.uriString(paymentRequest, EnumerateAllPayments)
        assertEquals(url, roundTrip)
    }

    @Test
    fun `Round-trip parsing and encoding via uriString of single payment with label but no amount with empty param index and address label omitted`() {
        val url = "zcash:tmEZhbWHTpdKMw5it8YDspUXSMGQyFwovpU?label=apple"
        val parserResult = ZIP321.parse(url, Network.TESTNET, ReferenceAddressValidator.TESTNET).getOrThrow()
        val paymentRequest = parserResult
        val roundTrip = ZIP321.uriString(paymentRequest, UseEmptyParamIndex(true))
        assertEquals(url, roundTrip)
    }

    @Test
    fun `Round-trip parsing and encoding via uriString of single payment with label but no amount with empty param index and address label not omitted`() {
        val url = "zcash:?address=tmEZhbWHTpdKMw5it8YDspUXSMGQyFwovpU&label=apple"
        val parserResult = ZIP321.parse(url, Network.TESTNET, ReferenceAddressValidator.TESTNET).getOrThrow()
        val paymentRequest = parserResult
        val roundTrip = ZIP321.uriString(paymentRequest, UseEmptyParamIndex(false))
        assertEquals(url, roundTrip)
    }

    @Test
    fun `Round-trip parsing and encoding via uriString of single payment with label but no amount with all payments enumerated`() {
        val url = "zcash:?address.1=tmEZhbWHTpdKMw5it8YDspUXSMGQyFwovpU&label.1=apple"
        val parserResult = ZIP321.parse(url, Network.TESTNET, ReferenceAddressValidator.TESTNET).getOrThrow()
        val paymentRequest = parserResult
        val roundTrip = ZIP321.uriString(paymentRequest, EnumerateAllPayments)
        assertEquals(url, roundTrip)
    }

    @Test
    fun `Round-trip parsing and encoding via uriString of single payment with amount and unknown parameter with empty param index and address label omitted`() {
        val url = "zcash:tmEZhbWHTpdKMw5it8YDspUXSMGQyFwovpU?amount=123.45&foo=bar"
        val parserResult = ZIP321.parse(url, Network.TESTNET, ReferenceAddressValidator.TESTNET).getOrThrow()
        val paymentRequest = parserResult
        val roundTrip = ZIP321.uriString(paymentRequest, UseEmptyParamIndex(true))
        assertEquals(url, roundTrip)
    }

    @Test
    fun `Round-trip parsing and encoding via uriString of single payment with amount and many 'other' parameters with empty param index and address label omitted`() {
        val url = "zcash:tmEZhbWHTpdKMw5it8YDspUXSMGQyFwovpU?amount=123.45&foo=bar&bar=foo"
        val parserResult = ZIP321.parse(url, Network.TESTNET, ReferenceAddressValidator.TESTNET).getOrThrow()
        val paymentRequest = parserResult
        val roundTrip = ZIP321.uriString(paymentRequest, UseEmptyParamIndex(true))
        assertEquals(url, roundTrip)
    }

    @Test
    fun `Round-trip parsing and encoding via uriString of single payment with amount and unknown parameter with empty param index and address label not omitted`() {
        val url = "zcash:?address=tmEZhbWHTpdKMw5it8YDspUXSMGQyFwovpU&amount=123.45&foo=bar"
        val parserResult = ZIP321.parse(url, Network.TESTNET, ReferenceAddressValidator.TESTNET).getOrThrow()
        val paymentRequest = parserResult
        val roundTrip = ZIP321.uriString(paymentRequest, UseEmptyParamIndex(false))
        assertEquals(url, roundTrip)
    }

    @Test
    fun `Round-trip parsing and encoding via uriString of single payment with amount and unknown parameter with all payments enumerated`() {
        val url = "zcash:?address.1=tmEZhbWHTpdKMw5it8YDspUXSMGQyFwovpU&amount.1=123.45&foo.1=bar"
        val parserResult = ZIP321.parse(url, Network.TESTNET, ReferenceAddressValidator.TESTNET).getOrThrow()
        val paymentRequest = parserResult
        val roundTrip = ZIP321.uriString(paymentRequest, EnumerateAllPayments)
        assertEquals(url, roundTrip)
    }

    // NOTE (K13): address-label omission only applies to a SINGLE payment at the empty
    // paramindex (matching the reference `to_uri`). A multi-payment request parsed from the
    // leading-address form re-renders with explicit `address[.n]=` labels; the two forms parse
    // to the same request, so the round trip holds by VALUE (not by string).
    @Test
    fun `Round-trip parsing and encoding of multiple payments with empty param index and address label omitted`() {
        val url = "zcash:tmEZhbWHTpdKMw5it8YDspUXSMGQyFwovpU?amount=123.45&label=apple&address.1=ztestsapling10yy2ex5dcqkclhc7z7yrnjq2z6feyjad56ptwlfgmy77dmaqqrl9gyhprdx59qgmsnyfska2kez&amount.1=1.2345&label.1=banana"
        val expected = "zcash:?address=tmEZhbWHTpdKMw5it8YDspUXSMGQyFwovpU&amount=123.45&label=apple&address.1=ztestsapling10yy2ex5dcqkclhc7z7yrnjq2z6feyjad56ptwlfgmy77dmaqqrl9gyhprdx59qgmsnyfska2kez&amount.1=1.2345&label.1=banana"
        val parserResult = ZIP321.parse(url, Network.TESTNET, ReferenceAddressValidator.TESTNET).getOrThrow()
        val paymentRequest = parserResult
        val roundTrip = ZIP321.uriString(paymentRequest, UseEmptyParamIndex(true))
        assertEquals(expected, roundTrip)
        // The re-rendered form parses back to the SAME request.
        val reparsed = ZIP321.parse(roundTrip, Network.TESTNET, ReferenceAddressValidator.TESTNET).getOrThrow()
        assertEquals(paymentRequest, reparsed)
    }

    // NOTE (K1/v2): this test had the same kotest name as the one above
    // (kotest's duplicate-name mangling kept both); "(2)" disambiguates.
    @Test
    fun `Round-trip parsing and encoding of multiple payments with empty param index and address label omitted 2`() {
        val url = "zcash:tmEZhbWHTpdKMw5it8YDspUXSMGQyFwovpU?foo=bar&bar=foo&address.1=ztestsapling10yy2ex5dcqkclhc7z7yrnjq2z6feyjad56ptwlfgmy77dmaqqrl9gyhprdx59qgmsnyfska2kez&amount.1=1.2345&label.1=banana"
        val expected = "zcash:?address=tmEZhbWHTpdKMw5it8YDspUXSMGQyFwovpU&foo=bar&bar=foo&address.1=ztestsapling10yy2ex5dcqkclhc7z7yrnjq2z6feyjad56ptwlfgmy77dmaqqrl9gyhprdx59qgmsnyfska2kez&amount.1=1.2345&label.1=banana"
        val parserResult = ZIP321.parse(url, Network.TESTNET, ReferenceAddressValidator.TESTNET).getOrThrow()
        val paymentRequest = parserResult
        val roundTrip = ZIP321.uriString(paymentRequest, UseEmptyParamIndex(true))
        assertEquals(expected, roundTrip)
        val reparsed = ZIP321.parse(roundTrip, Network.TESTNET, ReferenceAddressValidator.TESTNET).getOrThrow()
        assertEquals(paymentRequest, reparsed)
    }

    @Test
    fun `Round-trip parsing and encoding of multiple payments with empty param index and address label not omitted`() {
        val url = "zcash:?address=tmEZhbWHTpdKMw5it8YDspUXSMGQyFwovpU&amount=123.45&label=apple&address.1=ztestsapling10yy2ex5dcqkclhc7z7yrnjq2z6feyjad56ptwlfgmy77dmaqqrl9gyhprdx59qgmsnyfska2kez&amount.1=1.2345&label.1=banana"
        val parserResult = ZIP321.parse(url, Network.TESTNET, ReferenceAddressValidator.TESTNET).getOrThrow()
        val paymentRequest = parserResult
        val roundTrip = ZIP321.uriString(paymentRequest, UseEmptyParamIndex(false))
        assertEquals(url, roundTrip)
    }

    @Test
    fun `Round-trip parsing and encoding of multiple payments with all payments enumerated`() {
        val url = "zcash:?address.1=tmEZhbWHTpdKMw5it8YDspUXSMGQyFwovpU&amount.1=123.45&label.1=apple&address.2=ztestsapling10yy2ex5dcqkclhc7z7yrnjq2z6feyjad56ptwlfgmy77dmaqqrl9gyhprdx59qgmsnyfska2kez&amount.2=1.2345&label.2=banana"
        val parserResult = ZIP321.parse(url, Network.TESTNET, ReferenceAddressValidator.TESTNET).getOrThrow()
        val paymentRequest = parserResult
        val roundTrip = ZIP321.uriString(paymentRequest, EnumerateAllPayments)
        assertEquals(url, roundTrip)
    }

    // MARK: K13 — the round-trip law under the DEFAULT rendering options

    @Test
    fun `default formatting options render back the canonical single-payment form`() {
        val url = "zcash:tmEZhbWHTpdKMw5it8YDspUXSMGQyFwovpU?amount=123.45&label=apple+banana"
        val parsed = ZIP321.parse(url, Network.TESTNET, ReferenceAddressValidator.TESTNET).getOrThrow()
        val roundTrip = ZIP321.uriString(from = parsed)
        assertEquals(url, roundTrip)
    }

    /**
     * The round-trip law for the DEFAULT rendering options:
     * `parse(uriString(from = r)) == success(r)` for every request `r`.
     *
     * Exercised over EVERY valid vector of the shared conformance corpus (which is
     * oracle-verified against the librustzcash `zip321` reference) — including the bare
     * `zcash:<addr>` vectors, which are now ordinary one-payment requests rather than a
     * separate result shape.
     */
    @Test
    fun `default render round-trips for corpus requests`() {
        for (vector in ConformanceRunner.validVectors) {
            val network = networkOfVector(vector.network)
            val validator = ReferenceAddressValidator.of(network)

            val parsed = ZIP321.parse(vector.uri, network, validator).getOrThrow()

            // Render with the DEFAULT options, then parse back and assert equality.
            val rendered = ZIP321.uriString(from = parsed)
            val reparsed = ZIP321.parse(rendered, network, validator).getOrThrow()

            assertEquals(
                parsed,
                reparsed,
                "${vector.name}: round-trip law violated — parse(uriString(from = r)) != r " +
                    "(rendered: $rendered)",
            )
        }
    }
}
