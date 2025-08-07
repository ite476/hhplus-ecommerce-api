package kr.hhplus.be.server.service.user.service

import kr.hhplus.be.server.service.user.entity.User
import kr.hhplus.be.server.service.user.exception.UserNotFoundException
import kr.hhplus.be.server.service.user.port.UserPort
import kr.hhplus.be.server.service.user.usecase.FindUserByIdUsecase
import kr.hhplus.be.server.service.user.usecase.RequiresUserIdExistsUsecase
import org.springframework.stereotype.Service

@Service
class UserService (
    val userPort: UserPort
) : FindUserByIdUsecase,
    RequiresUserIdExistsUsecase {

    private fun existsUser (userId: Long) : Boolean {
        val existsUser: Boolean = userPort.existsUser(userId)

        return existsUser
    }

    override fun findUserById(userId: Long) : User {
        val user: User = userPort.findUserById(userId)
        user.requiresId()

        return user
    }

    override fun requireUserIdExists(userId: Long) {
        require(existsUser(userId)) {
            throw UserNotFoundException()
        }
    }
}