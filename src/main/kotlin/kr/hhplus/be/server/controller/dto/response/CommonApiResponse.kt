package kr.hhplus.be.server.controller.dto.response

data class CommonApiResponse<T> (
    val message: String = "요청 성공",
    val body: T? = null
)