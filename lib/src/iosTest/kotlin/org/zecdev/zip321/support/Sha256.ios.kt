package org.zecdev.zip321.support

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.convert
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.usePinned
import platform.CoreCrypto.CC_SHA256
import platform.CoreCrypto.CC_SHA256_DIGEST_LENGTH

/**
 * TEST SUPPORT — not part of the shipped library.
 *
 * iOS SHA-256: Apple's CommonCrypto `CC_SHA256` one-shot digest, reached
 * through Kotlin/Native's bundled `platform.CoreCrypto` interop (part of the
 * Apple platform libraries — no third-party dependency, nothing to add to the
 * consumer's Podfile/SPM manifest).
 *
 * `CC_SHA256(data, len, md)` writes exactly [CC_SHA256_DIGEST_LENGTH] (32)
 * bytes into `md`. Both the input and the output array are pinned for the
 * duration of the call so the garbage collector cannot move them while C code
 * holds their addresses. Empty input is routed through a one-byte scratch
 * buffer with `len = 0`: `Pinned.addressOf(0)` is not valid on a zero-length
 * array, and CommonCrypto reads nothing at the pointer when the length is 0.
 */
@OptIn(ExperimentalForeignApi::class)
internal actual fun sha256(bytes: ByteArray): ByteArray {
    val digest = ByteArray(CC_SHA256_DIGEST_LENGTH)
    val input = if (bytes.isEmpty()) ByteArray(1) else bytes
    input.usePinned { pinnedInput ->
        digest.usePinned { pinnedDigest ->
            CC_SHA256(
                pinnedInput.addressOf(0),
                bytes.size.convert(),
                pinnedDigest.addressOf(0).reinterpret(),
            )
        }
    }
    return digest
}
