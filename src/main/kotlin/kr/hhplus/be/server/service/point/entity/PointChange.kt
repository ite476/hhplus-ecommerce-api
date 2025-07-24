package kr.hhplus.be.server.service.point.entity

import java.time.ZonedDateTime

class PointChange (
    val id: Long,
    val userId: Long,
    val pointChange: Long,
    val type: PointChangeType,
    val happenedAt: ZonedDateTime
)

enum class PointChangeType {
    Charge,
    Use,
}
