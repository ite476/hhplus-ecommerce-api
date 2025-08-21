package kr.hhplus.be.server.service.lock

import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * 락 옵션 설정
 * 
 * @param wait 락 획득 대기 시간 (타임아웃)
 * @param lease 락 자동 만료 시간 (워치독 없이 이 시간 내에 작업 완료 필요)
 */
data class LockOptions(
    val wait: Duration,
    val lease: Duration
) {
    companion object {
        /**
         * 기본 락 옵션
         * - wait: 3초 (락 획득 대기)
         * - lease: 10초 (락 자동 만료)
         */
        val DEFAULT: LockOptions = LockOptions(
            wait = 3.seconds,
            lease = 10.seconds
        )
        
        /**
         * 빠른 락 옵션 (짧은 작업용)
         * - wait: 1초
         * - lease: 5초
         */
        val FAST: LockOptions = LockOptions(
            wait = 1.seconds,
            lease = 5.seconds
        )
        
        /**
         * 느린 락 옵션 (긴 작업용)
         * - wait: 10초
         * - lease: 30초
         */
        val SLOW: LockOptions = LockOptions(
            wait = 10.seconds,
            lease = 30.seconds
        )
    }
    
    /**
     * 락 옵션 유효성 검증
     */
    init {
        require(wait > Duration.ZERO) { "wait time must be positive" }
        require(lease > Duration.ZERO) { "lease time must be positive" }
        require(lease >= wait) { "lease time must be greater than or equal to wait time" }
    }
}