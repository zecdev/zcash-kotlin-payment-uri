package org.zecdev.zip321.parser

enum class ParserContext {
    MAINNET,
    TESTNET,
    REGTEST;

    private val sproutPrefix: String
        get() = when (this) {
            MAINNET -> "zc"
            TESTNET, REGTEST -> "zt"
        }

    private val saplingPrefix: String
        get() = when (this) {
            MAINNET -> "zs"
            TESTNET -> "ztestsapling"
            REGTEST -> "zregtestsapling"
        }

    private val unifiedPrefix: String
        get() = when (this) {
            MAINNET -> "u"
            TESTNET -> "utest"
            REGTEST -> "uregtest"
        }

    private val p2shPrefix: String
        get() = when (this) {
            MAINNET -> "t3"
            TESTNET -> "t2" // TODO: verify actual testnet prefix
            REGTEST -> "t3" // TODO: verify actual regtest prefix
        }

    private val p2pkhPrefix: String
        get() = when (this) {
            MAINNET -> "t1"
            TESTNET, REGTEST -> "tm" // TODO: verify actual prefixes
        }

    private val texPrefix: String
        get() = when (this) {
            MAINNET -> "tex"
            TESTNET -> "textest"
            REGTEST -> "texregtest"
        }
    private val trasparentAddressMinimumLength: Int = 35

    private val saplingAddressMinimumLength: Int
        get() = when (this) {
            MAINNET -> 78
            TESTNET -> 88
            REGTEST -> 91
        }

    private val unifiedAddressMinimumLength: Int
        get() = when (this) {
            MAINNET -> 141
            TESTNET -> 154
            REGTEST -> 154
        }

    private val texAddressLength: Int
        get() = when (this) {
            MAINNET -> 42
            TESTNET -> 46
            REGTEST -> 49
        }
    fun isValid(address: String): Boolean {
        if (!address.isAsciiAlphanumeric()) return false
        if (isSprout(address)) return false
        return isTransparent(address) || isShielded(address)
    }

    fun isTex(address: String): Boolean {
        if (!address.isAsciiAlphanumeric()) return false

        return address.length >= texAddressLength &&
                address.startsWith(texPrefix)
    }
    fun isTransparent(address: String): Boolean {
        if (!address.isAsciiAlphanumeric()) return false
        return address.length >= trasparentAddressMinimumLength &&
                address.startsWith(p2pkhPrefix) ||
                address.startsWith(p2shPrefix) ||
                address.startsWith(texPrefix)

    }

    fun isSprout(address: String): Boolean {
        return address.startsWith(sproutPrefix) &&
                !address.startsWith("ztestsapling")
    }

    fun isSapling(address: String): Boolean {
        if (!address.isAsciiAlphanumeric()) return false
        return address.length >= saplingAddressMinimumLength &&
                address.startsWith(saplingPrefix)
    }

    fun isUnified(address: String): Boolean {
        if (!address.isAsciiAlphanumeric()) return false
        return address.length >= unifiedAddressMinimumLength &&
                address.startsWith(unifiedPrefix)
    }

    fun isShielded(address: String): Boolean {
        return isSapling(address) ||
                isUnified(address)
    }
}

fun String.isAsciiAlphanumeric(): Boolean {
    return all { it.code in 0..127 && it.isLetterOrDigit() }
}