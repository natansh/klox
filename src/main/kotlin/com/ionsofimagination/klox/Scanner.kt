package com.ionsofimagination.klox

class Scanner(private val source: String) {
    private var tokens: MutableList<Token> = ArrayList()
    private var start = 0
    private var current = 0
    private var line = 1

    fun scanTokens(): List<Token> {
        while (!isAtEnd()) {
            start = current
            scanToken()
        }
        // When we run out of tokens and get to the end of file, the loop above will terminate, but we wouldn't have
        // specifically added a EOF token within that loop. Add it here.
        // The token isn't strictly needed, but it helps make the parser logic a bit more elegant.
        tokens.add(Token(TokenType.EOF, "", null, line))
        return tokens
    }

    private fun isAtEnd(): Boolean = current >= source.length

    private fun scanToken() {
        when (val c = advance()) {
            // Single-character tokens
            '(' -> addToken(TokenType.LEFT_PAREN)
            ')' -> addToken(TokenType.RIGHT_PAREN)
            '{' -> addToken(TokenType.LEFT_BRACE)
            '}' -> addToken(TokenType.RIGHT_BRACE)
            ',' -> addToken(TokenType.COMMA)
            '.' -> addToken(TokenType.DOT)
            '-' -> addToken(TokenType.MINUS)
            '+' -> addToken(TokenType.PLUS)
            ';' -> addToken(TokenType.SEMICOLON)
            '*' -> addToken(TokenType.STAR)
            // One or two character tokens.
            '!' -> addToken(if (match('=')) TokenType.BANG_EQUAL else TokenType.BANG)
            '=' -> addToken(if (match('=')) TokenType.EQUAL_EQUAL else TokenType.EQUAL)
            '<' -> addToken(if (match('=')) TokenType.LESS_EQUAL else TokenType.LESS)
            '>' -> addToken(if (match('=')) TokenType.GREATER_EQUAL else TokenType.GREATER)
            // Comments also begin with a /, hence we need some custom handling.
            '/' -> {
                // This matches comments.
                if (match('/')) {
                    while (peek() != '\n' && !isAtEnd()) advance()
                } else if (match('*')) {
                    addToken(TokenType.SLASH)
                }
            }
            // Ignore whitespace
            ' ', '\r', '\t' -> Unit
            // Ignore newlines, but do increment the line number.
            '\n' -> line += 1
            '"' -> string()
            else -> {
                // Numbers and identifiers are easier to check separately from the simpler cases.
                when {
                    isDigit(c) -> number()
                    isAlpha(c) -> identifier()
                    else -> Klox.error(line, "Unexpected character.")
                }
            }
        }
    }

    // Underscore is the only special character we allow
    private fun isAlpha(c: Char): Boolean = c in 'a'..'z' || c in 'A'..'Z' || c == '_'
    private fun isAlphaNumeric(c: Char): Boolean = isAlpha(c) || isDigit(c)

    private fun identifier() {
        while (isAlphaNumeric(peek())) advance()
        val text = source.substring(start, current)
        // Check if reserved keyword.
        var type = keywords[text]
        if (type == null) {
            type = TokenType.IDENTIFIER
        }
        addToken(type)
    }

    private fun isDigit(c: Char): Boolean = c in '0'..'9'

    private fun number() {
        while (isDigit(peek())) advance()
        if (peek() == '.' && isDigit(peekNext())) {
            advance()
            while (isDigit(peek())) advance()
        }
        addToken(TokenType.NUMBER, source.substring(start, current).toDouble())
    }


    private fun string() {
        // This seems like it can be rewritten to use advance, but this works too.
        while (peek() != '"' && !isAtEnd()) {
            // Note that a string can be multi-line. That means we can hit a newline while parsing a string.
            if (peek() == '\n') line += 1
            advance()
        }
        // Check for unterminated string.
        if (isAtEnd()) {
            Klox.error(line, "Unterminated string.")
            return
        }
        // Capture the closing "
        advance()
        // Note the potential for an off-by-one. Since we advanced earlier, we can
        // clip off the start and end *"*
        val value = source.substring(start + 1, current - 1)
        addToken(TokenType.STRING, value)
    }

    // This is a lookahead. Match does the same.
    private fun peek(): Char {
        if (isAtEnd()) return 0.toChar()
        return source[current]
    }

    // Still got to check if we're approaching the end.
    private fun peekNext(): Char {
        if (current + 1 >= source.length) return 0.toChar()
        return source[current + 1]
    }

    // Like a conditional advance. We only move if the character is what we expected.
    private fun match(expected: Char): Boolean {
        if (isAtEnd() || source[current] != expected) return false
        current += 1
        return true
    }

    private fun advance(): Char {
        // In Kotlin, use `String.get()` instead of `charAt`. This is implemented using the index/subscript operator []
        // too.
        // https://stackoverflow.com/questions/50297288/how-can-i-access-a-char-in-string-in-at-specific-number
        val char = source[current]
        current += 1
        return char
    }

    private fun addToken(type: TokenType, literal: Any? = null) {
        val lexeme = source.substring(start, current)
        tokens.add(Token(type, lexeme, literal, line))
    }

    companion object {
        val keywords: HashMap<String, TokenType> = HashMap()

        init {
            keywords.apply {
                put("and", TokenType.AND)
                put("class", TokenType.CLASS)
                put("else", TokenType.ELSE)
                put("false", TokenType.FALSE)
                put("for", TokenType.FOR)
                put("fun", TokenType.FUN)
                put("if", TokenType.IF)
                put("nil", TokenType.NIL)
                put("or", TokenType.OR)
                put("print", TokenType.PRINT)
                put("return", TokenType.RETURN)
                put("super", TokenType.SUPER)
                put("this", TokenType.THIS)
                put("true", TokenType.TRUE)
                put("var", TokenType.VAR)
                put("while", TokenType.WHILE)
            }
        }
    }
}