package kr.hhplus.be.server.service.exception

open class UnexpectedBusinessFailureException(
    message: String,
    cause: Throwable? = null
): RuntimeException(message, cause)
