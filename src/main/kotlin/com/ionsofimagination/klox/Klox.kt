package com.ionsofimagination.klox

import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.system.exitProcess

class Klox {
    companion object {
        private var interpreter = Interpreter()
        private var hadError = false
        private var hadRuntimeError = false

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
            if (hadRuntimeError) exitProcess(70)
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
            val parser = Parser(tokens)
            val statements = parser.parse()
            // Stop if there was a syntax error.
            if (hadError) return
            val resolver = Resolver(interpreter)
            resolver.resolve(statements)
            interpreter.interpret(statements)
        }

        fun error(line: Int, message: String) {
            report(line, "", message)
        }

        fun error(token: Token, message: String?) {
            if (token.type === TokenType.EOF) {
                report(token.line, " at end", message!!)
            } else {
                report(token.line, " at '" + token.lexeme + "'", message!!)
            }
        }

        fun runtimeError(error: RuntimeError) {
            System.err.println("${error.message}\n[line ${error.token.line}]");
            hadRuntimeError = true
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
