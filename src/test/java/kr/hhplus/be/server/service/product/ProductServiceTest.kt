package kr.hhplus.be.server.service.product

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.verify
import kr.hhplus.be.server.service.ServiceTestBase
import kr.hhplus.be.server.service.pagination.PagedList
import kr.hhplus.be.server.service.product.entity.Product
import kr.hhplus.be.server.service.product.entity.ProductSaleSummary
import kr.hhplus.be.server.service.product.exception.LackOfProductStockException
import kr.hhplus.be.server.service.product.port.ProductPort
import kr.hhplus.be.server.service.product.service.ProductService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.Duration

@DisplayName("ProductService 단위테스트")
class ProductServiceTest : ServiceTestBase() {

    @MockK
    private lateinit var productPort: ProductPort
    
    private lateinit var productService: ProductService

    @BeforeEach
    fun setupProductService() {
        super.setUp()
        productService = ProductService(productPort, timeProvider)
    }

    @Nested
    @DisplayName("findAllProducts 메서드는")
    inner class ReadProductsTest {

        @Test
        @DisplayName("모든 상품 목록을 반환한다")
        fun returnsAllProducts() {
            // given
            val expectedProducts = listOf(
                Product(1L, "아메리카노", 4500L, 100, fixedTime),
                Product(2L, "라떼", 5000L, 50, fixedTime)
            )
            every { productPort.findPagedProducts(any()) } returns PagedList(
                items = expectedProducts,
                page = pagingOptions.page,
                size = pagingOptions.size,
                totalCount = 2
            )

            // when
            val result = productService.findPagedProducts(pagingOptions).items

            // then
            result shouldBe expectedProducts
            verify { productPort.findPagedProducts(any()) }
        }

        @Test
        @DisplayName("상품이 없을 때 빈 리스트를 반환한다")
        fun returnsEmptyListWhenNoProducts() {
            // given

            every { productPort.findPagedProducts(any()) } returns PagedList(
                items = emptyList(),
                page = pagingOptions.page,
                size = pagingOptions.size,
                totalCount = 0
            )

            // when
            val result = productService.findPagedProducts(pagingOptions).items

            // then
            result shouldBe emptyList()
            verify { productPort.findPagedProducts(any()) }
        }
    }

    @Nested
    @DisplayName("findProductById 메서드는")
    inner class ReadSingleProductTest {

        @Test
        @DisplayName("특정 상품 정보를 반환한다")
        fun returnsSingleProduct() {
            // given
            val productId = 1L
            val expectedProduct = Product(productId, "아메리카노", 4500L, 100, fixedTime)

            every { productPort.existsProduct((productId)) } returns true
            every { productPort.findProductById(productId) } returns expectedProduct

            // when
            val result = productService.findProductById(productId)

            // then
            result shouldBe expectedProduct
            verify { productPort.findProductById(productId) }
        }
    }

    @Nested
    @DisplayName("findAllPopularProducts 메서드는")
    inner class ReadPopularProductsTest {

        @Test
        @DisplayName("인기 상품 목록을 반환한다")
        fun returnsPopularProducts() {
            // given
            val expectedProducts = listOf(
                ProductSaleSummary(
                    product = Product(id = 1L, name = "아메리카노", price = 4500L, stock = 100, createdAt = fixedTime),
                    rank = 1, soldCount = 1200, from = fixedTime.minusDays(3), until = fixedTime
                ),
                ProductSaleSummary(
                    product = Product(id = 2L, name = "라떼", price = 5000L, stock = 50, createdAt = fixedTime),
                    rank = 2, soldCount = 800, from = fixedTime.minusDays(3), until = fixedTime
                )
            )
            every { 
                productPort.findPagedPopularProducts(
                    whenSearch = fixedTime,
                    searchPeriod = Duration.ofDays(3),
                    pagingOptions = pagingOptions
                ) 
            } returns PagedList(
                items = expectedProducts,
                page = pagingOptions.page,
                size = pagingOptions.size,
                totalCount = 2
            )

            // when
            val result = productService.findPagedPopularProducts(pagingOptions).items

            // then
            result shouldBe expectedProducts
            verify { 
                productPort.findPagedPopularProducts(
                    whenSearch = fixedTime,
                    searchPeriod = Duration.ofDays(3),
                    pagingOptions = any()
                ) 
            }
        }
    }

