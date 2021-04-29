package com.ionsofimagination.klox

class LoxFunction(
    private val declaration: Stmt.Function,
    private val closure: Environment,
    private val isInitializer: Boolean
) : LoxCallable {
    override fun call(interpreter: Interpreter, arguments: List<Any?>): Any? {
        // Each function call gets its own environment. Otherwise, recursion would break.
        val environment = Environment(closure)
        declaration.params.zip(arguments) { token, value -> environment.define(token.lexeme, value) }
        try {
            interpreter.executeBlock(declaration.body, environment)
        } catch (returnValue: Return) {
            if (isInitializer) return closure.getAt(0, "this")
            return returnValue.value
        }
        if (isInitializer) return closure.getAt(0, "this")
        return null
    }

    override fun arity(): Int = declaration.params.size

    fun bind(instance: LoxInstance): LoxFunction {
        val environment = Environment(closure)
        environment.define("this", instance)
        return LoxFunction(declaration, environment, isInitializer)
    }

    override fun toString(): String {
        return "<fn " + declaration.name.lexeme + ">"
    }
}