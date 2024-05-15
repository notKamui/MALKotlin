package com.notkamui.mal

object Interpreter3 {

    fun read(input: String): MalType<*> = input.readMAL()

    fun eval(input: MalType<*>, env: Env = STDLIB): MalType<*> = input.eval(env)

    fun print(input: MalType<*>): String = input.print(printReadability = true)

    fun rep(input: String): String = input
        .let(::read)
        .let(::eval)
        .let(::print)
}



fun main() {
    while (true) {
        print("user> ")
        val input = readlnOrNull() ?: break
        if (input == "\\exit") break
        println(Interpreter1.rep(input))
    }
}