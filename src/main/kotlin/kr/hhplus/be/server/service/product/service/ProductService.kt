package kr.hhplus.be.server.service.product.service

import kotlinx.coroutines.runBlocking
import kr.hhplus.be.server.service.cache.CachePort
import kr.hhplus.be.server.service.cache.cacheKey
import kr.hhplus.be.server.service.cache.get
import kr.hhplus.be.server.service.pagination.PagedList
import kr.hhplus.be.server.service.pagination.PagingOptions
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
    val cachePort: CachePort,
    val timeProvider: KoreanTimeProvider
) : FindProductByIdUsecase,
    FindPagedProductsUsecase,
    FindPagedPopularProductsUsecase,
    AddProductStockUsecase,
    ReduceProductStockUsecase {
    override fun findPagedProducts(pagingOptions: PagingOptions) : PagedList<Product> {
        val products: PagedList<Product> = productPort.findPagedProducts(pagingOptions).let { paged ->
            paged.copy(
                items = paged.items.onEach { it.requiresId() }
            )
        }

        return products
    }

    override fun findProductById(
        productId: Long
    ) : Product {
        val product: Product = productPort.findProductById(productId)
        product.requiresId()

        return product
    }

    override fun findPagedPopularProducts(pagingOptions: PagingOptions): PagedList<ProductSaleSummary> {
        val now: ZonedDateTime = timeProvider.now();
        val searchPeriod: Duration =  Duration.ofDays(/* days = */ 3)

        val cacheKey = cacheKey("popularProducts:${pagingOptions.page}:${pagingOptions.size}")

        val popularProductsPage: PagedList<ProductSaleSummary>

        runBlocking {
            if (cachePort.exists(cacheKey)) {
                popularProductsPage = cachePort.get<PagedList<ProductSaleSummary>>(cacheKey)!!
            }
            else{
                popularProductsPage = productPort.findPagedPopularProducts(
                    whenSearch = now,
                    searchPeriod = searchPeriod,
                    pagingOptions = pagingOptions
                )

                cachePort.set(cacheKey, popularProductsPage)
            }
        }

        return popularProductsPage
    }

    override fun addProductStock(productId: Long, quantity: Long, now: ZonedDateTime) {
        val product: Product = findProductById(productId)

        product.addStock(quantity = quantity, now = now)

        productPort.saveProduct(product)
    }

    override fun reduceProductStock(productId: Long, quantity: Long, now: ZonedDateTime) {
        val product: Product = findProductById(productId)

        product.reduceStock(quantity = quantity, now = now)

        productPort.saveProduct(product)
    }
}