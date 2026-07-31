package org.zecdev.zip321.support

import java.security.MessageDigest

/**
 * TEST SUPPORT — not part of the shipped library.
 *
 * JVM SHA-256: `java.security.MessageDigest`, the platform's own
 * provider-backed digest. "SHA-256" is a MessageDigest algorithm every Java
 * implementation is *required* to support, so no provider plumbing and no
 * third-party dependency is needed.
 *
 * A fresh [MessageDigest] instance is obtained per call on purpose:
 * `MessageDigest` objects are stateful and NOT thread-safe, and
 * `getInstance` is cheap next to the digest itself — this keeps [sha256]
 * safely callable from any thread without locking or thread-locals.
 */
internal actual fun sha256(bytes: ByteArray): ByteArray = MessageDigest.getInstance("SHA-256").digest(bytes)
