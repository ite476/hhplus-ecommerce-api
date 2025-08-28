package kr.hhplus.be.server.repository.product

data class RedisPopularProductSummaryCacheDto(
    val productId: Int,
    val soldCount: Int
)