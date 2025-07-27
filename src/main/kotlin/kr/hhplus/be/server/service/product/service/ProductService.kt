package kr.hhplus.be.server.service.product.service

import kr.hhplus.be.server.service.product.entity.Product
import kr.hhplus.be.server.service.product.entity.ProductSaleSummary
import kr.hhplus.be.server.service.product.port.ProductPort
import kr.hhplus.be.server.service.product.usecase.*
import kr.hhplus.be.server.util.KoreanTimeProvider
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.ZonedDateTime

@Service
class ProductService(
    val productPort: ProductPort,
    val timeProvider: KoreanTimeProvider
) : FindProductByIdUsecase,
    FindAllProductsUsecase,
    FindAllPopularProductsUsecase,
    AddProductStockUsecase,
    ReduceProductStockUsecase {
    override fun findAllProducts() : List<Product> {
        val products: List<Product> = productPort.findAllProducts()
            .onEach { it.requiresId() }

        return products
    }

    override fun findProductById(
        productId: Long
    ) : Product {
        val product: Product = productPort.findProductById(productId)
        product.requiresId()

        return product
    }

    override fun findAllPopularProducts() : List<ProductSaleSummary> {
        val now: ZonedDateTime = timeProvider.now();
        val searchPeriod: Duration =  Duration.ofDays(/* days = */ 3)
        val fetchSize = 5

        val popularProducts: List<ProductSaleSummary> = productPort.findAllPopularProducts(
            whenSearch = now,
            searchPeriod = searchPeriod,
            fetchSize = fetchSize
        )

        return popularProducts
    }

    override fun addProductStock(productId: Long, quantity: Int, now: ZonedDateTime) {
        val product: Product = findProductById(productId)

        product.addStock(quantity = quantity, now = now)

        productPort.saveProduct(product)
    }

    override fun reduceProductStock(productId: Long, quantity: Int, now: ZonedDateTime) {
        val product: Product = findProductById(productId)

        product.reduceStock(quantity = quantity, now = now)

        productPort.saveProduct(product)
    }
}