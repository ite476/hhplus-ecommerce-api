package kr.hhplus.be.server.service.order.service

import kr.hhplus.be.server.service.common.transaction.CompensationScope
import kr.hhplus.be.server.service.coupon.service.CouponService
import kr.hhplus.be.server.service.order.entity.Order
import kr.hhplus.be.server.service.order.port.DataPlatformPort
import kr.hhplus.be.server.service.order.port.OrderPort
import kr.hhplus.be.server.service.point.service.PointService
import kr.hhplus.be.server.service.product.entity.ProductSale
import kr.hhplus.be.server.service.product.service.ProductService
import kr.hhplus.be.server.service.user.service.UserService
import kr.hhplus.be.server.util.KoreanTimeProvider
import org.springframework.stereotype.Service

@Service
class OrderService (
    val userService: UserService,
    val pointService: PointService,
    val productService: ProductService,
    val couponService: CouponService,
    val orderPort: OrderPort,
    val dataPlatformPort: DataPlatformPort,
    val timeProvider: KoreanTimeProvider,
) {
    data class CreateOrderInput(
        val userId: Long,
        val products: List<ProductWithQuantity>,
        val userCouponId: Long?
    ) {
        data class ProductWithQuantity(
            val productId: Long,
            val quantity: Int
        )
    }

    /**
     * 주문을 생성합니다.
     */
    suspend fun createOrder(
        input: CreateOrderInput
    ) : Order {
        userService.requireUserExists(input.userId)
        input.products.forEach { productService.requireProductExists(it.productId) }
        if(input.userCouponId != null) couponService.requireUserCouponExists(input.userCouponId)

        val now = timeProvider.now()
        val scope = CompensationScope()

        try{
            // 회원 정보 획득 (포인트 잔고 조회 겸용)
            val user = userService.readSingleUser(input.userId)

            // 각 상품 별
            // - 재고 차감
            // - 현재 상태 스탬프 처리
            val productsStamp = input.products.map {
                val product = productService.readSingleProduct(it.productId)

                scope.execute {
                    productService.reduceProductStock(it.productId, it.quantity, now)
                }.compensate {
                    productService.addProductStock(it.productId, it.quantity, now)
                }

                ProductSale(
                    product,
                    it.quantity,
                    now,
                )
            }

            // 총 가격 계산 시작
            var totalPrice : Long = productsStamp.sumOf { it.product.price * it.soldCount }

            // 쿠폰 사용 시
            // - 쿠폰 차감
            // - 총 가격에서 차감 (할인)
            if (input.userCouponId != null){
                val coupon = couponService.readSingleUserCoupon(
                    input.userId,
                    input.userCouponId)

                scope.execute {
                    couponService.useUserCoupon(coupon, now)
                }.compensate {
                    couponService.rollbackUserCouponUsage(coupon, now)
                }

                totalPrice -= coupon.discount
            }

            // 최종 가격을 회원 포인트 잔고에서 차감
            scope.execute {
                pointService.usePoint(user.id, totalPrice)
            }.compensate {
                pointService.chargePoint(user.id, totalPrice)
            }

            // 주문 생성
            val orderResult = scope.execute {
                orderPort.createOrder(
                    user,
                    input.userCouponId,
                    productsStamp,
                    now
                )
            }

            val order = orderResult.result

            orderResult.compensate {
                orderPort.cancelOrder(order)
            }

            // 주문 정보를 외부 데이터 플랫폼으로 전송
            dataPlatformPort.sendOrderData(order)

            return order
        }
        catch (ex: Exception){
            scope.rollbackAll()

            throw ex
        }
    }
}