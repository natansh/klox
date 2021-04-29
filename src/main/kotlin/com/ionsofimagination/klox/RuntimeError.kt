package com.ionsofimagination.klox

class RuntimeError(val token: Token, override val message: String) : RuntimeException(message)