package kr.hhplus.be.server.repository.jpa.repository.user

import kr.hhplus.be.server.repository.jpa.entity.user.UserEntity
import org.springframework.data.jpa.repository.JpaRepository

/**
 * 사용자 Entity를 위한 JPA Repository
 */
interface UserJpaRepository : JpaRepository<UserEntity, Long> {

} 