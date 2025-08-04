package kr.hhplus.be.server.service.point.port

import kr.hhplus.be.server.service.point.entity.PointChange
import java.time.ZonedDateTime

interface PointPort {
    fun chargePoint(
        userId: Long,
        pointChange: Long,
        `when`: ZonedDateTime
    ) : PointChange

    fun usePoint(
        userId: Long,
        pointChange: Long,
        `when`: ZonedDateTime
    ) : PointChange
}