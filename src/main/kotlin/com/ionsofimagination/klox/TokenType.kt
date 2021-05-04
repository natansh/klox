package com.ionsofimagination.klox

enum class TokenType {
    // Single-character tokens. These are easy to parse as we can match the single character directly.
    LEFT_PAREN, RIGHT_PAREN, LEFT_BRACE, RIGHT_BRACE, COMMA, DOT, MINUS, PLUS, SEMICOLON, SLASH, STAR,
    // One or two character tokens. We need to look at at least one more character to know what kind of token
    // it might be.
    BANG, BANG_EQUAL, EQUAL, EQUAL_EQUAL, GREATER, GREATER_EQUAL, LESS, LESS_EQUAL,
    // Literals. These are more complex than the ones above and can be matched manually by looping, or through regex.
    IDENTIFIER, STRING, NUMBER,
    // Keywords. While looking at identifiers, we might also find these special words.
    // By the principle of maximal munch, if we get a match which is like "ifi" then it won't be matched as a "if"
    // keyword.
    AND, CLASS, ELSE, FALSE, FUN, FOR, IF, NIL, OR, PRINT, RETURN, SUPER, THIS, TRUE, VAR, WHILE, EOF
}