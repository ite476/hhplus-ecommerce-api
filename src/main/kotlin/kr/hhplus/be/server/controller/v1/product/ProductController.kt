package kr.hhplus.be.server.controller.v1.product

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import kr.hhplus.be.server.controller.v1.product.response.GetProductsResponse
import kr.hhplus.be.server.controller.v1.product.response.GetProductsPopularResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/products")
@Tag(name = "Product API", description = "상품 API")
class ProductController {

    @GetMapping("")
    @Operation(
        summary = "전체 상품 조회",
        description = "등록된 모든 상품 목록을 조회합니다.",
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "성공",
                content = [
                    Content(
                        examples = [
                            ExampleObject(
                                name = "Success",
                                summary = "성공",
                                value = """
                                { 
                                    "message": "상품 목록을 불러왔습니다.",
                                    "body": [
                                        {
                                            "id": 1,
                                            "name": "상품1",
                                            "price": 10000,
                                            "stock": 100
                                        }
                                    ]
                                }
                                """
                            ),
                        ]
                    )
                ],
            ),
        ]
    )
    fun getProducts(): ResponseEntity<List<GetProductsResponse>> = ResponseEntity.ok(listOf(
        GetProductsResponse(
            id = 1,
            name = "갤럭시 Z 플립 7",
            price = 1_456_000,
            stock = 600,
        )
    ))

    @GetMapping("/popular")
    @Operation(
        summary = "인기 상품 조회",
        description = "인기 상품 목록을 조회합니다.",
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "성공",
                content = [
                    Content(
                        examples = [
                            ExampleObject(
                                name = "Success",
                                summary = "성공",
                                value = """
                                { 
                                    "message": "인기 상품 목록을 불러왔습니다.",
                                    "body": [
                                        {
                                            "id": 1,
                                            "name": "인기상품1",
                                            "price": 15000,
                                            "stock": 50,
                                            "rank": 1,
                                            "sold": 1200
                                        }
                                    ]
                                }
                                """
                            ),
                        ]
                    )
                ],
            ),
        ]
    )
    fun getPopularProducts(): ResponseEntity<List<GetProductsPopularResponse>> = ResponseEntity.ok(listOf(
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