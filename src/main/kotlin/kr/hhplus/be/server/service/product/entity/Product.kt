package kr.hhplus.be.server.service.product.entity

import kr.hhplus.be.server.service.product.exception.LackOfProductStockException
import java.time.ZonedDateTime

class Product(
    val id: Long,
    name: String,
    price: Long,
    stock: Int,
    val createdAt: ZonedDateTime
) {
    var name: String = name
        private set

    var price: Long = price
        private set

    var stock: Int = stock
        private set

    fun addStock(quantity: Int, now: ZonedDateTime)
    {
        stock += quantity
    }

    fun reduceStock(quantity: Int, now: ZonedDateTime) {
        if (stock - quantity <= 0) throw LackOfProductStockException()

        stock -= quantity
    }
}
