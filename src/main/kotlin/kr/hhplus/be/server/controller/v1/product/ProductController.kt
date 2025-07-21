package kr.hhplus.be.server.controller.v1.product

import io.swagger.v3.oas.annotations.tags.Tag
import kr.hhplus.be.server.controller.v1.product.response.GetProductsPopularResponse
import kr.hhplus.be.server.controller.v1.product.response.GetProductsResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/products")
@Tag(name = "Product API", description = "상품 API")
class ProductController
    : ProductApiSepc {

    @GetMapping("")
    override fun getProducts(): ResponseEntity<List<GetProductsResponse>> = ResponseEntity.ok(listOf(
        GetProductsResponse(
            id = 1,
            name = "갤럭시 Z 플립 7",
            price = 1_456_000,
            stock = 600,
        )
    ))

    @GetMapping("/popular")
    override fun getPopularProducts(): ResponseEntity<List<GetProductsPopularResponse>> = ResponseEntity.ok(listOf(
        GetProductsPopularResponse(
            id = 1,
            name = "람보르기니 우라칸",
            price = 344_600_000,
            stock = 3,
            rank = 1,
            sold = 7,
        )
    ))
}