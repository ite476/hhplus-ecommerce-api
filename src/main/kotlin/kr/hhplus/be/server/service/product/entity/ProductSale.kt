package kr.hhplus.be.server.service.product.entity

import java.time.ZonedDateTime

data class ProductSale (
    val product: Product,
    val soldCount: Long,
    val soldAt: ZonedDateTime
) {
    val unitPrice: Long
        get() = product.price
    val quantity: Long
        get() = product.stock
    val productId: Long
        get() = product.requiresId()
}