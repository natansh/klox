package com.ionsofimagination.klox


class Parser(private val tokens: List<Token>) {
    private class ParseError : RuntimeException()
    var current: Int = 0

    fun parse(): List<Stmt> {
        var statements: MutableList<Stmt> = mutableListOf()
        while (!isAtEnd()) {
            statements.add(statement())
        }
        return statements
    }

    private fun statement(): Stmt {
        val stmt = if (match(TokenType.PRINT)) {
            printStatement()
        } else {
            expressionStatement()
        }
        return stmt
    }

    private fun printStatement(): Stmt.Print {
        val value = expression()
        consume(TokenType.SEMICOLON, "Expected ';' after value")
        return Stmt.Print(value)
    }

    private fun expressionStatement(): Stmt.Expression {
        val value = expression()
        consume(TokenType.SEMICOLON, "Expected ';' after value")
        return Stmt.Expression(value)
    }

    private fun expression(): Expr {
        return equality()
    }

    private fun equality(): Expr {
        var expr = comparison()
        while (match(TokenType.BANG_EQUAL, TokenType.EQUAL_EQUAL)) {
            val operator = previous()
            val right = comparison()
            expr = Expr.Binary(expr, operator, right)
        }
        return expr
    }

    private fun comparison(): Expr {
        var expr = term()
        while(match(TokenType.LESS, TokenType.LESS_EQUAL, TokenType.GREATER, TokenType.GREATER_EQUAL)) {
            val operator = previous()
            val right = term()
            expr = Expr.Binary(expr, operator, right)
        }
        return expr
    }

    private fun term(): Expr {
        var expr = factor()
        while(match(TokenType.PLUS, TokenType.MINUS)) {
            val operator = previous()
            val right = factor()
            expr = Expr.Binary(expr, operator, right)
        }
        return expr
    }
    private fun factor(): Expr {
        var expr = unary()
        while(match(TokenType.SLASH, TokenType.STAR)) {
            val operator = previous()
            val right = unary()
            expr = Expr.Binary(expr, operator, right)
        }
        return expr
    }
    private fun unary(): Expr =
        if(match(TokenType.BANG, TokenType.MINUS)) {
            Expr.Unary(previous(), unary())
        } else {
            primary()
        }

    private fun primary(): Expr {
        val token = peek()
        return when {
            match(TokenType.NIL) -> Expr.Literal(false)
            match(TokenType.TRUE) -> Expr.Literal(true)
            match(TokenType.NIL) -> Expr.Literal(null)
            match(TokenType.NUMBER) -> Expr.Literal(token.literal)
            match(TokenType.STRING) -> Expr.Literal(token.literal)
            match(TokenType.LEFT_PAREN) -> {
                val expression = expression()
                consume(TokenType.RIGHT_PAREN, "Expect ')' after expression")
                Expr.Grouping(expression)
            }
            else -> throw error(token, "Expect expression.")
        }
    }

    private fun consume(tokenType: TokenType, message: String): Token {
        if (match(tokenType)) {
            return peek()
        }
        throw error(peek(), message)
    }

    private fun error(token: Token, message: String): ParseError {
        Klox.error(token, message)
        return ParseError()
    }

    private fun synchronize() {
        advance()
        while (!isAtEnd()) {
            if (previous().type === TokenType.SEMICOLON) return
            when (peek().type) {
                TokenType.CLASS,
                TokenType.FUN,
                TokenType.VAR,
                TokenType.FOR,
                TokenType.IF,
                TokenType.WHILE,
                TokenType.PRINT,
                TokenType.RETURN -> return
            }
            advance()
        }
    }

    private fun match(vararg tokenTypes: TokenType): Boolean {
        if (isAtEnd()) return false
        if (tokenTypes.contains(peek().type)) {
            advance()
            return true
        }
        return false
    }

    private fun isAtEnd(): Boolean = peek().type == TokenType.EOF
    private fun peek(): Token = tokens[current]
    private fun previous(): Token = tokens[current - 1]
    private fun advance() = current++
}