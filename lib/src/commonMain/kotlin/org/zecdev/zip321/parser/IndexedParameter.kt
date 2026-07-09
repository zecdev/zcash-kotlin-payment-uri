package org.zecdev.zip321.parser

// `equals`/`hashCode` are the compiler-synthesized data class defaults (comparing `index` and
// `param` structurally); an earlier revision hand-wrote an equivalent override, which was deleted
// as redundant (K16).
internal data class IndexedParameter(
    val index: UInt,
    val param: Param,
)
