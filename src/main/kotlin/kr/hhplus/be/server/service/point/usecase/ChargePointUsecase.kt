package kr.hhplus.be.server.service.point.usecase

import kr.hhplus.be.server.service.point.entity.PointChange

interface ChargePointUsecase {
    /**
     * 포인트 충전 처리
     */
    fun chargePoint(userId: Long, point: Long) : PointChange
}
