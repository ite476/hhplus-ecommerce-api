package kr.hhplus.be.server.repository.product

import kr.hhplus.be.server.repository.jpa.repository.product.ProductQueryRepository
import kr.hhplus.be.server.service.cache.CachePort
import kr.hhplus.be.server.service.cache.get
import kr.hhplus.be.server.service.lock.AsyncLockClient
import kr.hhplus.be.server.service.lock.LockKeys
import kr.hhplus.be.server.service.lock.LockScope
import kr.hhplus.be.server.service.lock.LockUtils.lockKeyWithDate
import kr.hhplus.be.server.service.lock.LockUtils.lockKeyWithReady
import kr.hhplus.be.server.service.pagination.PagedList
import kr.hhplus.be.server.service.pagination.PagingOptions
import kr.hhplus.be.server.service.product.entity.ProductSaleSummary
import kr.hhplus.be.server.service.product.port.PopularProductPort
import kr.hhplus.be.server.service.product.port.ProductPort
import kr.hhplus.be.server.util.KoreanTimeProvider
import org.springframework.stereotype.Component
import java.time.Duration
import java.time.LocalDate
import java.time.ZonedDateTime

@Component
class PopularProductRedisCacheAdapter(
    private val lockClient: AsyncLockClient,
    private val cachePort: CachePort,
    private val productPort: ProductPort,
    private val productsQueryRepository: ProductQueryRepository,
    private val timeProvider: KoreanTimeProvider
) : PopularProductPort {
    override suspend fun loadPopularProductsCacheIfNotLoaded() {
        val mainKey: String = LockKeys.popularProductsKey
        val readyKey: String = lockKeyWithReady(prefix = mainKey)

        LockScope.withLock(lockClient, key = readyKey) {
            // 캐시가 이미 존재하면 조기 반환
            // 캐시 쓰기 경합이 발생한 경우에 주로 해당
            if (cachePort.exists(readyKey)) return@withLock

            val today: ZonedDateTime = timeProvider.now()

            val searchFrom: ZonedDateTime = today.minusDays(10)
            val searchUntil: ZonedDateTime = today

            // 최근 10일간의 일자별 인기 상품 판매량 요약 조회
            val foundDbSummaries: List<Pair<ZonedDateTime, List<ProductSaleSummary>>> = productsQueryRepository.findPopularProductsByDate(
                searchFrom = searchFrom,
                searchUntil = searchUntil
            ).sortedByDescending {
                it.first
            }

            // Redis에 날짜별로 캐시 저장
            foundDbSummaries.forEach { summaryPair ->
                val date: ZonedDateTime = summaryPair.first
                val summary: List<ProductSaleSummary> = summaryPair.second
                val cacheKey: String = lockKeyWithDate(prefix = mainKey, date = date)

                cachePort.set(key = cacheKey, value = summary)
            }

            // 메인 키에 임의의 값 설정 (존재 유무 체크용)
            cachePort.set(key = readyKey, value = "The Cache Has Been Set!")
        }
    }


    override suspend fun findPagedPopularProducts(
        whenSearch: ZonedDateTime,
        searchPeriod: Duration,
        pagingOptions: PagingOptions
    ): PagedList<ProductSaleSummary> {
        loadPopularProductsCacheIfNotLoaded()

        val searchFrom: ZonedDateTime = whenSearch - searchPeriod
        val searchUntil: ZonedDateTime = whenSearch

        val mainKey: String = LockKeys.popularProductsKey

        // 1. 날짜 목록 생성 (최근 날짜가 마지막)
        val enumeratedDays: List<LocalDate> = generateSequence(seed = searchFrom.toLocalDate()) { it.plusDays(1) }
            .takeWhile { it <= searchUntil.toLocalDate() }
            .toList()

        // 2. 날짜별 Redis 키 목록
        val redisKeys: List<String> = enumeratedDays.map { date ->
            lockKeyWithDate(prefix = mainKey, date = date)
        }

        // 3. Redis에서 각 날짜별 인기 상품 리스트 가져오기
        val dailyRankings: List<RedisPopularProductSummaryCacheDto> = redisKeys.flatMap { key ->
            val foundCache: List<RedisPopularProductSummaryCacheDto>? = cachePort.get<List<RedisPopularProductSummaryCacheDto>>(key)

            foundCache ?: emptyList()
        }

        // 4. productId로 groupBy 후 soldCount 합산
        val aggregated: List<RedisPopularProductSummaryCacheDto> = dailyRankings
            .groupBy { it.productId }
            .map { (productId, entries) ->
                val totalSoldCount = entries.sumOf { it.soldCount }
                RedisPopularProductSummaryCacheDto(
                    productId = productId,
                    soldCount = totalSoldCount
                )
            }
            .sortedByDescending { it.soldCount }

        // 5. 페이지네이션 적용
        val total: Int = aggregated.size
        val fromIndex: Int = ((pagingOptions.page - 1) * pagingOptions.size).coerceAtMost(total)
        val toIndex: Int = (fromIndex + pagingOptions.size).coerceAtMost(total)
        val pagedItems: List<RedisPopularProductSummaryCacheDto> = aggregated.subList(fromIndex, toIndex)

        // 6. 랭킹 추가 + 결과 매핑
        val rankedItems: List<ProductSaleSummary> = pagedItems.mapIndexed { index, summary ->
            val rank: Int = (fromIndex + index + 1)

            ProductSaleSummary(
                product = productPort.findProductById(productId = summary.productId.toLong()),
                rank = rank,
                soldCount = summary.soldCount.toLong(),
                from = searchFrom,
                until = searchUntil
            )
        }

        val page: PagedList<ProductSaleSummary> = PagedList(
            items = rankedItems,
            page = pagingOptions.page,
            size = pagingOptions.size,
            totalCount = total.toLong()
        )

        return page
    }

    override suspend fun increaseProductSoldCount(map: List<RedisPopularProductSummaryCacheDto>, now: ZonedDateTime) {
        loadPopularProductsCacheIfNotLoaded()

        val mainKey: String = LockKeys.popularProductsKey
        val cacheKey: String = lockKeyWithDate(prefix = mainKey, date = now)

        map.forEach { it ->
            cachePort.incrementSortedSetScore(
                key = cacheKey,
                value = it.productId,
                delta = it.soldCount.toDouble()
            )
        }
    }

    override suspend fun decreaseProductSoldCount(map: List<RedisPopularProductSummaryCacheDto>, now: ZonedDateTime) {
        loadPopularProductsCacheIfNotLoaded()

        val mainKey: String = LockKeys.popularProductsKey
        val cacheKey: String = lockKeyWithDate(prefix = mainKey, date = now)

        map.forEach { it ->
            cachePort.decrementSortedSetScore(
                key = cacheKey,
                value = it.productId,
                delta = it.soldCount.toDouble()
            )
        }
    }
}

