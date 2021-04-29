package com.ionsofimagination.klox

class Environment(val enclosing: Environment? = null) {
    private var values: MutableMap<String, Any?> = HashMap()

    fun define(name: String, value: Any?) {
        values[name] = value
    }

    fun assign(name: Token, value: Any?) {
        if (values.containsKey(name.lexeme)) {
            values[name.lexeme] = value
            return
        }
        if (enclosing != null) {
            enclosing.assign(name, value)
            return
        }
        throw RuntimeError( name, "Undefined variable '${name.lexeme}'." )
    }

    fun get(name: Token): Any? {
        if (values.containsKey(name.lexeme)) {
           return values[name.lexeme]
        }
        if (enclosing != null) return enclosing.get(name)
        throw RuntimeError(name, "Undefined variable '${name.lexeme}'.")
    }

    fun getAt(distance: Int, name: String): Any? {
        // Why take from values and not use the `get` on environment? Because we want to be
        // specific about which environment we take it from.
        val value = ancestor(distance)?.values?.get(name)
        return value
    }

    fun assignAt(value: Any?, name: Token, distance: Int) {
        ancestor(distance)?.values?.put(name.lexeme, value)
    }

    private fun ancestor(distance: Int): Environment? {
        var env: Environment? = this
        for (i in 0 until distance) {
            env = env?.enclosing
        }
        return env
    }
}