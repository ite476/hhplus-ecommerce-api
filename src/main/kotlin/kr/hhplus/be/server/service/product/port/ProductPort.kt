package kr.hhplus.be.server.service.product.port

import kr.hhplus.be.server.service.product.entity.Product
import kr.hhplus.be.server.service.product.entity.ProductSaleSummary
import java.time.Duration
import java.time.ZonedDateTime

interface ProductPort {
    fun findAllProducts() : List<Product>

    fun findAllPopularProducts (
        whenSearch: ZonedDateTime,
        searchPeriod: Duration,
        fetchSize: Int
    ) : List<ProductSaleSummary>

    fun findProductById(productId: Long) : Product
    fun saveProduct(product: Product)
    fun existsProduct(productId: Long): Boolean
}