package kr.hhplus.be.server.service.order.service

import kr.hhplus.be.server.service.coupon.entity.UserCoupon
import kr.hhplus.be.server.service.coupon.usecase.FindUserCouponByIdUsecase
import kr.hhplus.be.server.service.coupon.usecase.UseUserCouponUsecase
import kr.hhplus.be.server.service.order.entity.Order
import kr.hhplus.be.server.service.order.entity.OrderItem
import kr.hhplus.be.server.service.order.port.DataPlatformPort
import kr.hhplus.be.server.service.order.port.OrderPort
import kr.hhplus.be.server.service.order.usecase.CreateOrderUsecase
import kr.hhplus.be.server.service.point.usecase.ChargePointUsecase
import kr.hhplus.be.server.service.point.usecase.UsePointUsecase
import kr.hhplus.be.server.service.product.entity.Product
import kr.hhplus.be.server.service.product.entity.ProductSale
import kr.hhplus.be.server.service.product.usecase.AddProductStockUsecase
import kr.hhplus.be.server.service.product.usecase.FindProductByIdUsecase
import kr.hhplus.be.server.service.product.usecase.ReduceProductStockUsecase
import kr.hhplus.be.server.service.transaction.CompensationScope
import kr.hhplus.be.server.service.user.entity.User
import kr.hhplus.be.server.service.user.usecase.FindUserByIdUsecase
import kr.hhplus.be.server.util.KoreanTimeProvider
import org.springframework.stereotype.Service
import java.time.ZonedDateTime

@Service
class OrderService(
    val findUserByIdUsecase: FindUserByIdUsecase,
    val findProductByIdUsercase: FindProductByIdUsecase,
    val reduceProductStockUsecase: ReduceProductStockUsecase,
    val addProduceStockUsecase: AddProductStockUsecase,
    val usePointUsecase: UsePointUsecase,
    val chargePointUsecase: ChargePointUsecase,
    val findUserCouponByIdUsecase: FindUserCouponByIdUsecase,
    val useUserCouponUsecase: UseUserCouponUsecase,
    val orderPort: OrderPort,
    val dataPlatformPort: DataPlatformPort,
    val timeProvider: KoreanTimeProvider
) : CreateOrderUsecase {
    /**
     * 주문 생성 Input
     */
    data class CreateOrderInput(
        val userId: Long,
        val products: List<ProductWithQuantity>,
        val userCouponId: Long?
    ) {
        data class ProductWithQuantity(
            val productId: Long,
            val quantity: Long
        )
    }

    override suspend fun createOrder(
        input: CreateOrderInput
    ) : Order {
        val context: OrderCreationContext = OrderCreationContext.prepare(
            input = input,
            now = timeProvider.now(),
            findUserByIdUsecase = findUserByIdUsecase,
            findProductByIdUsercase = findProductByIdUsercase,
            findUserCouponByIdUsecase = findUserCouponByIdUsecase
        )

        val order: Order = CompensationScope.runTransaction {
            createOrder(context)
        }

        return order
    }

    /**
     * 주문 생성 시 필요한 도메인 객체 모음
     */
    private data class OrderCreationContext(
        val now: ZonedDateTime,
        val user: User,
        val productSales: List<ProductSale>,
        val orderItems: List<OrderItem>,
        val usedCoupon: UserCoupon?
    ) {
        companion object {
            fun prepare(
                input: CreateOrderInput,
                now: ZonedDateTime,
                findUserByIdUsecase: FindUserByIdUsecase,
                findProductByIdUsercase: FindProductByIdUsecase,
                findUserCouponByIdUsecase: FindUserCouponByIdUsecase
            ): OrderCreationContext {
                val user: User = findUserByIdUsecase.findUserById(userId = input.userId)

                val productSales: List<ProductSale> = input.products.map { sale ->
                    val product: Product = findProductByIdUsercase.findProductById(productId = sale.productId)
                    ProductSale(product = product, soldCount = sale.quantity, soldAt = now)
                }

                val orderItems: List<OrderItem> = productSales.map { sale ->
                    val productId: Long = sale.product.requiresId()
                    OrderItem(productId = productId, productName = sale.product.name, unitPrice = sale.product.price, quantity = sale.soldCount)
                }

                val usedCoupon: UserCoupon? = input.userCouponId?.let {
                    findUserCouponByIdUsecase.findUserCouponById(userId = input.userId, userCouponId = it)
                }

                return OrderCreationContext(now = now, user = user, productSales = productSales, orderItems = orderItems, usedCoupon = usedCoupon)
            }
        }
    }

    /**
     * 주문 생성 (보상 트랜잭션 진입)
     */
    private suspend fun CompensationScope.createOrder(
        context: OrderCreationContext
    ) : Order {
        // 각 상품 별 재고 차감
        context.productSales.forEach { sale ->
            val product: Product = sale.product
            val productId: Long = product.requiresId()

            val quantity: Long = sale.soldCount
            val now: ZonedDateTime = context.now


            execute {
                reduceProductStockUsecase.reduceProductStock(productId = productId, quantity = quantity, now = now)
            }.compensate {
                addProduceStockUsecase.addProductStock(productId = productId, quantity = quantity, now = now)
            }
        }

        // 쿠폰 사용 시 쿠폰 차감
        context.usedCoupon?.let { userCoupon ->
            val now: ZonedDateTime = context.now

            execute {
                useUserCouponUsecase.useUserCoupon(userCoupon = userCoupon, now = now)
            }.compensate {
                useUserCouponUsecase.rollbackUserCouponUsage(userCoupon = userCoupon, now = now)
            }
        }

        // 총 금액 계산
        val totalPrice: Long = run {
            val productsPrice: Long = context.orderItems.sumOf { it.totalPrice }
            val discountedPrice: Long = (context.usedCoupon?.discount ?: 0L)

            productsPrice - discountedPrice
        }

        // 포인트 차감 처리
        val userId: Long = context.user.requiresId()

        execute {
            usePointUsecase.usePoint(userId = userId, point = totalPrice)
        }.compensate {
            chargePointUsecase.chargePoint(userId = userId, point = totalPrice)
        }

        // 주문 생성
        val order: Order = execute {
            orderPort.createOrder(user = context.user, userCouponId = context.usedCoupon?.id, productsStamp = context.productSales, now = context.now)
        }.compensateBy { order ->
            orderPort.cancelOrder(order = order)
        }

        // 주문 정보를 외부 데이터 플랫폼으로 전송
        execute {
            dataPlatformPort.sendOrderData(order)
        }

        return order
    }
}