    @Nested
    @DisplayName("addProductStock 메서드는")
    inner class AddProductStockTest {

        @Test
        @DisplayName("상품 재고를 증가시킨다")
        fun increasesProductStock() {
            // given
            val productId = 1L
            val quantity = 50L
            val product = Product(productId, "아메리카노", 4500L, 100, fixedTime)

            every { productPort.existsProduct((productId)) } returns true
            every { productPort.findProductById(productId) } returns product
            every { productPort.saveProduct(any()) } returns Unit

            // when
            productService.addProductStock(productId, quantity, fixedTime)

            // then
            product.stock shouldBe 150 // 100 + 50
            verify { productPort.findProductById(productId) }
            verify { productPort.saveProduct(product) }
        }
    }

    @Nested
    @DisplayName("reduceProductStock 메서드는")
    inner class ReduceProductStockTest {

        @Test
        @DisplayName("상품 재고를 감소시킨다")
        fun reducesProductStock() {
            // given
            val productId = 1L
            val quantity = 30L
            val product = Product(productId, "아메리카노", 4500L, 100, fixedTime)

            every { productPort.existsProduct((productId)) } returns true
            every { productPort.findProductById(productId) } returns product
            every { productPort.saveProduct(any()) } returns Unit

            // when
            productService.reduceProductStock(productId, quantity, fixedTime)

            // then
            product.stock shouldBe 70 // 100 - 30
            verify { productPort.findProductById(productId) }
            verify { productPort.saveProduct(product) }
        }

        @Test
        @DisplayName("재고가 부족할 때 예외를 던진다")
        fun throwsExceptionWhenInsufficientStock() {
            // given
            val productId = 1L
            val quantity = 150L // 재고(100)보다 많음
            val product = Product(productId, "아메리카노", 4500L, 100, fixedTime)

            every { productPort.existsProduct((productId)) } returns true
            every { productPort.findProductById(productId) } returns product

            // when & then
            shouldThrow<LackOfProductStockException> {
                productService.reduceProductStock(productId, quantity, fixedTime)
            }
            
            verify { productPort.findProductById(productId) }
            verify(exactly = 0) { productPort.saveProduct(any()) }
        }
    }

    @Nested
    @DisplayName("Product Entity 로직 테스트")
    inner class ProductEntityTest {

        @Test
        @DisplayName("addStock은 재고를 증가시킨다")
        fun addStockIncreasesStock() {
            // given
            val product = Product(1L, "아메리카노", 4500L, 100, fixedTime)

            // when
            product.addStock(50, fixedTime)

            // then
            product.stock shouldBe 150
        }

        @Test
        @DisplayName("reduceStock은 재고를 감소시킨다")
        fun reduceStockDecreasesStock() {
            // given
            val product = Product(1L, "아메리카노", 4500L, 100, fixedTime)

            // when
            product.reduceStock(30, fixedTime)

            // then
            product.stock shouldBe 70
        }

        @Test
        @DisplayName("reduceStock은 재고가 0 미만이 되면 예외를 던진다")
        fun reduceStockThrowsExceptionWhenStockBecomesZero() {
            // given
            val product = Product(1L, "아메리카노", 4500L, 100, fixedTime)

            // when & then
            shouldThrow<LackOfProductStockException> {
                product.reduceStock(101, fixedTime) // 재고가 정확히 0이 됨
            }
        }

        @Test
        @DisplayName("reduceStock은 재고가 음수가 되면 예외를 던진다")
        fun reduceStockThrowsExceptionWhenStockBecomesNegative() {
            // given
            val product = Product(1L, "아메리카노", 4500L, 50, fixedTime)

            // when & then
            shouldThrow<LackOfProductStockException> {
                product.reduceStock(100, fixedTime) // 재고가 음수가 됨
            }
        }
    }
} 