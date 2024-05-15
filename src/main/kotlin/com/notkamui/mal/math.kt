package com.notkamui.mal

fun MalType<*>.tryCastAsFloat(): Pair<Double, Boolean> = when (this) {
    is MalFloat -> value to true
    is MalInteger -> value.toDouble() to false
    else -> throw IllegalArgumentException("Cannot perform operation on non-numbers")
}

fun List<MalType<*>>.numberReduction(reduce: (Double, Double) -> Double): MalType<*> {
    if (isEmpty()) throw IllegalArgumentException("Cannot perform operation on empty list")
    var isFloat: Boolean
    val (first) = first().tryCastAsFloat().also { (_, newFloat) -> isFloat = newFloat }
    val rest = drop(1)
    val result = rest.fold(first) { acc, malType ->
        val (value, newFloat) = malType.tryCastAsFloat()
        isFloat = isFloat || newFloat
        reduce(acc, value)
    }
    return if (isFloat) MalFloat(result) else MalInteger(result.toInt())
}
