package kr.hhplus.be.server.service.coupon.port

/**
 * Redis 원자 스크립트 기반 쿠폰 발급 결과
 */
enum class CouponIssuanceResult {
    OK,
    ALREADY_ISSUED,
    OUT_OF_STOCK,
    NOT_READY
}


