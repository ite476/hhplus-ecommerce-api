package kr.hhplus.be.server.util

import java.util.*

fun <T> Optional<T?>.unwrapOrThrow(exception: () -> RuntimeException): T {
    return this.orElse(null) ?: throw exception()
}
