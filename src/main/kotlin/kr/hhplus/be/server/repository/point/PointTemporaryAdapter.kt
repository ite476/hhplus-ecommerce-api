package kr.hhplus.be.server.repository.point

import kr.hhplus.be.server.service.point.entity.PointChange
import kr.hhplus.be.server.service.point.port.PointPort
import org.springframework.stereotype.Component
import java.time.ZonedDateTime

@Component
class PointTemporaryAdapter : PointPort {
    override fun chargePoint(
        userId: Long,
        pointChange: Long,
        `when`: ZonedDateTime
    ): PointChange {
        TODO("Not yet implemented")
    }

    override fun usePoint(
        userId: Long,
        pointChange: Long,
        `when`: ZonedDateTime
    ): PointChange {
        TODO("Not yet implemented")
    }
}