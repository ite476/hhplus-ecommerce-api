package kr.hhplus.be.server.service.product.usecase

import kr.hhplus.be.server.service.pagination.PagedList
import kr.hhplus.be.server.service.pagination.PagingOptions
import kr.hhplus.be.server.service.product.entity.Product

interface FindPagedProductsUsecase {
    /**
     * 상품 목록 조회
     */
    fun findPagedProducts(pagingOptions: PagingOptions): PagedList<Product>
}
