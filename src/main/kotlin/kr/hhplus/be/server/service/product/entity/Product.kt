package kr.hhplus.be.server.service.product.entity

import kr.hhplus.be.server.service.product.exception.LackOfProductStockException
import kr.hhplus.be.server.service.product.exception.ProductNotFoundException
import java.time.ZonedDateTime

class Product(
    val id: Long? = null,
    name: String,
    price: Long,
    stock: Long,
    val createdAt: ZonedDateTime
) {
    var name: String = name
        private set

    var price: Long = price
        private set

    var stock: Long = stock
        private set

    fun addStock(quantity: Long, now: ZonedDateTime)
    {
        stock += quantity
    }

    fun reduceStock(quantity: Long, now: ZonedDateTime) {
        require (stock >= quantity) {
            throw LackOfProductStockException()
        }

        stock -= quantity
    }

    fun requiresId() : Long {
        return requireNotNull(id) {
            throw ProductNotFoundException()
        }
    }
}
