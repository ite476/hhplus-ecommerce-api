package kr.hhplus.be.server.service.coupon.entity

import java.time.ZonedDateTime

data class Coupon (
    val id: Long,
    val name: String,
    val discount: Long,
    val totalQuantity: Int,
    val issuedQuantity: Int,
    val expiredAt: ZonedDateTime,
)