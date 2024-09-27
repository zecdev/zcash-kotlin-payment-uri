package org.zecdev.zcash.zip321.parser

data class IndexedParameter(
    val index: UInt,
    val param: Param
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as IndexedParameter

        if (index != other.index) return false
        if (param != other.param) return false

        return true
    }

    override fun hashCode(): Int {
        var result = index.hashCode()
        result = 31 * result + param.hashCode()
        return result
    }
}
