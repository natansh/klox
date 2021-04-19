package com.ionsofimagination.klox

import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.system.exitProcess

class Klox {
    companion object {
        private var hadError = false

        fun main(args: Array<String>) {
            // There are likely better utilities in Kotlin for dealing with arguments.
            when {
                args.size > 1 -> {
                    println("Usage: klox [script]")
                    exitProcess(64)
                }
                args.size == 1 -> runFile(args[0])
                else -> runPrompt()
            }
        }

        // Didn't add @Throws(IOException::class) everywhere because
        // this code isn't meant to interoperate with Java code.
        // Also - https://kotlinlang.org/docs/exceptions.html#checked-exceptions
        private fun runFile(path: String) {
            val bytes = Files.readAllBytes(Paths.get(path))
            run(String(bytes, Charset.defaultCharset()))
            // Indicate an error in the exit code.
            if (hadError) exitProcess(65)
        }

        private fun runPrompt() {
            val input = InputStreamReader(System.`in`)
            val reader = BufferedReader(input)
            while (true) {
                print("> ")
                val line = reader.readLine() ?: break
                run(line)
                hadError = false
            }
        }

        private fun run(source: String) {
            val scanner = Scanner(source)
            val tokens: List<Token> = scanner.scanTokens()

            // For now, just print the tokens.
            for (token in tokens) {
                println(token)
            }
        }

        fun error(line: Int, message: String) {
            report(line, "", message)
        }

        private fun report(
            line: Int,
            where: String,
            message: String
        ) {
            System.err.println(
                "[line $line] Error$where: $message"
            )
            hadError = true
        }
    }
}
