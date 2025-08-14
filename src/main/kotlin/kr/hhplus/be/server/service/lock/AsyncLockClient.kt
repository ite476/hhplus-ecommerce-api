package kr.hhplus.be.server.service.lock

/**
 * 락 핸들 - 락의 소유권을 증명하는 토큰
 * 
 * @param key 락 키
 * @param token 락 소유권을 증명하는 고유 토큰
 */
data class LockHandle(val key: String, val token: String)

/**
 * 비동기 락 클라이언트 인터페이스
 * 
 * 구현체는 다음을 보장해야 합니다:
 * - 락 획득 실패 시 null 반환
 * - 토큰 기반 소유자 검증
 * - 스레드 안전성
 */
interface AsyncLockClient {
    /**
     * 주어진 key에 대해 lease 기간으로 락을 시도.
     * 
     * @param key 락을 획득할 키
     * @param lockOptions 락 옵션 (대기 시간, 만료 시간)
     * @return 락 획득 성공 시 LockHandle, 실패 시 null
     * 
     * 특징:
     * - wait: 대기 허용 시간 (타임아웃)
     * - lease: 자동 만료 시간 (워치독/갱신이 없다면 block은 lease 이내로 끝나야 함)
     */
    suspend fun tryAcquire(key: String, lockOptions: LockOptions): LockHandle?

    /**
     * 획득했던 핸들을 해제.
     * 
     * @param handle 해제할 락 핸들
     * 
     * 구현체는 다음을 보장해야 합니다:
     * - 토큰 검증으로 소유자 확인
     * - 소유자가 아닌 경우 해제하지 않음
     * - Lua 스크립트나 동등한 원자적 검증 사용 권장
     */
    suspend fun release(handle: LockHandle)
}