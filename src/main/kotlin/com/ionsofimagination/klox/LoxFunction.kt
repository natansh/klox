package com.ionsofimagination.klox

class LoxFunction(private val declaration: Stmt.Function): LoxCallable {
    override fun call(interpreter: Interpreter, arguments: List<Any?>): Any? {
        // Each function call gets its own environment. Otherwise, recursion would break.
        val environment = Environment(interpreter.globals)
        declaration.params.zip(arguments) { token, value -> environment.define(token.lexeme, value) }
        interpreter.executeBlock(declaration.body, environment)
        return null
    }

    override fun arity(): Int = declaration.params.size

    override fun toString(): String {
        return "<fn " + declaration.name.lexeme + ">"
    }
}