package org.zecdev.zip321.parser

enum class ParserContext {
    MAINNET,
    TESTNET,
    REGTEST;

    val sproutPrefix: String
        get() = when (this) {
            MAINNET -> "zc"
            TESTNET, REGTEST -> "zt"
        }

    val saplingPrefix: String
        get() = when (this) {
            MAINNET -> "zs"
            TESTNET -> "ztestsapling"
            REGTEST -> "zregtestsapling"
        }

    val unifiedPrefix: String
        get() = when (this) {
            MAINNET -> "u"
            TESTNET -> "utest"
            REGTEST -> "uregtest"
        }

    val p2shPrefix: String
        get() = when (this) {
            MAINNET -> "t3"
            TESTNET -> "t2" // TODO: verify actual testnet prefix
            REGTEST -> "t3" // TODO: verify actual regtest prefix
        }

    val p2pkhPrefix: String
        get() = when (this) {
            MAINNET -> "t1"
            TESTNET, REGTEST -> "tm" // TODO: verify actual prefixes
        }

    val texPrefix: String
        get() = when (this) {
            MAINNET -> "tex"
            TESTNET -> "textest"
            REGTEST -> "texregtest"
        }

    fun isValid(address: String): Boolean {
        if (!address.isAsciiAlphanumeric()) return false
        if (isSprout(address)) return false
        return isTransparent(address) || isShielded(address)
    }

    fun isTransparent(address: String): Boolean {
        if (!address.isAsciiAlphanumeric()) return false
        return address.startsWith(p2pkhPrefix) ||
                address.startsWith(p2shPrefix) ||
                address.startsWith(texPrefix)
    }

    fun isSprout(address: String): Boolean {
        return address.startsWith(sproutPrefix) &&
                !address.startsWith("ztestsapling")
    }

    fun isShielded(address: String): Boolean {
        return address.startsWith(saplingPrefix) ||
                address.startsWith(unifiedPrefix)
    }
}

fun String.isAsciiAlphanumeric(): Boolean {
    return all { it.code in 0..127 && it.isLetterOrDigit() }
}