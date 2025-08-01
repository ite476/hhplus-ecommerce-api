package kr.hhplus.be.server.repository.jpa.repository.product

import kr.hhplus.be.server.service.product.entity.ProductSaleSummary
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import java.time.ZonedDateTime

interface ProductQueryRepository {

    fun findPopularProducts(
        searchFrom: ZonedDateTime,
        searchUntil: ZonedDateTime,
        pageable: Pageable
    ): Page<ProductSaleSummary>
}
