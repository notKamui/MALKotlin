package com.notkamui.mal

typealias EnvMap = Map<MalSymbol, MalFunction>

class Env(
    private val parent: Env?,
    private val data: MutableMap<MalSymbol, MalFunction>
) : EnvMap by data {
    override fun get(key: MalSymbol): MalFunction? = data[key] ?: parent?.data?.get(key)

    fun set(key: MalSymbol, value: MalFunction) {
        data[key] = value
    }

    fun set(key: String, value: MalFunction) {
        set(MalSymbol(key), value)
    }

    fun set(key: String, value: (List<MalType<*>>) -> MalType<*>) {
        set(key, MalFunction.of(key, value))
    }

    operator fun String.invoke(value: (List<MalType<*>>) -> MalType<*>) = set(this, value)

    companion object {
        fun new(parent: Env? = null, init: Env.() -> Unit): Env = Env(parent, mutableMapOf()).apply(init)
    }
}

val STDLIB = Env.new {
    "+" { args -> args.numberReduction(Double::plus) }
    "-" { args -> args.numberReduction(Double::minus) }
    "*" { args -> args.numberReduction(Double::times) }
    "/" { args -> args.numberReduction(Double::div) }
}