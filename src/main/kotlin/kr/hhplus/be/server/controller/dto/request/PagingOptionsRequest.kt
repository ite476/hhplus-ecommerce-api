package kr.hhplus.be.server.controller.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import kr.hhplus.be.server.service.pagination.PagingOptions
import org.springdoc.core.annotations.ParameterObject

@ParameterObject
@Schema(description = "페이지 설정")
data class PagingOptionsRequestParam (
    @field:Schema(description = "페이지 번호", example = "0")
    val page: Int,

    @field:Schema(description = "페이지 크기", example = "100")
    val size: Int
) {
    fun toPagingOptions(): PagingOptions = PagingOptions(page,size)
}