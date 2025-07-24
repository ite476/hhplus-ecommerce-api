package kr.hhplus.be.server.service.user.service

import kr.hhplus.be.server.service.user.entity.User
import kr.hhplus.be.server.service.user.exception.UserNotFoundException
import kr.hhplus.be.server.service.user.port.UserPort
import org.springframework.stereotype.Service

@Service
class UserService (
    val userPort: UserPort
){
    fun existsUser (
        userId: Long
    ) : Boolean {
        val existsUser = userPort.existsUser(userId)

        return existsUser
    }

    fun readSingleUser (
        userId: Long
    ) : User {
        requireUserExists(userId)

        val user = userPort.readSingleUser(userId)

        return user
    }

    fun requireUserExists(userId: Long) {
        if (!existsUser(userId)) {
            throw UserNotFoundException()
        }
    }
}