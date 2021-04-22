package com.ionsofimagination.tool

import java.io.PrintWriter
import kotlin.system.exitProcess

// Kotlin makes the creation of data classes far easier than Java does, hence this code to generate a file doesn't seem
// necessary. Nonetheless, writing it such that it's easier to follow the flow of the book in future chapters.
class GenerateAst {
    companion object {
        fun main(args: Array<String>) {
            val hello: List<String> = listOf()
            if (args.size != 1) {
                System.err.println("Usage: generate_ast <output_directory>")
                exitProcess(64)
            }
            val outputDir = args[0]
            defineAst(outputDir, "Expr", listOf(
                "Assign: Token name, Expr value",
                "Binary: Expr left, Token operator, Expr right",
                "Grouping: Expr expression",
                // Using "Any?" instead of "Object" in Kotlin.
                "Literal: Any? value",
                "Unary: Token operator, Expr right",
                "Variable: Token name",
                "Logical: Expr left, Token operator, Expr right",
            ))
            defineAst(outputDir, "Stmt", listOf(
                "Expression: Expr expression",
                "If: Expr expression, Stmt thenBranch, Stmt? elseBranch",
                "While: Expr condition, Stmt body",
                "Print: Expr expression",
                "Block: List<Stmt> expression",
                "Var: Token name, Expr? initializer"
            ))
        }

        private val indent = "    "

        // The types are still defined Java-style, but are converted into Kotlin code through string manipulation.
        fun defineAst(outputDir: String, baseName: String, types: List<String>) {
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
            }
        }

        private fun defineType(writer: PrintWriter, baseName: String, className: String, fieldList: String) {
            val fields = fieldList.split(", ")
            writer.apply {
                println("${indent}data class $className(")
                println(fields.joinToString(",\n") { field ->
                    val fieldType = field.split(" ")[0].trim()
                    val fieldName = field.split(" ")[1].trim()
                    // To ensure proper indentation, indent by 8 spaces.
                    "$indent${indent}val $fieldName: $fieldType"
                })
                println("$indent): $baseName() {")
                println("$indent${indent}override fun <R> accept(visitor: Visitor<R>): R = visitor.visit$className$baseName(this)")
                println("$indent}")
            }
        }
    }
}