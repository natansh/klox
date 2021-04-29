package com.ionsofimagination.klox

class LoxClass(val name: String, methods: Map<String, LoxFunction>): LoxCallable {
    private val _methods: MutableMap<String, LoxFunction>
    val methods: Map<String, LoxFunction>
        get() = _methods

    init {
        this._methods = HashMap(methods)
    }

    fun findMethod(name: String): LoxFunction? {
        return if (methods.containsKey(name)) {
            methods[name]
        } else null
    }

    override fun toString(): String {
        return name
    }

    override fun call(interpreter: Interpreter, arguments: List<Any?>): Any? {
        val instance = LoxInstance(this)
        // Run initializer.
        findMethod("init")?.let { it.bind(instance).call(interpreter, arguments) }
        return instance
    }

    override fun arity(): Int {
        return findMethod("init")?.arity() ?: 0
    }

}