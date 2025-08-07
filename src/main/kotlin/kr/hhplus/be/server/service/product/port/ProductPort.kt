package kr.hhplus.be.server.service.product.port

import kr.hhplus.be.server.service.pagination.PagedList
import kr.hhplus.be.server.service.pagination.PagingOptions
import kr.hhplus.be.server.service.product.entity.Product
import kr.hhplus.be.server.service.product.entity.ProductSaleSummary
import java.time.Duration
import java.time.ZonedDateTime

interface ProductPort {
    fun findPagedProducts(pagingOptions: PagingOptions) : PagedList<Product>

    fun findPagedPopularProducts(
        whenSearch: ZonedDateTime,
        searchPeriod: Duration,
        pagingOptions: PagingOptions
    ): PagedList<ProductSaleSummary>

    fun findProductById(productId: Long) : Product
    fun saveProduct(product: Product)
    fun existsProduct(productId: Long): Boolean
}