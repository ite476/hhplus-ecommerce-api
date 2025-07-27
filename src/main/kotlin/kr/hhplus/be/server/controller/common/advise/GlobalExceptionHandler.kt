package kr.hhplus.be.server.controller.common.advise

import kr.hhplus.be.server.service.common.exception.BusinessConflictException
import kr.hhplus.be.server.service.common.exception.BusinessUnacceptableException
import kr.hhplus.be.server.service.common.exception.ResourceNotFoundException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler

@RestControllerAdvice
class GlobalExceptionHandler : ResponseEntityExceptionHandler() {
    @ExceptionHandler(BusinessConflictException::class)
    fun handleConflict(ex: BusinessConflictException): ResponseEntity<Any> =
        buildErrorResponse(
            status = HttpStatus.CONFLICT,
            message = ex.message
        )

    @ExceptionHandler(BusinessUnacceptableException::class)
    fun handleUnprocessableEntity(ex: BusinessUnacceptableException): ResponseEntity<Any> =
        buildErrorResponse(
            status = HttpStatus.UNPROCESSABLE_ENTITY,
            message = ex.message
        )

    @ExceptionHandler(ResourceNotFoundException::class)
    fun handleNotFound(ex: ResourceNotFoundException): ResponseEntity<Any> =
        buildErrorResponse(
            status = HttpStatus.NOT_FOUND,
            message = ex.message
        )

    @ExceptionHandler(Exception::class)
    fun handleUnexpected(ex: Exception): ResponseEntity<Any> =
        buildErrorResponse(
            status = HttpStatus.INTERNAL_SERVER_ERROR,
            message = "내부 오류가 발생했습니다. 지속적으로 발생 시 담당자에게 문의해주세요."
        )

    private fun buildErrorResponse(status: HttpStatus, message: String?): ResponseEntity<Any> =
        ResponseEntity.status(status).body(mapOf("message" to (message ?: "알 수 없는 오류")))
}