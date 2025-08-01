package kr.hhplus.be.server.repository.product

import kr.hhplus.be.server.repository.jpa.repository.product.PopularProductJpaRepository
import kr.hhplus.be.server.repository.jpa.repository.product.ProductJpaRepository
import kr.hhplus.be.server.repository.pagination.toPageable
import kr.hhplus.be.server.repository.pagination.toPagedList
import kr.hhplus.be.server.service.pagination.PagedList
import kr.hhplus.be.server.service.pagination.PagingOptions
import kr.hhplus.be.server.service.product.entity.Product
import kr.hhplus.be.server.service.product.entity.ProductSaleSummary
import kr.hhplus.be.server.service.product.exception.ProductNotFoundException
import kr.hhplus.be.server.service.product.port.ProductPort
import kr.hhplus.be.server.util.KoreanTimeProvider
import kr.hhplus.be.server.util.unwrapOrThrow
import org.springframework.stereotype.Component
import java.time.Duration
import java.time.ZonedDateTime

@Component
class ProductAdapter(
    private val productRepository: ProductJpaRepository,
    private val popularProductRepository: PopularProductJpaRepository,
    val timeProvider: KoreanTimeProvider
) : ProductPort {
    override fun findPagedProducts(pagingOptions: PagingOptions): PagedList<Product> {
        val products: PagedList<Product> = productRepository.findAll(pagingOptions.toPageable())
            .map { it.toDomain(timeProvider) }
            .toPagedList()

        return products
    }

    override fun findPagedPopularProducts(
        whenSearch: ZonedDateTime,
        searchPeriod: Duration,
        pagingOptions: PagingOptions
    ): PagedList<ProductSaleSummary> {
        val searchFrom = whenSearch - searchPeriod
        val searchUntil = whenSearch

        val populars: PagedList<ProductSaleSummary> = popularProductRepository.findPopularProducts(
            searchFrom = searchFrom,
            searchUntil = searchUntil,
            pageable = pagingOptions.toPageable()
        ).toPagedList()

        return populars
    }

    override fun findProductById(productId: Long): Product {
        val productEntity = productRepository.findById(productId)
            .unwrapOrThrow {
                ProductNotFoundException()
            }

        return productEntity.toDomain(timeProvider)
    }

    override fun saveProduct(product: Product) {
        val productId = requireNotNull(product.id)

        val productEntity = productRepository.findById(productId)
            .unwrapOrThrow {
                ProductNotFoundException()
            }

        productEntity.updateAsDomain(product)

        productRepository.save(productEntity)
    }

    override fun existsProduct(productId: Long): Boolean {
        val existsById = productRepository.existsById(productId)

        return existsById
    }
}