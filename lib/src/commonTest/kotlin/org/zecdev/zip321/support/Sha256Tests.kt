package org.zecdev.zip321.support

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Known-answer sanity vectors for the platform-backed SHA-256, mirroring the
 * set the Swift reference kept after its own switch to a platform digest
 * (`SHA256Tests.swift`).
 *
 * These no longer exist to validate a hash implementation of ours — there
 * isn't one; `java.security.MessageDigest` and CommonCrypto are validated by
 * their vendors. They pin the *wiring*: that the expect/actual plumbing, the
 * byte order of the digest, and [Sha256.doubleHash]'s composition are correct
 * on every target. Because they live in `commonTest` they run under BOTH
 * `jvmTest` and `iosSimulatorArm64Test`, exercising each actual in turn.
 */
class Sha256Tests {
    /** Renders a byte array as a lowercase hex string for comparison. */
    private fun hex(bytes: ByteArray): String =
        bytes.joinToString("") { (it.toInt() and 0xff).toString(16).padStart(2, '0') }

    private fun bytes(string: String): ByteArray = string.encodeToByteArray()

    @Test
    fun emptyString() {
        // NIST: SHA-256("") known answer.
        assertEquals(
            "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
            hex(Sha256.hash(ByteArray(0))),
        )
    }

    @Test
    fun abc() {
        // FIPS 180-4 Appendix B.1 (one-block message).
        assertEquals(
            "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
            hex(Sha256.hash(bytes("abc"))),
        )
    }

    @Test
    fun twoBlock() {
        // FIPS 180-4 Appendix B.2 (multi-block, 56 bytes -> forces a second block).
        val msg = "abcdbcdecdefdefgefghfghighijhijkijkljklmklmnlmnomnopnopq"
        assertEquals(
            "248d6a61d20638b8e5c026930c3e6039a33ce45964ff2167f6ecedd419db06c1",
            hex(Sha256.hash(bytes(msg))),
        )
    }

    @Test
    fun doubleHashHello() {
        // SHA-256d("hello") sanity vector.
        assertEquals(
            "9595c9df90075148eb06860365df33584b75bff782a510c6cd4883a419833d50",
            hex(Sha256.doubleHash(bytes("hello"))),
        )
    }
}
