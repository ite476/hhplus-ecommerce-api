package kr.hhplus.be.server.controller.v1.product

import com.ninjasquad.springmockk.MockkBean
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.clearAllMocks
import io.mockk.every
import kr.hhplus.be.server.controller.advise.GlobalExceptionHandler
import kr.hhplus.be.server.controller.v1.product.response.GetProductsPopularResponse
import kr.hhplus.be.server.controller.v1.product.response.GetProductsResponse
import kr.hhplus.be.server.service.pagination.PagedList
import kr.hhplus.be.server.service.product.entity.Product
import kr.hhplus.be.server.service.product.entity.ProductSaleSummary
import kr.hhplus.be.server.service.product.service.ProductService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.HttpStatus
import org.springframework.test.web.client.MockMvcClientHttpRequestFactory
import org.springframework.test.web.servlet.MockMvc
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientResponseException
import java.time.ZoneId
import java.time.ZonedDateTime

@WebMvcTest(ProductController::class)
@Import(GlobalExceptionHandler::class)
@DisplayName("ProductController 테스트")
class ProductControllerTest {
    @MockkBean
    lateinit var productService: ProductService

    private val getProductsEndpoint = "/api/v1/products"
    private val getPopularProductsEndpoint = "/api/v1/products/popular"
    private val fixedTime = ZonedDateTime.of(2024, 1, 15, 10, 30, 0, 0, ZoneId.of("Asia/Seoul"))

    @Autowired
    lateinit var mockMvc: MockMvc
    lateinit var restClient: RestClient

    @BeforeEach
    fun setUp() {
        clearAllMocks()
        restClient = RestClient.builder()
            .requestFactory(MockMvcClientHttpRequestFactory(mockMvc))
            .build()
    }

    @Test
    @DisplayName("정상적인 상품 목록 조회 요청 - 상품 목록이 성공적으로 반환된다")
    fun getProducts_ValidRequest_ReturnsSuccessResponse() {
        // Given
        val products = listOf(
            Product(1L, "아메리카노", 4500L, 100, fixedTime),
            Product(2L, "라떼", 5000L, 50, fixedTime),
            Product(3L, "카푸치노", 5500L, 30, fixedTime)
        )
        
        every { productService.findPagedProducts(any()) } returns PagedList(
            items = products,
            page = 0,
            size = 10,
            totalCount = 3
        )

        // When & Then
        val response = restClient.get()
            .uri {
                it.path(getProductsEndpoint)
                    .queryParam("page", 0)
                    .queryParam("size", 10)
                    .build()
            }
            .retrieve()
            .toEntity(GetProductsResponse::class.java)

        response.statusCode shouldBe HttpStatus.OK
        response.body.let { body ->
            body shouldNotBe null
            body?.run {
                products.size shouldBe 3
                products[0].id shouldBe 1L
                products[0].name shouldBe "아메리카노"
                products[0].price shouldBe 4500L
                products[0].stock shouldBe 100
                products[1].id shouldBe 2L
                products[1].name shouldBe "라떼"
                products[2].id shouldBe 3L
                products[2].name shouldBe "카푸치노"
            }
        }
    }

    @Test
    @DisplayName("상품이 없는 경우 목록 조회 - 빈 목록이 성공적으로 반환된다")
    fun getProducts_NoProducts_ReturnsEmptyList() {
        // Given
        every { productService.findPagedProducts(any()) } returns PagedList(
            items = emptyList(),
            page = 0,
            size = 10,
            totalCount = 0
        )

        // When & Then
        val response = restClient.get()
            .uri {
                it.path(getProductsEndpoint)
                    .queryParam("page", 0)
                    .queryParam("size", 10)
                    .build()
            }
            .retrieve()
            .toEntity(GetProductsResponse::class.java)

        response.statusCode shouldBe HttpStatus.OK
        response.body.let { body ->
            body shouldNotBe null
            body?.run {
                products.isEmpty() shouldBe true
                totalCount shouldBe 0
            }
        }
    }

    @Test
    @DisplayName("정상적인 인기 상품 조회 요청 - 인기 상품 목록이 순위별로 성공적으로 반환된다")
    fun getPopularProducts_ValidRequest_ReturnsSuccessResponse() {
        // Given
        val popularProducts = listOf(
            ProductSaleSummary(
                product = Product(1L, "아메리카노", 4500L, 100, fixedTime),
                rank = 1,
                soldCount = 1200,
                from = fixedTime.minusDays(3),
                until = fixedTime
            ),
            ProductSaleSummary(
                product = Product(2L, "라떼", 5000L, 50, fixedTime),
                rank = 2,
                soldCount = 800,
                from = fixedTime.minusDays(3),
                until = fixedTime
            ),
            ProductSaleSummary(
                product = Product(3L, "카푸치노", 5500L, 30, fixedTime),
                rank = 3,
                soldCount = 600,
                from = fixedTime.minusDays(3),
                until = fixedTime
            )
        )
        
        every { productService.findPagedPopularProducts(any()) } returns PagedList(
            items = popularProducts,
            page = 0,
            size = 10,
            totalCount = 3
        )

        // When & Then
        val response = restClient.get()
            .uri {
                it.path(getPopularProductsEndpoint)
                    .queryParam("page", 0)
                    .queryParam("size", 10)
                    .build()
            }
            .retrieve()
            .toEntity(GetProductsPopularResponse::class.java)

        response.statusCode shouldBe HttpStatus.OK
        response.body.let { body ->
            body shouldNotBe null
            body?.run {
                products.size shouldBe 3
                products[0].id shouldBe 1L
                products[0].name shouldBe "아메리카노"
                products[0].rank shouldBe 1
                products[0].sold shouldBe 1200
                products[1].rank shouldBe 2
                products[1].sold shouldBe 800
                products[2].rank shouldBe 3
                products[2].sold shouldBe 600
            }
        }
    }

