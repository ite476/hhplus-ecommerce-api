package kr.hhplus.be.server.controller.v1.product.response

import io.swagger.v3.oas.annotations.media.Schema
import kr.hhplus.be.server.service.pagination.PagedList
import kr.hhplus.be.server.service.product.entity.ProductSaleSummary

@Schema(description = "인기 상품 정보 응답")
data class GetProductsPopularResponse(
    val page: Int,
    val size: Int,
    val totalCount: Long,

    @field:Schema(description = "인기 상품 목록")
    val products: List<PopularProductInfo>,
) {
    companion object {
        fun fromEntity(popularProducts: PagedList<ProductSaleSummary>) : GetProductsPopularResponse {
            return GetProductsPopularResponse(
                products = popularProducts.items.map { it ->
                    val productId: Long = it.product.requiresId()

                    PopularProductInfo(
                        id = productId,
                        name = it.product.name,
                        price = it.product.price,
                        stock = it.product.stock,
                        rank = it.rank,
                        sold = it.soldCount,
                    )
                },
                page = popularProducts.page,
                size = popularProducts.size,
                totalCount = popularProducts.totalCount
            )
        }
    }
}

@Schema(description = "상품 정보")
data class PopularProductInfo(
    @field:Schema(description = "상품 ID", example = "1")
    val id: Long,
    @field:Schema(description = "상품명", example = "카페라떼")
    val name: String,
    @field:Schema(description = "상품 가격 (원)", example = "5500")
    val price: Long,
    @field:Schema(description = "재고 수량", example = "50")
    val stock: Long,
    @field:Schema(description = "인기 순위", example = "1")
    val rank: Int,
    @field:Schema(description = "총 판매량", example = "1200")
    val sold: Long
)
