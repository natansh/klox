package com.ionsofimagination.klox

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract


class Interpreter: Expr.Visitor<Any?>, Stmt.Visitor<Unit> {

    fun interpret(statements: List<Stmt>) {
        try {
            for (statement in statements) {
                evaluate(statement)
            }
        } catch (error: RuntimeError) {
            Klox.runtimeError(error)
        }
    }

    fun stringify(obj: Any?): String {
        if (obj == null) return "nil"
        if (obj is Double) {
            val text = obj.toString()
            if (text.endsWith(".0")) {
               return text.substring(0, endIndex = text.length - 2)
            }
            return text
        }
        return obj.toString()
    }

    override fun visitBinaryExpr(expr: Expr.Binary): Any? {
        val left = evaluate(expr.left)
        val right = evaluate(expr.right)
        return when (expr.operator.type) {
            TokenType.PLUS -> {
                if (left is Double && right is Double) {
                    left + right
                } else if (left is String && right is String) {
                    left + right
                } else {
                    throw RuntimeError(
                        expr.operator,
                        "Operands must be two numbers or two strings."
                    )
                }
            }
            TokenType.MINUS -> {
                checkNumberOperands(expr.operator, left, right)
                left - right
            }
            TokenType.STAR -> {
                checkNumberOperands(expr.operator, left, right)
                left * right
            }
            TokenType.SLASH -> {
                checkNumberOperands(expr.operator, left, right)
                left / right
            }
            TokenType.GREATER -> {
                checkNumberOperands(expr.operator, left, right)
                left > right
            }
            TokenType.GREATER_EQUAL -> {
                checkNumberOperands(expr.operator, left, right)
                left >= right
            }
            TokenType.LESS -> {
                checkNumberOperands(expr.operator, left, right)
                left < right
            }
            TokenType.LESS_EQUAL -> {
                checkNumberOperands(expr.operator, left, right)
                left <= right
            }
            // https://kotlinlang.org/docs/equality.html#referential-equality
            // == works as a?.equals(b) ?: (b === null), which is what we need.
            TokenType.BANG_EQUAL -> left != right // Kotlin does the right thing here for null
            TokenType.EQUAL_EQUAL -> left == right // Kotlin does the right thing here for null
            else -> throw Error()
        }
    }

    override fun visitGroupingExpr(expr: Expr.Grouping): Any? = evaluate(expr.expression)
    override fun visitLiteralExpr(expr: Expr.Literal): Any? = expr.value

    override fun visitUnaryExpr(expr: Expr.Unary): Any? {
        val right = evaluate(expr.right)
        return when (expr.operator.type) {
            TokenType.MINUS -> {
                checkNumberOperand(expr.operator, right)
                -1.0 * right
            }
            TokenType.BANG -> !(isTruthy(right))
            else -> throw Error() // TODO: Fix this error.
        }
    }


    // This is implemented differently in the book for Java. The use of Kotlin Contracts seemed more elegant here.
    // References:
    // 1. https://ncorti.com/blog/discovering-kotlin-contracts
    // 2. https://www.baeldung.com/kotlin/contracts
    @OptIn(ExperimentalContracts::class)
    private fun checkNumberOperand(operator: Token, operand: Any?) {
        contract {
            returns() implies (operand is Double)
        }
        if (operand is Double) return
        throw RuntimeError(operator, "Operand must be a number.")
    }

    @OptIn(ExperimentalContracts::class)
    private fun checkNumberOperands(operator: Token, left: Any?, right: Any?) {
        contract {
            returns() implies (left is Double && right is Double)
        }
        if (left is Double && right is Double) return
        throw RuntimeError(operator, "Operands must be numbers.")
    }

    // Lox follows Ruby’s simple rule: false and nil are false-ey, and everything else is truthy.
    private fun isTruthy(obj: Any?): Boolean {
        if (obj == null) return false
        if (obj is Boolean) return obj
        return true
    }

    private fun evaluate(stmt: Stmt) {
        stmt.accept(this)
    }

    private fun evaluate(expr: Expr): Any? {
        return expr.accept(this)
    }

    override fun visitExpressionStmt(stmt: Stmt.Expression) {
        // This might have side-effects.
        evaluate(stmt.expression)
    }

    override fun visitPrintStmt(stmt: Stmt.Print) {
        val value = evaluate(stmt.expression)
        println(stringify(value))
    }
}