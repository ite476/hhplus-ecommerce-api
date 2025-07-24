package kr.hhplus.be.server.repository.user

import kr.hhplus.be.server.service.user.entity.User
import kr.hhplus.be.server.service.user.port.UserPort
import org.springframework.stereotype.Component

@Component
class UserTemporaryAdapter : UserPort {
    override fun readSingleUser(userId: Long): User {
        TODO("Not yet implemented")
    }

    override fun existsUser(userId: Long): Boolean {
        TODO("Not yet implemented")
    }
}