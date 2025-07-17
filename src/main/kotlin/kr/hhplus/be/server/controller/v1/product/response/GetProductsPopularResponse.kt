package kr.hhplus.be.server.controller.v1.product.response

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "인기 상품 정보 응답")
data class GetProductsPopularResponse(
    @field:Schema(description = "상품 ID", example = "1")
    val id: Long,
    @field:Schema(description = "상품명", example = "카페라떼")
    val name: String,
    @field:Schema(description = "상품 가격 (원)", example = "5500")
    val price: Long,
    @field:Schema(description = "재고 수량", example = "50")
    val stock: Int,
    @field:Schema(description = "인기 순위", example = "1")
    val rank: Int,
    @field:Schema(description = "총 판매량", example = "1200")
    val sold: Int
)
