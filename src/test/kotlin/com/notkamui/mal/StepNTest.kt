package com.notkamui.mal

import io.kotest.core.spec.style.StringSpec
import io.kotest.core.spec.style.scopes.StringSpecRootScope
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldMatch
import org.opentest4j.TestAbortedException

abstract class StepNTest(step: Int, process: (String, Env) -> String) : StringSpec({
    val env = STDLIB
    getInput(step).forEachIndexed { index, (input, expected) ->
        if (expected != null) {
            test(index, input, expected, env, process)
        } else {
            step(index, input, env, process)
        }
    }
}) {
    constructor(step: Int, process: (String) -> String) : this(step, { input, _ -> process(input) })
}

private fun StringSpecRootScope.test(
    index: Int,
    input: String,
    expected: String,
    env: Env,
    process: (String, Env) -> String,
) {
    "#$index test: input> $input | expected> $expected" {
        val output = try {
            process(input, env)
        } catch (e: Exception) {
            e.printStackTrace()
            e.message ?: e.toString()
        }
        println(
            """
                |Input: $input
                |Output: $output
                |Expected: $expected
            """.trimMargin()
        )
        when {
            expected.startsWith(";/") -> output shouldMatch expected.drop(2)
                .replace("{", "\\{")
                .replace("}", "\\}")

            expected.startsWith(";=>") -> output shouldBe expected.drop(3)
            else -> throw TestAbortedException("Invalid expected output: $expected")
        }
    }
}

private fun StringSpecRootScope.step(
    index: Int,
    input: String,
    env: Env,
    process: (String, Env) -> String,
) {
    "#$index step: input> $input" {
        val output = try {
            process(input, env)
        } catch (e: Exception) {
            e.printStackTrace()
            e.message ?: e.toString()
        }
        println(
            """
                |Input: $input
                |Output: $output
            """.trimMargin()
        )
    }
}

private fun getInput(step: Int): List<Pair<String, String?>> {
    val folder = StepNTest::class.java.getResource("/test-files")?.readText()
        ?: throw IllegalStateException("Test files not found")
    val testFileName = folder.lines().first { it.startsWith("step$step") }
    val testFile = StepNTest::class.java.getResource("/test-files/$testFileName")?.readText()
        ?: throw IllegalStateException("Test file not found")
    return testFile
        .lines()
        .filter { it.isNotBlank() && !it.startsWith(";;") && !it.startsWith(";>>>") }
        .zipWithNext()
        .mapNotNull { (input, expected) ->
            if (input.startsWith(";")) null
            else input to expected.takeIf { it.startsWith(";") }
        }
        .also { println(it) }
}