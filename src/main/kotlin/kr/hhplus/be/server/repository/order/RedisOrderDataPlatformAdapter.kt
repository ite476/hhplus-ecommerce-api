package kr.hhplus.be.server.repository.order

import kr.hhplus.be.server.repository.product.RedisPopularProductSummaryCacheDto
import kr.hhplus.be.server.service.order.entity.Order
import kr.hhplus.be.server.service.order.port.DataPlatformPort
import kr.hhplus.be.server.service.product.port.PopularProductPort
import kr.hhplus.be.server.util.KoreanTimeProvider
import org.springframework.stereotype.Component

@Component
class RedisOrderDataPlatformAdapter(
    private val popularProductPort: PopularProductPort,
    private val timeProvider: KoreanTimeProvider
) : DataPlatformPort {



    override suspend fun sendOrderData(order: Order) {
        val now = timeProvider.now()

        popularProductPort.increaseProductSoldCount(
            order.orderItems.map { it ->
                RedisPopularProductSummaryCacheDto(
                    it.productId.toInt(),
                    it.quantity.toInt()
                )
            },
            now
        )
    }

    override suspend fun revertOrderData(order: Order) {
        val now = timeProvider.now()

        popularProductPort.decreaseProductSoldCount(
            order.orderItems.map { it ->
                RedisPopularProductSummaryCacheDto(
                    it.productId.toInt(),
                    it.quantity.toInt()
                )
            },
            now
        )
    }
}