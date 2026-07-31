package org.zecdev.zip321.support

/**
 * TEST SUPPORT — not part of the shipped library.
 *
 * Computes the SHA-256 digest of [bytes], returning the 32-byte hash, using
 * the host platform's own cryptographic library.
 *
 * Actual implementations:
 * - JVM: `java.security.MessageDigest.getInstance("SHA-256")`
 *   (`lib/src/jvmTest/.../support/Sha256.jvm.kt`).
 * - iOS (arm64 + simulator): CommonCrypto's `CC_SHA256`
 *   (`lib/src/iosTest/.../support/Sha256.ios.kt`).
 *
 * `expect`/`actual` works in test source sets exactly as it does in main
 * source sets: `commonTest` declares the expectation and each platform test
 * source set (`jvmTest`, and `iosTest` — materialised for both iOS targets by
 * Kotlin's default hierarchy template) supplies the actual. No production code
 * is involved, and the library keeps ZERO runtime dependencies of any kind:
 * this digest exists only so the test-only address-encoding checkers below can
 * verify Base58Check's SHA-256d checksums.
 */
internal expect fun sha256(bytes: ByteArray): ByteArray

/**
 * TEST SUPPORT — not part of the shipped library.
 *
 * SHA-256 façade over the platform digest, used by the Base58Check reference
 * checker to verify the double-SHA-256 checksum of transparent Zcash
 * addresses.
 */
internal object Sha256 {
    /** Computes the SHA-256 digest of [bytes], returning the 32-byte hash. */
    fun hash(bytes: ByteArray): ByteArray = sha256(bytes)

    /**
     * Computes `SHA256(SHA256(bytes))` (a.k.a. SHA-256d), as used by
     * Base58Check checksums.
     */
    fun doubleHash(bytes: ByteArray): ByteArray = hash(hash(bytes))
}
