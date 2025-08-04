package kr.hhplus.be.server.service.product.entity

import java.time.ZonedDateTime

data class ProductSale (
    val product: Product,
    val soldCount: Int,
    val soldAt: ZonedDateTime
)