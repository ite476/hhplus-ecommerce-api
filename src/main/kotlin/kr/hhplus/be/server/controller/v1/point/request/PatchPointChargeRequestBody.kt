package kr.hhplus.be.server.controller.v1.point.request

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "포인트 충전 요청")
data class PatchPointChargeRequestBody(
    @field:Schema(description = "충전 금액 (원)")
    val amount: Long
)