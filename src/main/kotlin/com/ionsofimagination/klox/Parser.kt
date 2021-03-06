package com.ionsofimagination.klox

import com.ionsofimagination.klox.Stmt.Return
import com.ionsofimagination.klox.Stmt.While


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
            if (match(TokenType.FUN)) return function("function")
            if (match(TokenType.VAR)) return varDeclaration()
            if (match(TokenType.CLASS)) return classDeclaration()
            return statement()
        } catch (error: ParseError) {
            synchronize()
            return null
        }
    }

    private fun classDeclaration(): Stmt {
        val name = consume(TokenType.IDENTIFIER, "Expect class name.")
        var superclass: Expr.Variable? = null
        if (match(TokenType.LESS)) {
            consume(TokenType.IDENTIFIER, "Expect superclass name.")
            superclass = Expr.Variable(previous())
        }
        consume(TokenType.LEFT_BRACE, "Expect '{' before class body.")
        val methods = mutableListOf<Stmt.Function>()
        while (!isAtEnd() && !check(TokenType.RIGHT_BRACE)) {
            methods.add(function("method"))
        }
        consume(TokenType.RIGHT_BRACE, "Expect '}' after class body.")
        return Stmt.Class(name, superclass, methods)
    }

    // `kind` param allows `function` to be reused later for parsing methods inside classes.
    private fun function(kind: String): Stmt.Function {
        val name = consume(TokenType.IDENTIFIER, "Expect $kind name.")
        consume(TokenType.LEFT_PAREN, "Expect '(' after function name.")
        val parameters = mutableListOf<Token>()
        // Check the 0-parameter case.
        if (!check(TokenType.RIGHT_PAREN)) {
            do {
                if (parameters.size >= 255) {
                    error(peek(), "Can't have more than 255 parameters.")
                }
                parameters.add(consume(TokenType.IDENTIFIER, "Expected identifier as a param name"))
            } while (match(TokenType.COMMA))
        }
        consume(TokenType.RIGHT_PAREN, "Expect ')' after parameters.")
        // Calling `block()` assumes that the left brace has already been parsed. Hence, parse the left brace beforehand.
        consume(TokenType.LEFT_BRACE, "Expect '{' before $kind body.")
        val body = block()
        return Stmt.Function(name, parameters, body)
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
        return when {
            match(TokenType.PRINT) -> printStatement()
            match(TokenType.LEFT_BRACE) -> return Stmt.Block(block())
            match(TokenType.IF) -> ifStatement()
            match(TokenType.WHILE) -> whileStatement()
            match(TokenType.FOR) -> forStatement()
            match(TokenType.RETURN) -> returnStatement()
            else -> expressionStatement()
        }
    }

    private fun returnStatement(): Stmt {
        val keyword = previous()
        var value: Expr? = null
        if (!check(TokenType.SEMICOLON)) {
            value = expression()
        }
        consume(TokenType.SEMICOLON, "Expect ';' after return value.")
        return Return(keyword, value)
    }

    private fun forStatement(): Stmt {
        consume(TokenType.LEFT_PAREN, "Expect '(' after 'for'.")
        val initializer: Stmt? = when {
            match(TokenType.SEMICOLON) -> null
            match(TokenType.VAR) -> varDeclaration()
            else -> expressionStatement()
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
        val elseStatement: Stmt? = if (match(TokenType.ELSE)) {
            statement()
        } else null
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
        consume(TokenType.SEMICOLON, "Expect ';' after expression.")
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
        consume(TokenType.SEMICOLON, "Expect ';' after expression.")
        return Stmt.Expression(value)
    }

    private fun expression(): Expr {
        return assignment()
    }

    private fun assignment(): Expr {
        val expr = logicOr()
        if (match(TokenType.EQUAL)) {
            val equals = previous()
            val value = assignment()

            if (expr is Expr.Variable) {
                val name = expr.name
                return Expr.Assign(name, value)
            } else if (expr is Expr.Get) {
                return Expr.Set(expr.obj, expr.name, value)
            }
            error(equals, "Invalid assignment target.")
        }
        return expr
    }

    private fun logicOr(): Expr {
        var expr = logicAnd()
        while (match(TokenType.OR)) {
            expr = Expr.Logical(expr, previous(), logicAnd())
        }
        return expr
    }

    private fun logicAnd(): Expr {
        var expr = equality()
        while (match(TokenType.AND)) {
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
        while (match(TokenType.LESS, TokenType.LESS_EQUAL, TokenType.GREATER, TokenType.GREATER_EQUAL)) {
            val operator = previous()
            val right = term()
            expr = Expr.Binary(expr, operator, right)
        }
        return expr
    }

    private fun term(): Expr {
        var expr = factor()
        while (match(TokenType.PLUS, TokenType.MINUS)) {
            val operator = previous()
            val right = factor()
            expr = Expr.Binary(expr, operator, right)
        }
        return expr
    }

    private fun factor(): Expr {
        var expr = unary()
        while (match(TokenType.SLASH, TokenType.STAR)) {
            val operator = previous()
            val right = unary()
            expr = Expr.Binary(expr, operator, right)
        }
        return expr
    }

    private fun unary(): Expr =
        if (match(TokenType.BANG, TokenType.MINUS)) {
            Expr.Unary(previous(), unary())
        } else {
            call()
        }

    private fun call(): Expr {
        var expr = primary()
        while (true) {
            expr = if (match(TokenType.LEFT_PAREN)) {
                finishCall(expr)
            } else if (match(TokenType.DOT)) {
                val name = consume(TokenType.IDENTIFIER, "Expect property name after '.'.")
                Expr.Get(expr, name)
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
                    error(peek(), "Can't have more than 255 arguments.")
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
            match(TokenType.TRUE) -> Expr.Literal(true)
            match(TokenType.FALSE) -> Expr.Literal(false)
            match(TokenType.NIL) -> Expr.Literal(null)
            match(TokenType.NUMBER) -> Expr.Literal(token.literal)
            match(TokenType.STRING) -> Expr.Literal(token.literal)
            match(TokenType.THIS) -> Expr.This(token)
            match(TokenType.IDENTIFIER) -> Expr.Variable(token)
            match(TokenType.SUPER) -> {
                val keyword = previous()
                consume(TokenType.DOT, "Expect '.' after 'super'.")
                Expr.Super(keyword, consume(TokenType.IDENTIFIER, "Expect superclass method name."))
            }
            match(TokenType.LEFT_PAREN) -> {
                val expression = expression()
                consume(TokenType.RIGHT_PAREN, "Expect ')' after expression")
                Expr.Grouping(expression)
            }
            else -> throw error(token, "Expect expression.")
        }
    }

    private fun consume(tokenType: TokenType, message: String): Token {
        if (check(tokenType)) {
            return advance()
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
    private fun advance(): Token {
        if (!isAtEnd()) current++
        return previous()
    }

    private fun check(tokenType: TokenType) = peek().type == tokenType
}
