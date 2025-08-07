package kr.hhplus.be.server.service.order.service

import kr.hhplus.be.server.service.coupon.entity.UserCoupon
import kr.hhplus.be.server.service.coupon.usecase.FindUserCouponByIdUsecase
import kr.hhplus.be.server.service.coupon.usecase.UseUserCouponUsecase
import kr.hhplus.be.server.service.point.usecase.ChargePointUsecase
import kr.hhplus.be.server.service.point.usecase.UsePointUsecase
import kr.hhplus.be.server.service.product.entity.Product
import kr.hhplus.be.server.service.product.usecase.AddProductStockUsecase
import kr.hhplus.be.server.service.product.usecase.FindProductByIdUsecase
import kr.hhplus.be.server.service.product.usecase.ReduceProductStockUsecase
import kr.hhplus.be.server.service.user.entity.User
import kr.hhplus.be.server.service.user.usecase.FindUserByIdUsecase
import org.springframework.stereotype.Component
import java.time.ZonedDateTime

@Component
class OrderServiceFacade (
    val findUserByIdUsecase: FindUserByIdUsecase,
    val usePointUsecase: UsePointUsecase,
    val chargePointUsecase: ChargePointUsecase,

    val findProductByIdUsecase: FindProductByIdUsecase,
    val reduceProductStockUsecase: ReduceProductStockUsecase,
    val addProductStockUsecase: AddProductStockUsecase,

    val findUserCouponByIdUsecase: FindUserCouponByIdUsecase,
    val useUserCouponUsecase: UseUserCouponUsecase,
){
    // 회원 및 포인트 도메인
    fun findUserById(userId: Long): User = findUserByIdUsecase.findUserById(userId)
    fun usePoint(userId: Long, point: Long) = usePointUsecase.usePoint(userId, point)
    fun chargePoint(userId: Long, point: Long) = chargePointUsecase.chargePoint(userId, point)

    // 상품 도메인
    fun findProductById(productId: Long): Product = findProductByIdUsecase.findProductById(productId)
    fun addProductStock(productId: Long, quantity: Long, now: ZonedDateTime) = addProductStockUsecase.addProductStock(productId, quantity, now)
    fun reduceProductStock(productId: Long, quantity: Long, now: ZonedDateTime) = reduceProductStockUsecase.reduceProductStock(productId, quantity, now)

    // 쿠폰 도메인
    fun findUserCouponById(userId: Long, userCouponId: Long): UserCoupon? = findUserCouponByIdUsecase.findUserCouponById(userId, userCouponId)
    fun useUserCoupon(userCoupon: UserCoupon, now: ZonedDateTime) = useUserCouponUsecase.useUserCoupon(userCoupon, now)
    fun rollbackUserCouponUsage(userCoupon: UserCoupon, now: ZonedDateTime) = useUserCouponUsecase.rollbackUserCouponUsage(userCoupon, now)
}
