package kr.hhplus.be.server.service.user.usecase

interface RequiresUserIdExistsUsecase {
    /**
     * 회원 Id 존재 여부 검증
     */
    fun requireUserIdExists(userId: Long)
}
