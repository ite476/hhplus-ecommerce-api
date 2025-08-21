package kr.hhplus.be.server.service.product.port

import kr.hhplus.be.server.service.pagination.PagedList
import kr.hhplus.be.server.service.pagination.PagingOptions
import kr.hhplus.be.server.service.product.entity.Product

interface ProductPort {
    fun findPagedProducts(pagingOptions: PagingOptions) : PagedList<Product>

    fun findProductById(productId: Long) : Product
    fun saveProduct(product: Product)
    fun existsProduct(productId: Long): Boolean
}