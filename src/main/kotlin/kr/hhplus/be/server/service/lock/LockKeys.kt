package kr.hhplus.be.server.service.lock

/**
 * 전역에서 공통으로 사용할 예약된 분산 락 키
 */
object LockKeys {
    /**
     * 인기 상품 조회 DB 로딩 등 비싼 작업 시 사용
     */
    val popularProductsKey: String
        get() = "popularProducts"
}