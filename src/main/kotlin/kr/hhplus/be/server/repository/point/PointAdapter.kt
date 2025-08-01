package kr.hhplus.be.server.repository.point

import kr.hhplus.be.server.repository.jpa.repository.user.UserJpaRepository
import kr.hhplus.be.server.service.point.entity.PointChange
import kr.hhplus.be.server.service.point.entity.PointChangeType
import kr.hhplus.be.server.service.point.exception.LackOfPointException
import kr.hhplus.be.server.service.point.exception.PointChargeMustBeGreaterThanZeroException
import kr.hhplus.be.server.service.point.exception.PointUsageMustBeGreaterThanZeroException
import kr.hhplus.be.server.service.point.port.PointPort
import kr.hhplus.be.server.service.user.exception.UserNotFoundException
import kr.hhplus.be.server.util.unwrapOrThrow
import org.springframework.stereotype.Component
import java.time.ZonedDateTime

@Component
class PointAdapter(
    private val userRepository: UserJpaRepository
) : PointPort {

    override fun chargePoint(userId: Long, pointChange: Long, `when`: ZonedDateTime): PointChange {
        require (pointChange > 0) {
            throw PointChargeMustBeGreaterThanZeroException()
        }

        val userEntity = userRepository.findById(userId)
            .unwrapOrThrow { UserNotFoundException() }

        // 포인트 충전 실행
        userEntity.chargePoint(pointChange)
        
        // 변경된 사용자 정보 저장
        val savedUserEntity = userRepository.save(userEntity)

        // PointChange 엔티티 생성
        // TODO: (실제로는 별도 PointHistory 테이블이 필요할 수 있음)
        return PointChange(
            id = null, // 포인트 이력 ID (별도 테이블 구현 시)
            userId = savedUserEntity.id,
            pointChange = pointChange,
            type = PointChangeType.Charge,
            happenedAt = `when`
        )
    }

    override fun usePoint(userId: Long, pointChange: Long, `when`: ZonedDateTime): PointChange {
        require(pointChange > 0) {
            throw PointUsageMustBeGreaterThanZeroException()
        }

        val userEntity = userRepository.findById(userId)
            .unwrapOrThrow { UserNotFoundException() }

        // 사용 가능한 포인트인지 확인
        require(userEntity.canUsePoint(pointChange)) {
            throw LackOfPointException()
        }

        // 포인트 사용 실행
        userEntity.usePoint(pointChange)
        
        // 변경된 사용자 정보 저장
        val savedUserEntity = userRepository.save(userEntity)

        // PointChange 엔티티 생성
        // TODO: (실제로는 별도 PointHistory 테이블이 필요할 수 있음)
        return PointChange(
            id = null, // 포인트 이력 ID (별도 테이블 구현 시)
            userId = savedUserEntity.id,
            pointChange = pointChange,
            type = PointChangeType.Use,
            happenedAt = `when`
        )
    }
}