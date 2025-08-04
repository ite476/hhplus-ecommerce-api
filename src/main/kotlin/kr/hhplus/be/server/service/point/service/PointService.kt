package kr.hhplus.be.server.service.point.service

import kr.hhplus.be.server.service.point.entity.PointChange
import kr.hhplus.be.server.service.point.port.PointPort
import kr.hhplus.be.server.service.user.service.UserService
import kr.hhplus.be.server.util.KoreanTimeProvider
import org.springframework.stereotype.Service

@Service
class PointService (
    val userService: UserService,
    val timeProvider: KoreanTimeProvider,
    val pointPort: PointPort
){
    fun chargePoint(
        userId: Long,
        point: Long
    ) : PointChange {
        val user = userService.readSingleUser(userId)

        val pointChange = pointPort.chargePoint(
            user.id,
            point,
            timeProvider.now()
        )

        return pointChange
    }

    fun usePoint(
        userId: Long,
        point: Long
    ) : PointChange {
        userService.requireUserExists(userId)

        val pointChange = pointPort.usePoint(
            userId,
            point,
            timeProvider.now()
        )

        return pointChange
    }
}