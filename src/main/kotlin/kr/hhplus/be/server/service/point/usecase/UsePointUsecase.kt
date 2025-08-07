package kr.hhplus.be.server.service.point.usecase

import kr.hhplus.be.server.service.point.entity.PointChange

interface UsePointUsecase {
    /**
     * 포인트 사용 처리
     */
    fun usePoint(userId: Long, point: Long) : PointChange
}
