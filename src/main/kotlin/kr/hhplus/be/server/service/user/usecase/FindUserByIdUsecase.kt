package kr.hhplus.be.server.service.user.usecase

import kr.hhplus.be.server.service.user.entity.User

interface FindUserByIdUsecase {
    /**
     * 회원을 Id로 조회
     */
    fun findUserById(userId: Long): User
}