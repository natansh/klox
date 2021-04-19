package com.ionsofimagination.klox

sealed class Expr {
    data class Binary(
        val left: Expr,
        val operator: Token,
        val right: Expr
    )
    data class Grouping(
        val expression: Expr
    )
    data class Literal(
        val value: Any
    )
    data class Unary(
        val operator: Token,
        val right: Expr
    )
}
