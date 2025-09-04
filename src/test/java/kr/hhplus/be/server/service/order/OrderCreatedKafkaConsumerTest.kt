package kr.hhplus.be.server.service.order

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import kotlinx.coroutines.test.runTest
import kr.hhplus.be.server.repository.product.RedisPopularProductSummaryCacheDto
import kr.hhplus.be.server.service.ServiceTestBase
import kr.hhplus.be.server.service.order.entity.Order
import kr.hhplus.be.server.service.order.entity.OrderItem
import kr.hhplus.be.server.service.order.event.OrderCreated
import kr.hhplus.be.server.service.order.event.OrderCreatedKafkaConsumer
import kr.hhplus.be.server.service.product.port.PopularProductPort
import kr.hhplus.be.server.service.idempotency.IdempotencyPort
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
@DisplayName("OrderCreatedKafkaConsumer - 멱등 가드")
class OrderCreatedKafkaConsumerTest : ServiceTestBase() {

    @MockK
    lateinit var popularProductPort: PopularProductPort

    @MockK
    lateinit var idempotencyPort: IdempotencyPort

    private lateinit var consumer: OrderCreatedKafkaConsumer

    @BeforeEach
    override fun setUp() {
        super.setUp()
        consumer = OrderCreatedKafkaConsumer(popularProductPort, timeProvider, idempotencyPort, 86400)
    }

    @Test
    @DisplayName("동일 이벤트를 두 번 수신하면, 첫 번째만 처리되고 두 번째는 무시된다")
    fun 동일_이벤트_두_번_수신_첫번째만_처리() = runTest {
        // Given
        val order = Order(
            id = 1L,
            userId = 1L,
            userCouponId = null,
            orderItems = listOf(OrderItem(id = 1L, productId = 10L, productName = "p", unitPrice = 1000L, quantity = 2)),
            totalProductsPrice = 2000L,
            discountedPrice = 0L,
            orderedAt = fixedTime
        )
        val event = OrderCreated(order)
        val key = "evt:processed:${event.eventId}"

        coEvery { idempotencyPort.tryAcquire(key, any()) } returnsMany listOf(true, false)

        // When
        consumer.onMessage(event)
        consumer.onMessage(event)

        // Then
        coVerify(exactly = 1) {
            popularProductPort.increaseProductSoldCount(
                listOf(RedisPopularProductSummaryCacheDto(10, 2)),
                fixedTime
            )
        }
    }

    @Test
    @DisplayName("멱등 선점에 실패하면, 집계는 호출되지 않는다")
    fun 멱등_선점_실패시_집계_호출되지_않음() = runTest {
        // Given
        val order = Order(
            id = 1L,
            userId = 1L,
            userCouponId = null,
            orderItems = listOf(OrderItem(id = 1L, productId = 11L, productName = "p", unitPrice = 1000L, quantity = 1)),
            totalProductsPrice = 1000L,
            discountedPrice = 0L,
            orderedAt = fixedTime
        )
        val event = OrderCreated(order)
        val key = "evt:processed:${event.eventId}"

        coEvery { idempotencyPort.tryAcquire(key, any()) } returns false

        // When
        consumer.onMessage(event)

        // Then
        coVerify(exactly = 0) { popularProductPort.increaseProductSoldCount(any(), any()) }
    }
}


