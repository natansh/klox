package com.ionsofimagination.klox

class AstPrinter: Expr.Visitor<String> {
    fun print(expr: Expr): String {
        return expr.accept(this)
    }

    override fun visitBinaryExpr(expr: Expr.Binary): String {
        return "(${expr.operator.lexeme} ${print(expr.left)} ${print(expr.right)})"
    }

    override fun visitGroupingExpr(expr: Expr.Grouping): String {
        return "(group ${print(expr.expression)})"
    }

    override fun visitLiteralExpr(expr: Expr.Literal): String {
        return "${expr.value ?: "nil"}"
    }

    override fun visitUnaryExpr(expr: Expr.Unary): String {
        return "(${expr.operator.lexeme} ${print(expr.right)})"
    }

    override fun visitVariableExpr(expr: Expr.Variable): String {
        TODO("Not yet implemented")
    }
}

// Temporary hack to test out AST Printing

fun main(args: Array<String>) {
    val expression: Expr = Expr.Binary(
        Expr.Unary(
            Token(TokenType.MINUS, "-", null, 1),
            Expr.Literal(123)
        ),
        Token(TokenType.STAR, "*", null, 1),
        Expr.Grouping(
            Expr.Literal(45.67)
        )
    )
    println(AstPrinter().print(expression))
}