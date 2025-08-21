package kr.hhplus.be.server.repository.jpa.repository.product

import com.querydsl.jpa.impl.JPAQueryFactory
import kr.hhplus.be.server.repository.jpa.entity.order.QOrderItemEntity
import kr.hhplus.be.server.repository.jpa.entity.product.ProductEntity
import kr.hhplus.be.server.repository.jpa.entity.product.QProductEntity
import kr.hhplus.be.server.service.product.entity.Product
import kr.hhplus.be.server.service.product.entity.ProductSaleSummary
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.support.PageableExecutionUtils
import org.springframework.stereotype.Repository
import java.time.Duration
import java.time.ZoneOffset
import java.time.ZonedDateTime

@Repository
class PopularProductJpaRepository(
    private val queryFactory: JPAQueryFactory
) : ProductQueryRepository {
    // QueryDSL Q클래스 인스턴스 정의
    private val productEntity = QProductEntity.productEntity
    private val orderItemEntity = QOrderItemEntity.orderItemEntity

    /**
     * 인기 상품 조회
     * - 판매량(SUM oi.quantity) 기준 내림차순
     */
    override fun findPopularProducts(
        searchFrom: ZonedDateTime,
        searchUntil: ZonedDateTime,
        pageable: Pageable
    ): Page<ProductSaleSummary> {
        // 집계 쿼리 - ProductEntity 전체를 가져와서 나중에 Product로 변환
         val selectQuery = queryFactory
            .select(
                productEntity,
                orderItemEntity.quantity.sum(), // soldCount: Long
                com.querydsl.core.types.dsl.Expressions.constant(searchFrom),
                com.querydsl.core.types.dsl.Expressions.constant(searchUntil)
            )
            .from(orderItemEntity)
            .join(orderItemEntity.product, productEntity)
            .where(
                orderItemEntity.deletedAt.isNull,
                orderItemEntity.createdAt.between(searchFrom.toInstant(), searchUntil.toInstant())
            )
            .groupBy(productEntity.id)
            .orderBy(orderItemEntity.quantity.sum().desc())
            .offset(pageable.offset)
            .limit(pageable.pageSize.toLong())
        val results = selectQuery.fetch()

        // ProductEntity를 Product로 변환하고 ProductSaleSummary 생성
        val productSaleSummaries = results.mapIndexed { index, tuple ->
            val productEntity = tuple.get(0, ProductEntity::class.java)!!
            val soldCount = tuple.get(1, Long::class.java)!!
            val from = tuple.get(2, ZonedDateTime::class.java)!!
            val until = tuple.get(3, ZonedDateTime::class.java)!!

            val product = Product(
                id = productEntity.id,
                name = productEntity.name,
                price = productEntity.price,
                stock = productEntity.stock,
                createdAt = productEntity.createdAt?.atZone(ZoneOffset.UTC)
                    ?: from // createdAt이 null이면 검색 시작일로 대체
            )

            ProductSaleSummary(
                product = product,
                rank = index + 1 + pageable.offset.toInt(), // 순위 계산
                soldCount = soldCount,
                from = from,
                until = until
            )
        }

        // count 쿼리
        val countQuery = queryFactory
            .select(orderItemEntity.product.id.countDistinct())
            .from(orderItemEntity)
            .where(
                orderItemEntity.deletedAt.isNull,
                orderItemEntity.createdAt.between(searchFrom.toInstant(), searchUntil.toInstant())
            )


        return PageableExecutionUtils.getPage(productSaleSummaries, pageable) { countQuery.fetchOne() ?: 0L }
    }

    override fun findPopularProductsByDate(
        searchFrom: ZonedDateTime,
        searchUntil: ZonedDateTime
    ): List<Pair<ZonedDateTime, List<ProductSaleSummary>>> {
        val dailySummaries = mutableListOf<Pair<ZonedDateTime, List<ProductSaleSummary>>>()

        val dayCount = Duration.between(searchFrom.toLocalDate().atStartOfDay(searchFrom.zone), searchUntil).toDays()

        val maxRowPerDay = 500 // 일자당 최대 500개 (보수적으로 제한)
        val approxRowSize = 200 // 평균 row 크기 (Byte) 가정 → 200B * 500 = 약 100KB
        val maxTotalSize = 100 * 1024 * 1024 // 100MB 제한

        var estimatedTotalSize = 0

        for (i in 0..dayCount) {
            val dayStart = searchFrom.toLocalDate().plusDays(i).atStartOfDay(searchFrom.zone)
            val dayEnd = dayStart.plusDays(1)

            val query = queryFactory
                .select(
                    productEntity,
                    orderItemEntity.quantity.sum(), // soldCount
                    com.querydsl.core.types.dsl.Expressions.constant(dayStart),
                    com.querydsl.core.types.dsl.Expressions.constant(dayEnd)
                )
                .from(orderItemEntity)
                .join(orderItemEntity.product, productEntity)
                .where(
                    orderItemEntity.deletedAt.isNull,
                    orderItemEntity.createdAt.between(dayStart.toInstant(), dayEnd.toInstant())
                )
                .groupBy(productEntity.id)
                .orderBy(orderItemEntity.quantity.sum().desc())
                .limit(maxRowPerDay.toLong())

            val tuples = query.fetch()

            if (tuples.isEmpty()) continue

            val summaries = tuples.mapIndexed { index, tuple ->
                val productEntity = tuple.get(0, ProductEntity::class.java)!!
                val soldCount = tuple.get(1, Long::class.java)!!
                val from = tuple.get(2, ZonedDateTime::class.java)!!
                val until = tuple.get(3, ZonedDateTime::class.java)!!

                val product = Product(
                    id = productEntity.id,
                    name = productEntity.name,
                    price = productEntity.price,
                    stock = productEntity.stock,
                    createdAt = productEntity.createdAt?.atZone(ZoneOffset.UTC) ?: from
                )

                ProductSaleSummary(
                    product = product,
                    rank = index + 1,
                    soldCount = soldCount,
                    from = from,
                    until = until
                )
            }

            // 메모리 사용량 추정 (row 당 평균 200B 기준)
            estimatedTotalSize += summaries.size * approxRowSize
            if (estimatedTotalSize >= maxTotalSize) break

            dailySummaries += dayStart to summaries
        }

        return dailySummaries
    }
}