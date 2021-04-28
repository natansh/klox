package com.ionsofimagination.klox

class LoxClass(val name: String): LoxCallable {
    override fun toString(): String {
        return name
    }

    override fun call(interpreter: Interpreter, arguments: List<Any?>): Any? {
        return LoxInstance(this)
    }

    override fun arity(): Int {
        return 0
    }

}