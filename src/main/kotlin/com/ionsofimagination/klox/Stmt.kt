package com.ionsofimagination.klox

sealed class Stmt {
    interface Visitor<R> {
        fun visitExpressionStmt(stmt: Expression): R
        fun visitFunctionStmt(stmt: Function): R
        fun visitIfStmt(stmt: If): R
        fun visitWhileStmt(stmt: While): R
        fun visitPrintStmt(stmt: Print): R
        fun visitBlockStmt(stmt: Block): R
        fun visitVarStmt(stmt: Var): R
    }
    data class Expression(
        val expression: Expr
    ): Stmt() {
        override fun <R> accept(visitor: Visitor<R>): R = visitor.visitExpressionStmt(this)
    }
    data class Function(
        val name: Token,
        val params: List<Token>,
        val body: List<Stmt>
    ): Stmt() {
        override fun <R> accept(visitor: Visitor<R>): R = visitor.visitFunctionStmt(this)
    }
    data class If(
        val expression: Expr,
        val thenBranch: Stmt,
        val elseBranch: Stmt?
    ): Stmt() {
        override fun <R> accept(visitor: Visitor<R>): R = visitor.visitIfStmt(this)
    }
    data class While(
        val condition: Expr,
        val body: Stmt
    ): Stmt() {
        override fun <R> accept(visitor: Visitor<R>): R = visitor.visitWhileStmt(this)
    }
    data class Print(
        val expression: Expr
    ): Stmt() {
        override fun <R> accept(visitor: Visitor<R>): R = visitor.visitPrintStmt(this)
    }
    data class Block(
        val statements: List<Stmt>
    ): Stmt() {
        override fun <R> accept(visitor: Visitor<R>): R = visitor.visitBlockStmt(this)
    }
    data class Var(
        val name: Token,
        val initializer: Expr?
    ): Stmt() {
        override fun <R> accept(visitor: Visitor<R>): R = visitor.visitVarStmt(this)
    }

    abstract fun <R> accept(visitor: Visitor<R>): R
}
