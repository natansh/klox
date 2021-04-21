package com.ionsofimagination.klox

class Environment {
    private var values: MutableMap<String, Any> = HashMap()

    fun define(name: String, value: Any?) {
        if (value != null) {
            values[name] = value
        } else {
            values.remove(name)
        }
    }

    fun get(name: Token): Any {
        return values.getOrElse(name.lexeme) {
            throw RuntimeError(name, "Undefined variable '${name.lexeme}'.")
        }
    }
}