package kr.hhplus.be.server.service.user.entity

import kr.hhplus.be.server.service.user.exception.UserNotFoundException

class User (
    val id: Long? = null,
    name: String,
    point: Long
) {
    var name: String = name
        private set

    var point: Long = point
        private set

    fun requiresId(): Long {
        return requireNotNull(id) { throw UserNotFoundException() }
    }
}