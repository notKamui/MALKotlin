package com.notkamui.mal

import java.util.LinkedList
import java.util.Map.entry

class Reader(private val tokens: List<String>) : Iterator<String> {
    private var position = 0

    override fun hasNext(): Boolean = position < tokens.size

    override fun next(): String {
        val token = peek()
        position++
        return token
    }

    fun peek(): String {
        if (!hasNext()) throw NoSuchElementException()
        return tokens[position]
    }
}

private val LEX_REGEX = """[\s,]*(~@|[\[\]{}()'`~^@]|"(?:\\.|[^\\"])*"?|;.*|[^\s\[\]{}('"`,;)]*)""".toRegex()
fun String.tokenizeMAL(): List<String> = LEX_REGEX.findAll(this)
    .map { it.groupValues[1] }
    .filter { it.isNotEmpty() && it[0] != ';' }
    .toList()

fun Reader.readCollection(
    provider: (MalCollectionValue) -> MalType<*>,
    unit: () -> MalType<*>,
    end: String,
    error: String
): MalType<*> = buildList {
    next()
    while (hasNext()) {
        if (peek() == end) {
            next()
            return@buildList
        }
        add(read())
    }
    throw IllegalStateException(error)
}.takeIf { it.isNotEmpty() }
    ?.let(provider)
    ?: unit()

fun Reader.readList(): MalType<*> = readCollection(
    { MalList(LinkedList(it)) },
    { MalUnit },
    ")",
    "EOF while reading list"
)

fun Reader.readVector(): MalType<*> = readCollection(
    ::MalVector,
    { MalVector(emptyList()) },
    "]",
    "EOF while reading vector"
)

fun Reader.readHashMap(): MalType<*> = readCollection(
    { elements -> MalHashMap(elements.chunked(2)
        .onEach { entry ->
            if (entry.size != 2) throw IllegalArgumentException("Hashmap must contain an even number of elements")
            if (entry[0] !is MalKeyType) throw IllegalArgumentException("Key must be a keyword or string")
        }
        .associate { (k, v) -> k as MalKeyType to v }) },
    { MalHashMap(emptyMap()) },
    "}",
    "EOF while reading hashmap"
)

fun Reader.readAtom(): MalType<*> {
    val token = next()
    return when {
        token.startsWith(":") -> token.drop(1).toMalKeyword()
        token.startsWith("\"") -> token.toMalString()
        token.startsWith("'") -> MalMacro.from(MalSymbol("quote"), read())
        token.startsWith("`") -> MalMacro.from(MalSymbol("quasiquote"), read())
        token.startsWith("~@") -> MalMacro.from(MalSymbol("splice-unquote"), read())
        token.startsWith("~") -> MalMacro.from(MalSymbol("unquote"), read())
        token.startsWith("^") -> {
            val meta = read()
            val value = read()
            MalMacro.from(MalSymbol("with-meta"), value, meta).withMetadata(meta)
        }
        token.startsWith("@") -> MalMacro.from(MalSymbol("deref"), read())
        token.toIntOrNull() != null -> token.toInt().toMal()
        token.toDoubleOrNull() != null -> token.toDouble().toMal()
        token == "true" -> true.toMal()
        token == "false" -> false.toMal()
        token == "nil" -> MalNil
        else -> token.toMal()
    }
}

fun Reader.read(): MalType<*> = when (peek()) {
    "(" -> readList()
    "[" -> readVector()
    "{" -> readHashMap()
    else -> readAtom()
}

fun String.readMAL(): MalType<*> = tokenizeMAL()
    .takeIf { it.isNotEmpty() }
    ?.let(::Reader)
    ?.read()
    ?: MalUnit
