package kr.hhplus.be.server.repository.jpa.entity.coupon

import jakarta.persistence.*
import kr.hhplus.be.server.repository.jpa.entity.BaseEntity
import kr.hhplus.be.server.repository.jpa.entity.user.UserEntity
import kr.hhplus.be.server.service.coupon.entity.UserCoupon
import kr.hhplus.be.server.service.coupon.entity.UserCouponStatus
import org.hibernate.annotations.SQLRestriction
import java.time.ZonedDateTime

/**
 * 사용자 쿠폰 정보를 저장하는 JPA Entity
 */
@Entity
@Table(
    name = "user_coupon",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_user_coupon_user_id_coupon_id", 
            columnNames = ["user_id", "coupon_id"]
        )
    ]
)
@SQLRestriction("deleted_at IS NULL") // Soft Delete: 삭제된 행 제외
class UserCouponEntity(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    val user: UserEntity,

    @ManyToOne(fetch = FetchType.LAZY) 
    @JoinColumn(name = "coupon_id", nullable = false)
    val coupon: CouponEntity,

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    var status: UserCouponStatus = UserCouponStatus.ACTIVE,

    @Column(name = "issued_at", nullable = false)
    val issuedAt: ZonedDateTime = ZonedDateTime.now(),

    @Column(name = "used_at")
    var usedAt: ZonedDateTime? = null,

    @Column(name = "expired_at", nullable = false) 
    val expiredAt: ZonedDateTime
) : BaseEntity() {
    
    /**
     * 쿠폰 사용 가능 여부 확인
     */
    fun canUse(): Boolean {
        return status == UserCouponStatus.ACTIVE && !isExpired()
    }
    
    /**
     * 쿠폰 사용
     */
    fun use() {
        require(canUse()) { "사용할 수 없는 쿠폰입니다" }
        status = UserCouponStatus.USED
        usedAt = ZonedDateTime.now()
    }
    
    /**
     * 쿠폰 사용 취소 (환불 등의 경우)
     */
    fun cancelUse() {
        require(status == UserCouponStatus.USED) { "사용된 쿠폰이 아닙니다" }
        require(!isExpired()) { "만료된 쿠폰은 사용 취소할 수 없습니다" }
        status = UserCouponStatus.ACTIVE
        usedAt = null
    }
    
    /**
     * 쿠폰 만료 처리
     */
    fun expire() {
        if (status == UserCouponStatus.ACTIVE) {
            status = UserCouponStatus.EXPIRED
        }
    }
    
    /**
     * 쿠폰 만료 여부 확인
     */
    fun isExpired(): Boolean {
        return ZonedDateTime.now().isAfter(expiredAt) || status == UserCouponStatus.EXPIRED
    }
    
    /**
     * 사용된 쿠폰인지 확인
     */
    fun isUsed(): Boolean {
        return status == UserCouponStatus.USED
    }

    fun toDomain(): UserCoupon {
        return UserCoupon(
            id = this.id,
            userId = this.user.id,
            couponId = this.coupon.id,
            couponName = requireNotNull(this.coupon.name) {
                "CouponEntity.name is null (ID=${this.coupon.id}). 쿠폰명 데이터 정합성 확인 필요"
            },
            discount = this.coupon.discount,
            status = this.status,
            issuedAt = requireNotNull(this.issuedAt) {
                "UserCouponEntity.issuedAt is null (ID=${this.id}). 쿠폰 발급일시 데이터 정합성 확인 필요"
            },
            usedAt = this.usedAt,
            validUntil = requireNotNull(this.expiredAt) {
                "UserCouponEntity.expiredAt is null (ID=${this.id}). 쿠폰 만료일시 데이터 정합성 확인 필요"
            }
        )
    }

    fun updateFromDomain(userCoupon: UserCoupon): UserCouponEntity {
        // ID, 관계 매핑(user, coupon)은 건드리지 않는 게 일반적
        this.status = userCoupon.status
        this.usedAt = userCoupon.usedAt

        return this
    }
}

fun UserCoupon.toEntity(
    userEntity: UserEntity,
    couponEntity: CouponEntity
): UserCouponEntity {
    return UserCouponEntity(
        user = userEntity,
        coupon = couponEntity,
        status = this.status,
        issuedAt = this.issuedAt,
        usedAt = this.usedAt,
        expiredAt = this.validUntil
    )
}
