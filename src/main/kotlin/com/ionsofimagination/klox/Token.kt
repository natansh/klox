package com.ionsofimagination.klox

// It's not clear whether a data class will work here.
data class Token(
    // In Kotlin, sealed classes exist, so it might be better to attach a literal to the enum itself, such that the
    // type of literal can be preserved.
    val type: TokenType,
    val lexeme: String,
    val literal: Any?,
    val line: Int) {

    override fun toString(): String {
        return "$type $lexeme $literal"
    }
}