    @Test
    @DisplayName("인기 상품이 없는 경우 조회 - 빈 목록이 성공적으로 반환된다")
    fun getPopularProducts_NoPopularProducts_ReturnsEmptyList() {
        // Given
        every { productService.findPagedPopularProducts(any()) } returns PagedList(
            items = emptyList(),
            page = 0,
            size = 10,
            totalCount = 0
        )

        // When & Then
        val response = restClient.get()
            .uri {
                it.path(getPopularProductsEndpoint)
                    .queryParam("page", 0)
                    .queryParam("size", 10)
                    .build()
            }
            .retrieve()
            .toEntity(GetProductsPopularResponse::class.java)

        response.statusCode shouldBe HttpStatus.OK
        response.body.let { body ->
            body shouldNotBe null
            body?.run {
                products.isEmpty() shouldBe true
                totalCount shouldBe 0
            }
        }
    }

    @Test
    @DisplayName("잘못된 페이지 번호로 상품 목록 조회 - 400 Bad Request를 반환한다")
    fun getProducts_InvalidPageNumber_Returns400Error() {
        // When & Then
        try {
            restClient.get()
                .uri {
                    it.path(getProductsEndpoint)
                        .queryParam("page", -1)
                        .queryParam("size", 10)
                        .build()
                }
                .retrieve()
                .toEntity(GetProductsResponse::class.java)
        } catch (e: RestClientResponseException) {
            e.statusCode shouldBe HttpStatus.BAD_REQUEST
        }
    }

    @Test
    @DisplayName("잘못된 페이지 크기로 상품 목록 조회 - 400 Bad Request를 반환한다")
    fun getProducts_InvalidPageSize_Returns400Error() {
        // When & Then
        try {
            restClient.get()
                .uri {
                    it.path(getProductsEndpoint)
                        .queryParam("page", 0)
                        .queryParam("size", 0)
                        .build()
                }
                .retrieve()
                .toEntity(GetProductsResponse::class.java)
        } catch (e: RestClientResponseException) {
            e.statusCode shouldBe HttpStatus.BAD_REQUEST
        }
    }

    @Test
    @DisplayName("잘못된 페이지 번호로 인기 상품 조회 - 400 Bad Request를 반환한다")
    fun getPopularProducts_InvalidPageNumber_Returns400Error() {
        // When & Then
        try {
            restClient.get()
                .uri {
                    it.path(getPopularProductsEndpoint)
                        .queryParam("page", -1)
                        .queryParam("size", 10)
                        .build()
                }
                .retrieve()
                .toEntity(GetProductsPopularResponse::class.java)
        } catch (e: RestClientResponseException) {
            e.statusCode shouldBe HttpStatus.BAD_REQUEST
        }
    }

    @Test
    @DisplayName("시스템 오류로 상품 목록 조회 실패 - 500 Internal Server Error를 반환한다")
    fun getProducts_SystemError_Returns500Error() {
        // Given
        every { productService.findPagedProducts(any()) } throws 
            RuntimeException("시스템 오류가 발생했습니다.")

        // When & Then
        try {
            restClient.get()
                .uri {
                    it.path(getProductsEndpoint)
                        .queryParam("page", 0)
                        .queryParam("size", 10)
                        .build()
                }
                .retrieve()
                .toEntity(GetProductsResponse::class.java)
        } catch (e: RestClientResponseException) {
            e.statusCode shouldBe HttpStatus.INTERNAL_SERVER_ERROR
        }
    }

    @Test
    @DisplayName("시스템 오류로 인기 상품 조회 실패 - 500 Internal Server Error를 반환한다")
    fun getPopularProducts_SystemError_Returns500Error() {
        // Given
        every { productService.findPagedPopularProducts(any()) } throws 
            RuntimeException("시스템 오류가 발생했습니다.")

        // When & Then
        try {
            restClient.get()
                .uri {
                    it.path(getPopularProductsEndpoint)
                        .queryParam("page", 0)
                        .queryParam("size", 10)
                        .build()
                }
                .retrieve()
                .toEntity(GetProductsPopularResponse::class.java)
        } catch (e: RestClientResponseException) {
            e.statusCode shouldBe HttpStatus.INTERNAL_SERVER_ERROR
        }
    }
}