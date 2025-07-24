package kr.hhplus.be.server.controller.common.advise

import kr.hhplus.be.server.service.common.exception.BusinessConflictException
import kr.hhplus.be.server.service.common.exception.BusinessUnacceptableException
import kr.hhplus.be.server.service.common.exception.ResourceNotFoundException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionHandler {
    @ExceptionHandler(BusinessConflictException::class)
    fun handleConflicted(e: BusinessConflictException): ResponseEntity<String> {
        return ResponseEntity
            .status(HttpStatus.CONFLICT)
            .body(e.message)
    }

    @ExceptionHandler(BusinessUnacceptableException::class)
    fun handleUnacceptable(e: BusinessUnacceptableException): ResponseEntity<String> {
        return ResponseEntity
            .status(HttpStatus.UNPROCESSABLE_ENTITY)
            .body(e.message)
    }

    @ExceptionHandler(ResourceNotFoundException::class)
    fun handleUnacceptable(e: ResourceNotFoundException): ResponseEntity<String> {
        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(e.message)
    }

    @ExceptionHandler(Exception::class)
    fun handleUnacceptable(e: Exception): ResponseEntity<String> {
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body("내부 오류가 발생했습니다. 지속적으로 발생 시 담당자에게 문의해주세요.")
    }
}