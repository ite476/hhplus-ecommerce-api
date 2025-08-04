package kr.hhplus.be.server.service.user.port

import kr.hhplus.be.server.service.user.entity.User

interface UserPort {
    fun readSingleUser(
        userId: Long
    ) : User

    fun existsUser(userId: Long) : Boolean
}
