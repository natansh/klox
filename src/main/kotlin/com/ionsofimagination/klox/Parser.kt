package com.ionsofimagination.klox

import com.ionsofimagination.klox.Stmt.While
import java.util.*


class Parser(private val tokens: List<Token>) {
    private class ParseError : RuntimeException()
    private var current: Int = 0

    fun parse(): List<Stmt> {
        val statements: MutableList<Stmt> = mutableListOf()
        while (!isAtEnd()) {
            declaration()?.let { statements.add(it) }
        }
        return statements
    }

    private fun declaration(): Stmt? {
        try {
            if (match(TokenType.VAR)) return varDeclaration()
            return statement()
        } catch (error: ParseError) {
            synchronize()
            return null
        }
    }

    private fun varDeclaration(): Stmt {
        val name = consume(TokenType.IDENTIFIER, "Expect variable name.")
        var initializer: Expr? = null
        if (match(TokenType.EQUAL)) {
            initializer = expression()
        }
        consume(TokenType.SEMICOLON, "Expect ';' after variable declaration.")
        return Stmt.Var(name, initializer)
    }

    private fun statement(): Stmt {
        return if (match(TokenType.PRINT)) {
            printStatement()
        } else if (match(TokenType.LEFT_BRACE)) {
            return Stmt.Block(block())
        } else if (match(TokenType.IF)) {
            ifStatement()
        } else if (match(TokenType.WHILE)) {
            whileStatement()
        } else if (match(TokenType.FOR)) {
            forStatement()
        } else {
            expressionStatement()
        }
    }

    private fun forStatement(): Stmt {
        consume(TokenType.LEFT_PAREN, "Expect '(' after 'for'.")
        val initializer: Stmt? = when {
            match(TokenType.SEMICOLON) ->  null
            match(TokenType.VAR) ->  varDeclaration()
            else ->  expressionStatement()
        }

        var condition: Expr? = null
        if (!check(TokenType.SEMICOLON)) {
            condition = expression()
        }
        consume(TokenType.SEMICOLON, "Expect ';' after condition")

        var increment: Expr? = null
        if (!check(TokenType.RIGHT_PAREN)) {
            increment = expression()
        }
        consume(TokenType.RIGHT_PAREN, "Expect ')' after for clauses.")
        var body = statement()
        if (increment != null) {
            body = Stmt.Block(
                listOf(
                    body,
                    Stmt.Expression(increment)
                )
            )
        }

        if (condition == null) condition = Expr.Literal(true)
        body = While(condition, body)

        if (initializer != null) {
            body = Stmt.Block(listOf(initializer, body))
        }
        return body
    }

    private fun ifStatement(): Stmt.If {
        consume(TokenType.LEFT_PAREN, "Expect '(' after if.")
        val condition = expression()
        consume(TokenType.RIGHT_PAREN, "Expect ')' after expression.")
        val thenStatement = statement()
        val elseStatement: Stmt? = if (match(TokenType.ELSE)) { statement() } else null
        return Stmt.If(condition, thenStatement, elseStatement)
    }

    private fun whileStatement(): While {
        consume(TokenType.LEFT_PAREN, "Expect '(' after while.")
        val condition = expression()
        consume(TokenType.RIGHT_PAREN, "Expect ')' after expression.")
        val body = statement()
        return While(condition, body)
    }

    private fun printStatement(): Stmt.Print {
        val value = expression()
        consume(TokenType.SEMICOLON, "Expected ';' after value")
        return Stmt.Print(value)
    }

    private fun block(): List<Stmt> {
        val statements: MutableList<Stmt> = ArrayList()
        while (!isAtEnd() && peek().type != TokenType.RIGHT_BRACE) {
            // It wasn't immediately clear how the Lox reference implementation was dealing with
            // null's arising in declaration.
            declaration()?.let { statements.add(it) }
        }
        consume(TokenType.RIGHT_BRACE, "Expect '}' after block.")
        return statements
    }

    private fun expressionStatement(): Stmt.Expression {
        val value = expression()
        consume(TokenType.SEMICOLON, "Expected ';' after value")
        return Stmt.Expression(value)
    }

    private fun expression(): Expr {
        return assignment()
    }


    private fun assignment(): Expr {
        val expr = logicOr()
        if (match(TokenType.EQUAL)) {
            val equals = previous()
            val value = logicOr()

            if (expr is Expr.Variable) {
                val name = expr.name
                return Expr.Assign(name, value)
            }
            error(equals, "Invalid assignment target.")
        }
        return expr
    }

    private fun logicOr(): Expr {
        var expr = logicAnd()
        while (match(TokenType.OR))  {
            expr = Expr.Logical(expr, previous(), logicAnd())
        }
        return expr
    }

    private fun logicAnd(): Expr {
        var expr = equality()
        while (match(TokenType.AND))  {
            expr = Expr.Logical(expr, previous(), equality())
        }
        return expr
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
            call()
        }

    private fun call(): Expr {
        var expr = primary()
        while (true) {
            if (match(TokenType.LEFT_PAREN)) {
                expr = finishCall(expr)
            } else {
                break
            }
        }
        return expr
    }

    private fun finishCall(callee: Expr): Expr {
        val arguments: MutableList<Expr> = ArrayList()
        if (!check(TokenType.RIGHT_PAREN)) {
            do {
                if (arguments.size >= 255) {
                    error(peek(), "Cannot have more than 255 arguments.")
                }
                arguments.add(expression())
            } while (match(TokenType.COMMA))
        }
        val paren = consume(
            TokenType.RIGHT_PAREN,
            "Expect ')' after arguments."
        )
        return Expr.Call(callee, paren, arguments)
    }

    private fun primary(): Expr {
        val token = peek()
        return when {
            match(TokenType.NIL) -> Expr.Literal(false)
            match(TokenType.TRUE) -> Expr.Literal(true)
            match(TokenType.NIL) -> Expr.Literal(null)
            match(TokenType.NUMBER) -> Expr.Literal(token.literal)
            match(TokenType.STRING) -> Expr.Literal(token.literal)
            match(TokenType.IDENTIFIER) -> Expr.Variable(token)
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
            return previous()
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
            if (previous().type == TokenType.SEMICOLON) return
            when (peek().type) {
                TokenType.CLASS,
                TokenType.FUN,
                TokenType.VAR,
                TokenType.FOR,
                TokenType.IF,
                TokenType.WHILE,
                TokenType.PRINT,
                TokenType.RETURN -> return
                else -> advance()
            }
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
    private fun check(tokenType: TokenType) = peek().type == tokenType
}
