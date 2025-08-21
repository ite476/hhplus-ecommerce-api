package kr.hhplus.be.server.service.cache

import kotlinx.coroutines.withTimeout
import kotlin.reflect.KClass
import kotlin.time.Duration

interface CachePort {
    suspend fun <T : Any> get(key: String, type: KClass<T>): T?          // 미스 시 null
    suspend fun set(key: String, value: Any, ttl: Duration? = null)      // put/set 네이밍
    suspend fun evict(key: String)                                       // 개별 삭제
    suspend fun exists(key: String): Boolean                             // 선택

    /**
     * Sorted Set (ZSet) 조회
     * @param key Redis ZSet key
     * @param type 요소 타입
     * @param fromScore 조회 시작 score (nullable → -∞)
     * @param toScore 조회 끝 score (nullable → +∞)
     * @param descending 내림차순 정렬 여부 (기본값: false)
     * @param limit 결과 수 제한 (null이면 전부)
     */
    suspend fun <T : Any> getSortedSet(
        key: String,
        type: KClass<T>,
        fromScore: Double? = null,
        toScore: Double? = null,
        descending: Boolean = false,
        limit: Int? = null
    ): List<Pair<T, Double>>?

    /**
     * Sorted Set (ZSet) 저장
     * @param key Redis ZSet key
     * @param values score와 함께 저장할 값 목록
     * @param ttl ZSet 자체의 TTL 설정 (null이면 만료 없음)
     */
    suspend fun <T : Any> setSortedSet(
        key: String,
        values: List<Pair<T, Double>>,
        ttl: Duration? = null
    )

    /**
     * Sorted Set(ZSet)의 특정 요소에 대해 점수를 증가시킴
     *
     * @param key ZSet 키
     * @param value 증가 대상 요소 (직렬화 기준으로 동일해야 함)
     * @param delta 증가할 score 양 (음수도 가능)
     * @return 증가 후의 최종 score
     */
    suspend fun <T : Any> incrementSortedSetScore(
        key: String,
        value: T,
        delta: Double
    ): Double

    /**
     * Sorted Set(ZSet)의 특정 요소에 대해 점수를 감소시킴
     *
     * @param key ZSet 키
     * @param value 감소 대상 요소 (직렬화 기준으로 동일해야 함)
     * @param delta 감소할 score 양 (음수도 가능)
     * @return 증가 후의 최종 score
     */
    suspend fun <T : Any> decrementSortedSetScore(
        key: String,
        value: T,
        delta: Double
    ): Double
}

// 호출 편의용 (reified는 인터페이스에 못 쓰니 확장함수로)
suspend inline fun <reified T : Any> CachePort.get(key: String): T? =
    get(key, T::class)

suspend inline fun <reified T : Any> CachePort.getOrPut(
    key: String,
    ttl: Duration? = null,
    opTimeout: Duration,
    crossinline loader: suspend () -> T
): T = withTimeout(opTimeout) {
    get<T>(key) ?: loader().also { set(key, it, ttl) }
}

suspend inline fun <reified T : Any> CachePort.getSortedSet(
    key: String,
    fromScore: Double? = null,
    toScore: Double? = null,
    descending: Boolean = false,
    limit: Int? = null
): List<Pair<T, Double>>? {
    return getSortedSet(
        key = key,
        type = T::class,
        fromScore = fromScore,
        toScore = toScore,
        descending = descending,
        limit = limit
    )
}


data class CacheKey<T : Any>(
    val name: String,
    val ttl: Duration? = null
    // 필요하면 serializer/namespace/version 등을 여기에
)

interface TypedCachePort {
    suspend fun <T : Any> get(key: CacheKey<T>, type: KClass<T>): T?
    suspend fun <T : Any> set(key: CacheKey<T>, value: T)
    suspend fun evict(key: CacheKey<*>)
}

suspend inline fun <reified T : Any> TypedCachePort.get(key: CacheKey<T>): T? =
    get(key, T::class)

fun cacheKey(key: String) = "cache:$key"