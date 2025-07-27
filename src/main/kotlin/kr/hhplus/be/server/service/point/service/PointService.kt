package kr.hhplus.be.server.service.point.service

import kr.hhplus.be.server.service.point.entity.PointChange
import kr.hhplus.be.server.service.point.port.PointPort
import kr.hhplus.be.server.service.point.usecase.ChargePointUsecase
import kr.hhplus.be.server.service.point.usecase.UsePointUsecase
import kr.hhplus.be.server.service.user.usecase.RequiresUserIdExistsUsecase
import kr.hhplus.be.server.util.KoreanTimeProvider
import org.springframework.stereotype.Service
import java.time.ZonedDateTime

@Service
class PointService (
    val requireUserIdExistsUsecase: RequiresUserIdExistsUsecase,
    val timeProvider: KoreanTimeProvider,
    val pointPort: PointPort
) : ChargePointUsecase,
    UsePointUsecase {
    override fun chargePoint(
        userId: Long,
        point: Long
    ) : PointChange {
        requireUserIdExistsUsecase.requireUserIdExists(userId);
        val now : ZonedDateTime = timeProvider.now()

        val pointChange: PointChange = pointPort.chargePoint(
            userId = userId,
            pointChange = point,
            `when` = now
        )

        return pointChange
    }

    override fun usePoint(
        userId: Long,
        point: Long
    ) : PointChange {
        requireUserIdExistsUsecase.requireUserIdExists(userId);
        val now : ZonedDateTime = timeProvider.now()

        val pointChange: PointChange = pointPort.usePoint(
            userId = userId,
            pointChange = point,
            `when` = now
        )

        return pointChange
    }
}