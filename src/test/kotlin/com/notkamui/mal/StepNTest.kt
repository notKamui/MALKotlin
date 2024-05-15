package com.notkamui.mal

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldMatch
import org.opentest4j.TestAbortedException

fun getInput(step: Int): List<Pair<String, String>> {
    val folder = StepNTest::class.java.getResource("/test-files")?.readText()
        ?: throw IllegalStateException("Test files not found")
    val testFileName = folder.lines().first { it.startsWith("step$step") }
    val testFile = StepNTest::class.java.getResource("/test-files/$testFileName")?.readText()
        ?: throw IllegalStateException("Test file not found")
    return testFile
        .lines()
        .filter { it.isNotBlank() && !it.startsWith(";;") && !it.startsWith(";>>>") }
        .chunked(2)
        .map { (input, expected) -> input to expected }
        .also { println(it) }
}

abstract class StepNTest(step: Int, process: (String) -> String) : StringSpec({
    getInput(step).forEachIndexed { index, (input, expected) ->
        "test $index (input: $input | expected: $expected)" {
            val output = try {
                process(input)
            } catch (e: Exception) {
                e.printStackTrace()
                e.message ?: e.toString()
            }
            println("""
                |Input: $input
                |Output: $output
                |Expected: $expected
            """.trimMargin())
            when {
                expected.startsWith(";/") -> output shouldMatch expected.drop(2)
                    .replace("{", "\\{")
                    .replace("}", "\\}")

                expected.startsWith(";=>") -> output shouldBe expected.drop(3)
                else -> throw TestAbortedException("Invalid expected output: $expected")
            }
        }
    }
})