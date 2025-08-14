package kr.hhplus.be.server.service.cache

import kotlinx.coroutines.withTimeout
import kotlin.reflect.KClass
import kotlin.time.Duration

interface CachePort {
    suspend fun <T : Any> get(key: String, type: KClass<T>): T?          // 미스 시 null
    suspend fun set(key: String, value: Any, ttl: Duration? = null)      // put/set 네이밍
    suspend fun evict(key: String)                                       // 개별 삭제
    suspend fun exists(key: String): Boolean                             // 선택
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