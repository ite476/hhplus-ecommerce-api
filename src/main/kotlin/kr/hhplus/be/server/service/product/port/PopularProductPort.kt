package kr.hhplus.be.server.service.product.port

import kr.hhplus.be.server.repository.product.RedisPopularProductSummaryCacheDto
import kr.hhplus.be.server.service.pagination.PagedList
import kr.hhplus.be.server.service.pagination.PagingOptions
import kr.hhplus.be.server.service.product.entity.ProductSaleSummary
import java.time.Duration
import java.time.ZonedDateTime

interface PopularProductPort {
    /**
     * 캐시가 준비되지 않은 경우 로드
     */
    suspend fun loadPopularProductsCacheIfNotLoaded()

    /**
     * 특정 기간으로부터 n일 간 집계.
     * 캐시 범위에 잡히지 않을 경우 데이터 제공 불가 (집계 안됨)
     */
    suspend fun findPagedPopularProducts(
        whenSearch: ZonedDateTime,
        searchPeriod: Duration,
        pagingOptions: PagingOptions
    ): PagedList<ProductSaleSummary>

    suspend fun increaseProductSoldCount(
        map: List<RedisPopularProductSummaryCacheDto>,
        now: ZonedDateTime
    )

    suspend fun decreaseProductSoldCount(
        map: List<RedisPopularProductSummaryCacheDto>,
        now: ZonedDateTime
    )
}