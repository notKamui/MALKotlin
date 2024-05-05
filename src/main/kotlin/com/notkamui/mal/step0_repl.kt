package com.notkamui.mal

object Interpreter0 {

    fun read(input: String): String = input

    fun eval(input: String): String = input

    fun print(input: String): String = input

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
        println(Interpreter0.rep(input))
    }
}