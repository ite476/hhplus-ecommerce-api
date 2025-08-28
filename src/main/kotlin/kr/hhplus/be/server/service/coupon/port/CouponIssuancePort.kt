package kr.hhplus.be.server.service.coupon.port

/**
 * 고경합 환경에서의 선착순/1인1매 발급 결정을 담당하는 포트
 * 구현은 Redis 원자 스크립트 기반을 권장.
 */
interface CouponIssuancePort {
    /**
     * 준비 플래그(ready)가 없을 때, 단발 락으로 재고/ready를 로딩
     */
    suspend fun ensureReady(couponId: Long)

    /**
     * 발급 시도: 이미 발급 여부/재고를 원자 검증하여 결과를 반환
     */
    suspend fun tryIssue(userId: Long, couponId: Long): CouponIssuanceResult
}


