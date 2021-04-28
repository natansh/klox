package com.ionsofimagination.klox

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract


class Interpreter: Expr.Visitor<Any?>, Stmt.Visitor<Unit> {
    // Why keep global as a separate variable? So that native/foreign functions can be added directly to the global
    // environment.
    val globals = Environment()
    private var environment = globals
    private val locals: MutableMap<Expr, Int> = HashMap()

    init {
        globals.define("clock", object : LoxCallable {
            override fun arity(): Int {
                return 0
            }

            override fun call(
                interpreter: Interpreter,
                arguments: List<Any?>
            ): Any? {
                return System.currentTimeMillis().toDouble() / 1000.0
            }

            override fun toString(): String {
                return "<native fn>"
            }
        })
    }

    fun interpret(statements: List<Stmt>) {
        try {
            for (statement in statements) {
                execute(statement)
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

    override fun visitVariableExpr(expr: Expr.Variable): Any? {
        return lookUpVariable(expr.name, expr)
    }

    private fun lookUpVariable(name: Token, expr: Expr): Any? {
        val distance = locals[expr]
        return if (distance != null) {
            environment.getAt(distance, name)
        } else {
            globals.get(name)
        }
    }

    override fun visitAssignExpr(expr: Expr.Assign): Any? {
        val value = evaluate(expr.value)
        val distance = locals[expr]
        if (distance != null) {
            environment.assignAt(value, expr.name, distance)
        } else {
            globals.assign(expr.name, value)
        }
        return value
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

    private fun execute(stmt: Stmt) {
        stmt.accept(this)
    }

    fun resolve(expr: Expr, depth: Int) {
        locals[expr] = depth
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


    override fun visitVarStmt(stmt: Stmt.Var) {
        val value = stmt.initializer?.let { evaluate(it) }
        environment.define(stmt.name.lexeme, value)
    }

    override fun visitIfStmt(stmt: Stmt.If) {
        if (isTruthy(evaluate(stmt.expression))) {
            execute(stmt.thenBranch)
        } else if (stmt.elseBranch != null) {
            execute(stmt.elseBranch)
        }
    }

    override fun visitBlockStmt(stmt: Stmt.Block) {
        executeBlock(stmt.statements, Environment(environment))
    }

    fun executeBlock(statements: List<Stmt>, environment: Environment) {
        val previous = this.environment
        try {
            this.environment = environment
            for (statement in statements) {
                execute(statement)
            }
        } finally {
            this.environment = previous
        }
    }

    override fun visitLogicalExpr(expr: Expr.Logical): Any? {
        val left = evaluate(expr.left)
        if (expr.operator.type === TokenType.OR) {
            if (isTruthy(left)) return left
        } else {
            if (!isTruthy(left)) return left
        }
        return evaluate(expr.right)
    }

    override fun visitWhileStmt(stmt: Stmt.While) {
        while (isTruthy(evaluate(stmt.condition))) {
            execute(stmt.body)
        }
    }

    override fun visitCallExpr(expr: Expr.Call): Any? {
        val callee = evaluate(expr.callee)
        val arguments = expr.arguments.map { evaluate(it) }
        if (callee is LoxCallable) {
            if (arguments.size != callee.arity()) {
                throw RuntimeError(
                    expr.paren, "Expected " +
                            callee.arity().toString() + " arguments but got " +
                            arguments.size.toString() + "."
                )
            }
            return callee.call(this, arguments)
        } else {
            throw RuntimeError(expr.paren, "Can only call functions and classes.")
        }
    }

    override fun visitFunctionStmt(stmt: Stmt.Function) {
        environment.define(stmt.name.lexeme, LoxFunction(stmt, environment))
    }

    override fun visitReturnStmt(stmt: Stmt.Return) =
        throw Return(stmt.value?.let { evaluate(it) })
}
