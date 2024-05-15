package com.notkamui.mal

sealed interface MalType<T> {
    val value: T
    val metadata: MalType<*>
    fun print(printReadability: Boolean = false): String
    fun withMetadata(metadata: MalType<*>): MalType<T>
}

abstract class AbstractMalType<T> : MalType<T> {
    override var metadata: MalType<*> = MalNil
    override fun withMetadata(metadata: MalType<*>): MalType<T> = apply { this.metadata = metadata }
}

sealed interface MalKeyType {
    fun key(): String
}

data object MalUnit : MalType<Unit> {
    override val value = Unit
    override val metadata = MalNil
    override fun print(printReadability: Boolean): String = "()"
    override fun withMetadata(metadata: MalType<*>): MalType<Unit> = apply {
        throw UnsupportedOperationException("Unit cannot have metadata")
    }
}

data class MalFunction(
    val name: MalSymbol,
    val body: (List<MalType<*>>) -> MalType<*>,
) : AbstractMalType<MalType<*>>(), MalType<MalType<*>> {
    override val value: MalType<*> = MalUnit
    override fun print(printReadability: Boolean): String {
        return "(fn $name ...)"
    }

    companion object {
        fun of(params: String, body: (List<MalType<*>>) -> MalType<*>): MalFunction = MalFunction(MalSymbol(params), body)
    }
}

typealias MalCollectionValue = Collection<MalType<*>>

open class MalList(override val value: MalCollectionValue) : AbstractMalType<MalCollectionValue>(),
    MalType<MalCollectionValue> {
    override fun print(printReadability: Boolean): String =
        value.joinToString(separator = " ", prefix = "(", postfix = ")") { it.print() }
}

fun MalCollectionValue.toMalList() = MalList(this)

data class MalVector(override val value: MalCollectionValue) : AbstractMalType<MalCollectionValue>(),
    MalType<MalCollectionValue> {
    override fun print(printReadability: Boolean): String =
        value.joinToString(separator = " ", prefix = "[", postfix = "]") { it.print() }
}

fun MalCollectionValue.toMalVector() = MalVector(this)

typealias MalMapValue = Map<MalKeyType, MalType<*>>

data class MalHashMap(override val value: MalMapValue) : AbstractMalType<MalMapValue>(), MalType<MalMapValue> {
    override fun print(printReadability: Boolean): String =
        value.entries.joinToString(separator = " ", prefix = "{", postfix = "}") { (k, v) ->
            "${k.key()} ${v.print()}"
        }
}

data class MalSymbol(override val value: String) : AbstractMalType<String>(), MalType<String> {
    override fun print(printReadability: Boolean): String = value
}

fun String.toMal() = MalSymbol(this)

data class MalInteger(override val value: Int) : AbstractMalType<Int>(), MalType<Int> {
    override fun print(printReadability: Boolean): String = value.toString()
}

fun Int.toMal() = MalInteger(this)

data class MalFloat(override val value: Double) : AbstractMalType<Double>(), MalType<Double> {
    override fun print(printReadability: Boolean): String = value.toString()
}

fun Double.toMal() = MalFloat(this)

data class MalString(override val value: String) : AbstractMalType<String>(), MalType<String>, MalKeyType {
    override fun print(printReadability: Boolean): String = if (printReadability) "\"${
        value
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
    }\"" else value

    override fun key(): String = "\"$value\""
}

fun String.toMalString() = this
    .also { token ->
        val cleaned = token
            .replace("\\\\", "")
            .replace("\\n", "")
            .replace("\\\"", "")
        if (cleaned.length < 2 || cleaned[0] != '"' || cleaned[cleaned.length - 1] != '"') {
            throw IllegalArgumentException("EOF while reading string $cleaned")
        }
    }
    .replace("\\\"", "\"")
    .replace("\\n", "\n")
    .replace("\\\\", "\\")
    .drop(1)
    .dropLast(1)
    .let(::MalString)

data class MalKeyword(override val value: String) : AbstractMalType<String>(), MalType<String>, MalKeyType {
    override fun print(printReadability: Boolean): String = ":$value"
    override fun key(): String = ":$value"
}

fun String.toMalKeyword() = MalKeyword(this)

data class MalMacro(
    override val value: MalCollectionValue,
    val actual: MalType<*>,
    val symbol: MalSymbol
) : MalList(value), MalType<MalCollectionValue> {
    override fun print(printReadability: Boolean): String {
        val sSymbol = symbol.print(printReadability)
        val sValue = actual.print(printReadability)
        val sMetadata = if (metadata != MalNil) " " + metadata.print(printReadability) else ""
        return "($sSymbol $sValue$sMetadata)"
    }

    companion object {
        fun from(
            symbol: MalSymbol,
            value: MalType<*>,
            vararg values: MalType<*>
        ): MalMacro = MalMacro(listOf(symbol, value) + values.toList(), value, symbol)
    }
}

data class MalBoolean(override val value: Boolean) : AbstractMalType<Boolean>(), MalType<Boolean> {
    override fun print(printReadability: Boolean): String = value.toString()
}

fun Boolean.toMal() = MalBoolean(this)

data object MalNil : MalType<Nothing> {
    override val metadata = MalNil
    override val value: Nothing get() = throw NullPointerException("nil has no value")
    override fun print(printReadability: Boolean): String = "nil"
    override fun withMetadata(metadata: MalType<*>): MalType<Nothing> = apply {
        throw UnsupportedOperationException("nil cannot have metadata")
    }
}