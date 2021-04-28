package com.ionsofimagination.klox

import java.util.*


class Resolver(private val interpreter: Interpreter) : Expr.Visitor<Unit>, Stmt.Visitor<Unit> {
    private val scopes = Stack<MutableMap<String, Boolean>>()
    override fun visitBlockStmt(stmt: Stmt.Block) {
        beginScope()
        resolve(stmt.statements)
        endScope()
    }

    override fun visitVarStmt(stmt: Stmt.Var) {
        declare(stmt.name)
        if (stmt.initializer != null) {
            resolve(stmt.initializer)
        }
        define(stmt.name)
    }

    override fun visitVariableExpr(expr: Expr.Variable) {
        if (!scopes.isEmpty() &&
            scopes.peek()[expr.name.lexeme] == false) {
            Klox.error(expr.name,
                "Can't read local variable in its own initializer.");
        }

        resolveLocal(expr, expr.name);
    }

    private fun declare(token: Token) {
        if (scopes.isEmpty()) return
        // By marking as "false" we say that the resolution is not done yet.
        scopes.peek()[token.lexeme] = false
    }

    private fun define(token: Token) {
        if (scopes.isEmpty()) return
        // Once we define the variable, we're ready.
        scopes.peek()[token.lexeme] = true
    }

    private fun beginScope() {
        scopes.push(HashMap())
    }

    private fun endScope() {
        scopes.pop()
    }

    fun resolve(statements: List<Stmt>) {
        for (statement in statements) {
            resolve(statement)
        }
    }

    private fun resolve(statement: Stmt) {
       statement.accept(this)
    }

    private fun resolve(expr: Expr) {
        expr.accept(this)
    }

    private fun resolveLocal(expr: Expr, name: Token) {
        for (i in scopes.indices.reversed()) {
            if (scopes[i].containsKey(name.lexeme)) {
                interpreter.resolve(expr, scopes.size - 1 - i)
                return
            }
        }
    }

    override fun visitAssignExpr(expr: Expr.Assign) {
        resolve(expr.value)
        resolveLocal(expr, expr.name)
    }

    override fun visitBinaryExpr(expr: Expr.Binary) {
        resolve(expr.left)
        resolve(expr.right)
    }

    override fun visitCallExpr(expr: Expr.Call) {
        resolve(expr.callee)
        expr.arguments.forEach { resolve(it) }
    }

    override fun visitGroupingExpr(expr: Expr.Grouping) {
        resolve(expr.expression)
    }

    override fun visitLiteralExpr(expr: Expr.Literal) = Unit

    override fun visitUnaryExpr(expr: Expr.Unary) {
        resolve(expr.right)
    }

    override fun visitLogicalExpr(expr: Expr.Logical) {
        resolve(expr.left)
        resolve(expr.right)
    }

    override fun visitExpressionStmt(stmt: Stmt.Expression) {
        resolve(stmt.expression)
    }

    override fun visitFunctionStmt(stmt: Stmt.Function) {
        declare(stmt.name)
        define(stmt.name)
        resolveFunction(stmt)
    }

    private fun resolveFunction(stmt: Stmt.Function) {
        beginScope()
        stmt.params.forEach {
            declare(it)
            define(it)
        }
        resolve(stmt.body)
        endScope()
    }

    override fun visitIfStmt(stmt: Stmt.If) {
        resolve(stmt.expression)
        resolve(stmt.thenBranch)
        if (stmt.elseBranch != null) {
            resolve(stmt.elseBranch)
        }
    }

    override fun visitWhileStmt(stmt: Stmt.While) {
        resolve(stmt.condition)
        resolve(stmt.body)
    }

    override fun visitPrintStmt(stmt: Stmt.Print) {
        resolve(stmt.expression)
    }

    override fun visitReturnStmt(stmt: Stmt.Return) {
        stmt.value?.let { resolve(it) }
    }
}