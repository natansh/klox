package com.ionsofimagination.klox

import java.lang.RuntimeException

class RuntimeError(val token: Token, override val message: String): RuntimeException(message)