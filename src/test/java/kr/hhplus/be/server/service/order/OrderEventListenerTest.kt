package kr.hhplus.be.server.service.order

import io.mockk.coVerify
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import kotlinx.coroutines.test.runTest
import kr.hhplus.be.server.service.ServiceTestBase
import kr.hhplus.be.server.service.order.entity.Order
import kr.hhplus.be.server.service.order.entity.OrderItem
import kr.hhplus.be.server.service.order.event.OrderCreated
import kr.hhplus.be.server.service.order.event.OrderEventListener
import kr.hhplus.be.server.service.order.port.DataPlatformPort
import kr.hhplus.be.server.service.product.port.PopularProductPort
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
@DisplayName("OrderEventListener 테스트")
class OrderEventListenerTest : ServiceTestBase() {

    @MockK
    lateinit var popularProductPort: PopularProductPort

    @MockK
    lateinit var dataPlatformPort: DataPlatformPort

    private lateinit var listener: OrderEventListener

    @BeforeEach
    override fun setUp() {
        super.setUp()
        listener = OrderEventListener(popularProductPort, dataPlatformPort, timeProvider)
    }

    @Test
    @DisplayName("OrderCreated 수신 시 데이터 플랫폼 전송을 호출한다")
    fun callsDataPlatformOnOrderCreated() = runTest {
        // given
        val orderItems = listOf(
            OrderItem(id = 1L, productId = 1L, productName = "아메리카노", unitPrice = 4500L, quantity = 1)
        )
        val order = Order(
            id = 1L,
            userId = 1L,
            userCouponId = null,
            orderItems = orderItems,
            totalProductsPrice = 4500L,
            discountedPrice = 0L,
            orderedAt = fixedTime
        )

        // when
        listener.sendOrderToDataPlatformOnOrderCreated(OrderCreated(order))

        // then
        coVerify { dataPlatformPort.sendOrderData(order) }
    }
}


