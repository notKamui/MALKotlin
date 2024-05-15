package com.notkamui.mal

fun MalType<*>.eval(env: Env): MalType<*> =
    if (this is MalList && value.isNotEmpty()) {
        val evaluated = preprocess(env) as MalList
        if (evaluated.value.first() !is MalFunction) throw IllegalArgumentException("First element is not a function")
        val fn = evaluated.value.first() as MalFunction
        val args = evaluated.value.drop(1)
        fn.body(args)
    } else preprocess(env)

fun MalType<*>.preprocess(env: Env): MalType<*> = when (this) {
    is MalSymbol -> env[this] ?: throw IllegalArgumentException("Symbol not found: $this")
    is MalList -> value.map { it.eval(env) }.toMalList()
    is MalVector -> value.map { it.eval(env) }.toMalVector()
    is MalHashMap -> value.mapValues { it.value.eval(env) }.let(::MalHashMap)
    else -> this
}