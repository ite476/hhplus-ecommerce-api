package kr.hhplus.be.server.controller.v1.product.response

import io.swagger.v3.oas.annotations.media.Schema
import kr.hhplus.be.server.service.pagination.PagedList
import kr.hhplus.be.server.service.product.entity.Product

@Schema(description = "상품 정보 응답")
data class GetProductsResponse(
    val page: Int,
    val size: Int,
    val totalCount: Long,

    @field:Schema(description = "등록된 상품 목록")
    val products: List<ProductInfo>,
) {
    companion object {
        fun fromEntity(products: PagedList<Product>): GetProductsResponse {
            return GetProductsResponse(
                products = products.items.map { product ->
                    val productId: Long = product.requiresId()

                    ProductInfo(
                        id = productId,
                        name = product.name,
                        price = product.price,
                        stock = product.stock,
                    )
                },
                page = products.page,
                size = products.size,
                totalCount = products.totalCount
            )
        }
    }
}

@Schema(description = "상품 정보")
data class ProductInfo(
    @field:Schema(description = "상품 ID", example = "1")
    val id: Long,
    @field:Schema(description = "상품명", example = "아메리카노")
    val name: String,
    @field:Schema(description = "상품 가격 (원)", example = "4500")
    val price: Long,
    @field:Schema(description = "재고 수량", example = "100")
    val stock: Long
) 