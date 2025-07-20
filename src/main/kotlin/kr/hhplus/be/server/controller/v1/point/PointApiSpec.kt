package kr.hhplus.be.server.controller.v1.point

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.headers.Header
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import kr.hhplus.be.server.controller.v1.point.request.PatchPointChargeRequestBody
import kr.hhplus.be.server.controller.v1.point.response.GetPointResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader

interface PointApiSpec {
    @Operation(
        summary = "포인트 충전",
        description = "회원의 포인트 잔액을 충전합니다.",
        responses = [
            ApiResponse(
                responseCode = "201",
                description = "성공",
                headers = [
                    Header(
                        name = "Location",
                        description = "충전 후 포인트 조회 경로",
                        schema = Schema(type = "string", example = "/point")
                    )
                ],
            ),
            ApiResponse(responseCode = "404", description = "회원이 존재하지 않습니다."),
            ApiResponse(responseCode = "409", description = "이미 처리된 충전 요청입니다."),
            ApiResponse(responseCode = "422", description = "포인트 충전 액수는 1 이상이어야 합니다."),
        ]
    )
    fun chargePoint(
        @RequestHeader userId: String,
        @RequestBody body: PatchPointChargeRequestBody
    ) : ResponseEntity<Object>

    @Operation(
        summary = "포인트 조회",
        description = "회원의 포인트 잔액을 조회합니다.",
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
                                    "message": "메뉴 목록을 불러왔습니다.",
                                    "body": [
                                        {
                                            "id": 1,
                                            "name": "아메리카노",
                                            "pricePoint": 5000
                                        }
                                    ]
                                }
                                """
                            ),
                        ]
                    )
                ],
            ),
            ApiResponse(responseCode = "404", description = "회원이 존재하지 않습니다."),
        ]
    )
    fun readPoint(
        @RequestHeader userId: String
    ) : ResponseEntity<GetPointResponse>
}