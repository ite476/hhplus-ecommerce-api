package kr.hhplus.be.server.service.lock

/**
 * 락 관련 유틸리티 함수들
 */
object LockUtils {
    
    /**
     * 상품 ID로 락 키 생성
     * 
     * @param productId 상품 ID
     * @return 락 키 (예: "product:123")
     */
    fun productLockKey(productId: Long): String = "product:$productId"
    
    /**
     * 사용자 ID로 락 키 생성
     * 
     * @param userId 사용자 ID
     * @return 락 키 (예: "user:456")
     */
    fun userLockKey(userId: Long): String = "user:$userId"
    
    /**
     * 주문 ID로 락 키 생성
     * 
     * @param orderId 주문 ID
     * @return 락 키 (예: "order:789")
     */
    fun orderLockKey(orderId: Long): String = "order:$orderId"
    
    /**
     * 쿠폰 ID로 락 키 생성
     * 
     * @param couponId 쿠폰 ID
     * @return 락 키 (예: "coupon:101")
     */
    fun couponLockKey(couponId: Long): String = "coupon:$couponId"
    
    /**
     * 복합 락 키 생성 (여러 식별자 조합)
     * 
     * @param prefix 접두사
     * @param ids 식별자들
     * @return 락 키 (예: "user_product:123:456")
     */
    fun compositeLockKey(prefix: String, vararg ids: Any): String = 
        "$prefix:${ids.joinToString(":")}"
}