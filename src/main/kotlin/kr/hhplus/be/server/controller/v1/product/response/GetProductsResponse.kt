package kr.hhplus.be.server.controller.v1.product.response

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "상품 정보 응답")
data class GetProductsResponse(
    @field:Schema(description = "상품 ID", example = "1")
    val id: Long,
    @field:Schema(description = "상품명", example = "아메리카노")
    val name: String,
    @field:Schema(description = "상품 가격 (원)", example = "4500")
    val price: Long,
    @field:Schema(description = "재고 수량", example = "100")
    val stock: Int
) 