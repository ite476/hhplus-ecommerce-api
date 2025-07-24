package kr.hhplus.be.server.service.product.entity

import java.time.ZonedDateTime

data class ProductSaleSummary(
    val product: Product,
    val rank: Int,
    val soldCount: Int,
    val from: ZonedDateTime,
    val until: ZonedDateTime
)