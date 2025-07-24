package kr.hhplus.be.server.repository.product

import kr.hhplus.be.server.service.product.entity.Product
import kr.hhplus.be.server.service.product.entity.ProductSaleSummary
import kr.hhplus.be.server.service.product.port.ProductPort
import org.springframework.stereotype.Component
import java.time.Duration
import java.time.ZonedDateTime

@Component
class ProductTemporaryAdapter : ProductPort {
    override fun findAllProducts(): List<Product> {
        TODO("Not yet implemented")
    }

    override fun findAllPopularProducts(
        whenSearch: ZonedDateTime,
        searchPeriod: Duration,
        fetchSize: Int
    ): List<ProductSaleSummary> {
        TODO("Not yet implemented")
    }

    override fun findProductById(productId: Long): Product {
        TODO("Not yet implemented")
    }

    override fun saveProduct(product: Product) {
        TODO("Not yet implemented")
    }

    override fun existsProduct(productId: Long): Boolean {
        TODO("Not yet implemented")
    }
}