package com.ionsofimagination.tool

import java.io.PrintWriter
import kotlin.system.exitProcess

// Kotlin makes the creation of data classes far easier than Java does, hence this code to generate a file doesn't seem
// necessary. Nonetheless, writing it such that it's easier to follow the flow of the book in future chapters.
class GenerateAst {
    companion object {
        fun main(args: Array<String>) {
            if (args.size != 1) {
                System.err.println("Usage: generate_ast <output_directory>")
                exitProcess(64)
            }
            val outputDir = args[0]
            defineAst(
                outputDir, "Expr", listOf(
                    "Binary: Expr left, Token operator, Expr right",
                    "Call: Expr callee, Token paren, List<Expr> arguments", // The `paren` token is used for reporting runtime errors caused by the function call.
                    "Get: Expr obj, Token name",
                    "Grouping: Expr expression",
                    // Using "Any?" instead of "Object" in Kotlin.
                    "Literal: Any? value",
                    "Assign: Token name, Expr value",
                    "Logical: Expr left, Token operator, Expr right",
                    "Set: Expr obj, Token name, Expr value",
                    "This: Token keyword",
                    "Unary: Token operator, Expr right",
                    "Variable: Token name",
                    "Super: Token keyword, Token method",
                )
            )
            defineAst(
                outputDir, "Stmt", listOf(
                    "Expression: Expr expression",
                    "Function: Token name, List<Token> params, List<Stmt> body",
                    "If: Expr expression, Stmt thenBranch, Stmt? elseBranch",
                    "While: Expr condition, Stmt body",
                    "Print: Expr expression",
                    "Block: List<Stmt> statements",
                    "Return: Token keyword, Expr? value", // `keyword` token used for error reporting
                    "Var: Token name, Expr? initializer",
                    // The grammar restricts the superclass clause to a single identifier, but at runtime, that identifier is evaluated as a variable access. Wrapping the name in an Expr.Variable early on in the parser gives us an object that the resolver can hang the resolution information off of.
                    "Class: Token name, Expr.Variable? superclass, List<Stmt.Function> methods"
                )
            )
        }

        private const val indent = "    "

        // The types are still defined Java-style, but are converted into Kotlin code through string manipulation.
        private fun defineAst(outputDir: String, baseName: String, types: List<String>) {
            val path: String = "$outputDir/$baseName.kt"
            val writer = PrintWriter(path, "UTF-8")
            writer.apply {
                println("package com.ionsofimagination.klox")
                println()
                println("sealed class $baseName {")
                defineVisitor(this, baseName, types)
                for (type in types) {
                    val className = type.split(":")[0].trim()
                    val fields = type.split(":")[1].trim()
                    defineType(this, baseName, className, fields)
                }
                println()
                println("${indent}abstract fun <R> accept(visitor: Visitor<R>): R")
                println("}")
                close()
            }
        }

        private fun defineVisitor(writer: PrintWriter, baseName: String, types: List<String>) {
            writer.apply {
                println("${indent}interface Visitor<R> {")
                for (type in types) {
                    val className = type.split(":")[0].trim()
                    println("$indent${indent}fun visit$className$baseName(${baseName.toLowerCase()}: $className): R")
                }
                println("$indent}")
                println()
            }
        }

        private fun defineType(writer: PrintWriter, baseName: String, className: String, fieldList: String) {
            val fields = fieldList.split(", ")
            writer.apply {
                println("${indent}class $className(")
                println(fields.joinToString(",\n") { field ->
                    val fieldType = field.split(" ")[0].trim()
                    val fieldName = field.split(" ")[1].trim()
                    // To ensure proper indentation, indent by 8 spaces.
                    "$indent${indent}val $fieldName: $fieldType"
                })
                println("$indent) : $baseName() {")
                println("$indent${indent}override fun <R> accept(visitor: Visitor<R>): R = visitor.visit$className$baseName(this)")
                println("$indent}")
                println()
            }
        }
    }
}