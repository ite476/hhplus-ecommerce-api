package kr.hhplus.be.server.service.product.service

import kr.hhplus.be.server.service.product.entity.Product
import kr.hhplus.be.server.service.product.entity.ProductSaleSummary
import kr.hhplus.be.server.service.product.exception.ProductNotFoundException
import kr.hhplus.be.server.service.product.port.ProductPort
import kr.hhplus.be.server.util.KoreanTimeProvider
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.ZonedDateTime

@Service
class ProductService(
    val productPort: ProductPort,
    val timeProvider: KoreanTimeProvider
) {
    fun readProducts() : List<Product> {
        val products = productPort.findAllProducts()

        return products
    }

    fun readSingleProduct(
        productId: Long
    ) : Product {
        requireProductExists(productId)

        val product = productPort.findProductById(productId)

        return product
    }

    fun requireProductExists(productId: Long) {
        if (!existsProduct(productId)){
            throw ProductNotFoundException()
        }
    }

    fun existsProduct(productId: Long) : Boolean {
        return productPort.existsProduct(productId)
    }

    fun readPopularProducts() : List<ProductSaleSummary> {
        val now = timeProvider.now();
        val searchPeriod =  Duration.ofDays(3)
        val fetchSize = 5

        val popularProducts = productPort.findAllPopularProducts(
            now,
            searchPeriod,
            fetchSize
        )

        return popularProducts
    }

    fun addProductStock(productId: Long, quantity: Int, now: ZonedDateTime) {
        requireProductExists(productId)

        val product = productPort.findProductById(productId)

        product.addStock(quantity, now)

        productPort.saveProduct(product)
    }

    fun reduceProductStock(productId: Long, quantity: Int, now: ZonedDateTime) {
        requireProductExists(productId)

        val product = productPort.findProductById(productId)

        product.reduceStock(quantity, now)

        productPort.saveProduct(product)
    }
}