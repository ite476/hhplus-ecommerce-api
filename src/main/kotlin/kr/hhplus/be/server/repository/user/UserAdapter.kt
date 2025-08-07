package kr.hhplus.be.server.repository.user

import kr.hhplus.be.server.repository.jpa.entity.user.UserEntity
import kr.hhplus.be.server.repository.jpa.repository.user.UserJpaRepository
import kr.hhplus.be.server.service.user.entity.User
import kr.hhplus.be.server.service.user.exception.UserNotFoundException
import kr.hhplus.be.server.service.user.port.UserPort
import kr.hhplus.be.server.util.unwrapOrThrow
import org.springframework.stereotype.Component
import java.util.*

@Component
class UserAdapter(
    private val userRepository: UserJpaRepository
): UserPort {
    override fun findUserById(userId: Long): User {
        val rawUser: Optional<UserEntity?> = userRepository.findById(userId)

        val userEntity: UserEntity = rawUser.unwrapOrThrow { UserNotFoundException() }

        val user: User = userEntity.let { e ->
            User(
                id = e.id,
                name = e.name,
                point = e.point
            )
        }

        return user
    }

    override fun existsUser(userId: Long): Boolean {
        val exists: Boolean = userRepository.existsById(userId)

        return exists
    }
}