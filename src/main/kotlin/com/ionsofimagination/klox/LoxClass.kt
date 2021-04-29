package com.ionsofimagination.klox

class LoxClass(val name: String, val superclass: LoxClass?, methods: Map<String, LoxFunction>) : LoxCallable {
    private val methods: MutableMap<String, LoxFunction>

    init {
        this.methods = HashMap(methods)
    }

    fun findMethod(name: String): LoxFunction? {
        return when {
            methods.containsKey(name) -> methods[name]
            superclass != null -> superclass.findMethod(name)
            else -> null
        }
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