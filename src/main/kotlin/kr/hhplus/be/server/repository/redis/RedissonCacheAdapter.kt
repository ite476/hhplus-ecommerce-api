package kr.hhplus.be.server.repository.redis

import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kr.hhplus.be.server.service.cache.CachePort
import org.redisson.api.RBucket
import org.redisson.api.RedissonClient
import org.redisson.client.protocol.ScoredEntry
import org.redisson.codec.JsonJacksonCodec
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit
import kotlin.reflect.KClass
import kotlin.time.Duration

@Component
class RedissonCacheAdapter(
    private val redisson: RedissonClient,
    @Autowired private val objectMapper: ObjectMapper,
    private val keyPrefix: String = "cache:"
) : CachePort {

    private val logger = LoggerFactory.getLogger(RedissonCacheAdapter::class.java)

    init {
        logger.info("RedissonCacheAdapter initialized with ObjectMapper: ${objectMapper.javaClass.simpleName}")
        logger.info("ObjectMapper modules: ${objectMapper.registeredModuleIds}")
    }

    // 전역 ObjectMapper를 사용하여 JsonJacksonCodec 생성
    private val jsonCodec = JsonJacksonCodec(objectMapper)

    private fun namespaced(key: String) = if (keyPrefix.isEmpty()) key else "$keyPrefix$key"

    private fun <T : Any> bucket(key: String): RBucket<T> =
        redisson.getBucket(namespaced(key), jsonCodec)

    override suspend fun <T : Any> get(key: String, type: KClass<T>): T? = withContext(Dispatchers.IO) {
        try {
            logger.debug("Cache GET attempt - key: $key, type: ${type.simpleName}")
            
            // Redis에서 raw JSON 확인 (동일한 JsonJacksonCodec 사용)
            val rawValue = bucket<Any>(key).get()
            logger.debug("Raw value from Redis: $rawValue")
            
            val result = bucket<T>(key).get()
            logger.debug("Cache GET success - key: $key, result: $result")
            result
        } catch (e: Exception) {
            logger.warn("Cache GET failed - key: $key, type: ${type.simpleName}, error: ${e.message}", e)
            logger.debug("Cache GET error details", e)
            // 직렬화 오류 시 null 반환 (캐시 미스로 처리)
            null
        }
    }

    override suspend fun set(key: String, value: Any, ttl: Duration?) = withContext(Dispatchers.IO) {
        require(ttl == null || !ttl.isNegative()) { "TTL must be null or non-negative" }

        try {
            logger.debug("Cache SET attempt - key: $key, value: $value, ttl: $ttl")
            
            // 직렬화 테스트
            val jsonString = objectMapper.writeValueAsString(value)
            logger.debug("Serialized JSON: $jsonString")
            
            val bucket = bucket<Any>(key)
            
            if (ttl != null) {
                bucket.set(value, ttl.inWholeMilliseconds, TimeUnit.MILLISECONDS)
            } else {
                bucket.set(value)
            }
            logger.debug("Cache SET success - key: $key")
        } catch (e: Exception) {
            logger.error("Cache SET failed - key: $key, value: $value, error: ${e.message}", e)
            logger.debug("Cache SET error details", e)
            throw e // 캐시 저장 실패는 치명적이므로 예외 전파
        }
    }

    override suspend fun evict(key: String) = withContext(Dispatchers.IO) {
        try {
            bucket<Any>(key).delete()
            logger.debug("Cache EVICT success - key: $key")
            Unit
        } catch (e: Exception) {
            logger.warn("Cache EVICT failed - key: $key, error: ${e.message}", e)
            // 삭제 실패는 치명적이지 않으므로 로깅만
        }
    }

    override suspend fun exists(key: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val exists = bucket<Any>(key).isExists
            logger.debug("Cache EXISTS check - key: $key, exists: $exists")
            exists
        } catch (e: Exception) {
            logger.warn("Cache EXISTS check failed - key: $key, error: ${e.message}", e)
            false // 확인 실패 시 false 반환
        }
    }

    override suspend fun <T : Any> getSortedSet(
        key: String,
        type: KClass<T>,
        fromScore: Double?,
        toScore: Double?,
        descending: Boolean,
        limit: Int?
    ): List<Pair<T, Double>>? = withContext(Dispatchers.IO) {
        try {
            logger.debug("ZSET GET attempt - key: $key, type: ${type.simpleName}, scoreRange: [$fromScore ~ $toScore], desc: $descending, limit: $limit")

            val scoredSortedSet = redisson.getScoredSortedSet<String>(namespaced(key), jsonCodec)

            val entries: Collection<ScoredEntry<String>> =
                if (descending) {
                    scoredSortedSet.entryRangeReversed(
                        /* startScore = */ fromScore ?: Double.NEGATIVE_INFINITY,
                        /* startScoreInclusive = */ true,
                        /* endScore = */ toScore ?: Double.POSITIVE_INFINITY,
                        /* endScoreInclusive = */ true,
                        /* offset = */ 0,
                        /* count = */ limit ?: Int.MAX_VALUE
                    )
                } else {
                    scoredSortedSet.entryRange(
                        /* startScore = */ fromScore ?: Double.NEGATIVE_INFINITY,
                        /* startScoreInclusive = */ true,
                        /* endScore = */ toScore ?: Double.POSITIVE_INFINITY,
                        /* endScoreInclusive = */ true,
                        /* offset = */ 0,
                        /* count = */ limit ?: Int.MAX_VALUE
                    )
                }

            val parsed = entries.mapNotNull { entry ->
                try {
                    val parsedValue = objectMapper.readValue(entry.value, type.java)
                    parsedValue to entry.score
                } catch (e: Exception) {
                    logger.warn("ZSET GET parse failed - raw: ${entry.value}, error: ${e.message}", e)
                    null
                }
            }

            logger.debug("ZSET GET success - key: $key, size: ${parsed.size}")
            parsed
        } catch (e: Exception) {
            logger.warn("ZSET GET failed - key: $key, type: ${type.simpleName}, error: ${e.message}", e)
            logger.debug("ZSET GET error details", e)
            null
        }
    }


    override suspend fun <T : Any> setSortedSet(
        key: String,
        values: List<Pair<T, Double>>,
        ttl: Duration?
    ) = withContext(Dispatchers.IO) {
        require(ttl == null || !ttl.isNegative()) { "TTL must be null or non-negative" }

        try {
            logger.debug("ZSET SET attempt - key: $key, values: ${values.size}, ttl: $ttl")

            val scoredSortedSet = redisson.getScoredSortedSet<String>(namespaced(key), jsonCodec)

            // 기존 항목 초기화
            scoredSortedSet.clear()

            for ((item, score) in values) {
                val serialized = objectMapper.writeValueAsString(item)
                scoredSortedSet.add(score, serialized)
            }

            if (ttl != null) {
                scoredSortedSet.expire(ttl.inWholeMilliseconds, TimeUnit.MILLISECONDS)
            }

            logger.debug("ZSET SET success - key: $key, total: ${values.size}")
        } catch (e: Exception) {
            logger.error("ZSET SET failed - key: $key, error: ${e.message}", e)
            logger.debug("ZSET SET error details", e)
            throw e
        }
    }

    override suspend fun <T : Any> incrementSortedSetScore(
        key: String,
        value: T,
        delta: Double
    ): Double = withContext(Dispatchers.IO) {
        try {
            val scoredSortedSet = redisson.getScoredSortedSet<String>(namespaced(key), jsonCodec)

            val serialized = objectMapper.writeValueAsString(value)

            // Sorted Set의 addScore - O(logN) 연산
            val newScore = scoredSortedSet.addScore(serialized, delta)

            logger.debug("ZSET SCORE INCREMENT - key: $key, value: $value, delta: $delta, newScore: $newScore")

            newScore
        } catch (e: Exception) {
            logger.error("ZSET SCORE INCREMENT failed - key: $key, value: $value, error: ${e.message}", e)
            throw e
        }
    }

    override suspend fun <T : Any> decrementSortedSetScore(key: String, value: T, delta: Double): Double
        = incrementSortedSetScore(key, value, -delta)
}
