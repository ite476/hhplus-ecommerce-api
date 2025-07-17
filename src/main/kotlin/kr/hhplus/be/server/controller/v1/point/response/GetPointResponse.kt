package kr.hhplus.be.server.controller.v1.point.response

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "포인트 충전 요청")
data class GetPointResponse(
    @field:Schema(description = "회원 로그인 아이디")
    val userId: Long,
    @field:Schema(description = "포인트 잔액")
    val ponit: Long,
)