package kr.hhplus.be.server.controller.v1.product

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.responses.ApiResponse
import kr.hhplus.be.server.controller.v1.product.response.GetProductsPopularResponse
import kr.hhplus.be.server.controller.v1.product.response.GetProductsResponse
import org.springframework.http.ResponseEntity

interface ProductApiSepc {
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
    fun getProducts(): ResponseEntity<List<GetProductsResponse>>

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
    fun getPopularProducts(): ResponseEntity<List<GetProductsPopularResponse>>
}