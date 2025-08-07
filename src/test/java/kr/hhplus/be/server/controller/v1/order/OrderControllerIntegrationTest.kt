package kr.hhplus.be.server.controller.v1.order

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import kr.hhplus.be.server.TestcontainersConfiguration
import kr.hhplus.be.server.config.jpa.JpaConfig
import kr.hhplus.be.server.controller.v1.order.request.OrderItemRequest
import kr.hhplus.be.server.controller.v1.order.request.PostOrderRequestBody
import kr.hhplus.be.server.controller.v1.order.response.PostOrderResponse
import kr.hhplus.be.server.repository.jpa.entity.product.ProductEntity
import kr.hhplus.be.server.repository.jpa.entity.user.UserEntity
import kr.hhplus.be.server.repository.jpa.repository.coupon.CouponJpaRepository
import kr.hhplus.be.server.repository.jpa.repository.product.ProductJpaRepository
import kr.hhplus.be.server.repository.jpa.repository.user.UserJpaRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.context.annotation.Import
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.test.context.ActiveProfiles
import org.testcontainers.junit.jupiter.Testcontainers

/**
 * 주문 동시성 통합 테스트
 * 
 * 이 테스트는 실제 DB와 전체 애플리케이션 컨텍스트를 사용하여
 * 주문 생성의 동시성 문제를 검증합니다.
 * 
 * 주요 검증 사항:
 * - 재고 차감의 동시성 안전성
 * - 포인트 차감의 동시성 안전성
 * - 복합 자원 경합 상황에서의 정확한 롤백
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@Import(TestcontainersConfiguration::class, JpaConfig::class)
@ActiveProfiles("test")
@DisplayName("주문 동시성 통합 테스트")
class OrderControllerIntegrationTest {

    @LocalServerPort
    private var port: Int = 0

    @Autowired
    private lateinit var restTemplate: TestRestTemplate

    @Autowired
    private lateinit var userRepository: UserJpaRepository

    @Autowired
    private lateinit var productRepository: ProductJpaRepository

    @Autowired
    private lateinit var couponRepository: CouponJpaRepository

    private lateinit var testUsers: List<UserEntity>
    private lateinit var testProduct: ProductEntity
    private lateinit var limitedStockProduct: ProductEntity
    private val baseUrl get() = "http://localhost:$port/api/v1/orders"

    @BeforeEach
    fun setUp() {
        // 테스트용 사용자들 생성 (충분한 포인트)
        testUsers = (1..15).map { i ->
            userRepository.save(
                UserEntity(
                    name = "주문테스트사용자$i",
                    point = 50000L // 충분한 포인트
                )
            )
        }

        // 일반 상품 생성 (충분한 재고)
        testProduct = productRepository.save(
            ProductEntity(
                name = "일반상품",
                price = 10000L,
                stock = 100
            )
        )

        // 한정 재고 상품 생성 (재고 5개)
        limitedStockProduct = productRepository.save(
            ProductEntity(
                name = "한정재고상품",
                price = 15000L,
                stock = 5
            )
        )
    }

    @Test
    @DisplayName("동시 주문 성공 시나리오 - 충분한 재고와 포인트가 있을 때 모든 주문이 성공해야 한다")
    fun `concurrent orders with sufficient resources should all succeed`() = runBlocking {
        // Given: 10명의 사용자가 동시에 일반 상품 주문
        val concurrentUsers = testUsers.take(10)
        val orderRequest = PostOrderRequestBody(
            orderItems = listOf(
                OrderItemRequest(
                    productId = testProduct.id!!,
                    quantity = 1
                )
            ),
            userCouponId = null
        )

        // When: 10명이 동시에 주문
        val startTime = System.currentTimeMillis()
        val results = concurrentUsers.map { user ->
            async {
                try {
                    val response = createOrder(user.id!!, orderRequest)
                    "SUCCESS" to response
                } catch (e: Exception) {
                    "FAILED" to e.message
                }
            }
        }.awaitAll()
        val endTime = System.currentTimeMillis()

        // Then: 모든 주문이 성공해야 함
        val successCount = results.count { it.first == "SUCCESS" }
        val failedCount = results.count { it.first == "FAILED" }

        val requestSize = concurrentUsers.size
        val elapsedMillis = endTime - startTime
        val elapsedSeconds = elapsedMillis.toDouble() / 1000.0
        val tps = requestSize / elapsedSeconds
        val averageElapsedMillis = ( elapsedMillis * 1.0 ) / (requestSize)

        println("🔍 동시 주문 성공 시나리오 테스트 결과:")
        println("   주문 시도: ${concurrentUsers.size}건")
        println("   성공: ${successCount}건")
        println("   실패: ${failedCount}건")
        println("   총 처리 시간: ${elapsedMillis}ms")
        println("   평균 처리 시간: %.2fms".format(averageElapsedMillis))
        println("   TPS: %.2f".format(tps))

        // 재고 및 포인트 확인
        val updatedProduct = productRepository.findById(testProduct.id!!).get()

        // 검증: 모든 주문 성공
        successCount shouldBe 10
        failedCount shouldBe 0
        updatedProduct.stock shouldBe (testProduct.stock - 10) // 재고 정확히 차감

        println("   상품 재고 변화: ${testProduct.stock} → ${updatedProduct.stock}")
    }

    @Test
    @DisplayName("한정 재고 동시 주문 테스트 - 재고 부족 시 일부만 성공해야 한다")
    fun `concurrent orders with limited stock should partially succeed`() = runBlocking {
        // Given: 10명의 사용자가 재고 5개인 상품에 동시 주문
        val concurrentUsers = testUsers.take(10)
        val orderRequest = PostOrderRequestBody(
            orderItems = listOf(
                OrderItemRequest(
                    productId = limitedStockProduct.id!!,
                    quantity = 1
                )
            ),
            userCouponId = null
        )

        // When: 10명이 동시에 한정 재고 상품 주문
        val startTime = System.currentTimeMillis()
        val results = concurrentUsers.map { user ->
            async {
                try {
                    val response = createOrder(user.id!!, orderRequest)
                    "SUCCESS" to response
                } catch (e: Exception) {
                    "FAILED" to e.message
                }
            }
        }.awaitAll()
        val endTime = System.currentTimeMillis()

        // Then: 재고만큼만 성공해야 함
        val successCount = results.count { it.first == "SUCCESS" }
        val failedCount = results.count { it.first == "FAILED" }

        val requestSize = concurrentUsers.size
        val elapsedMillis = endTime - startTime
        val elapsedSeconds = elapsedMillis.toDouble() / 1000.0
        val tps = requestSize / elapsedSeconds
        val averageElapsedMillis = ( elapsedMillis * 1.0 ) / (requestSize)

        println("🔍 한정 재고 동시 주문 테스트 결과:")
        println("   주문 시도: ${concurrentUsers.size}건")
        println("   성공: ${successCount}건")
        println("   실패: ${failedCount}건")
        println("   총 처리 시간: ${elapsedMillis}ms")
        println("   평균 처리 시간: %.2fms".format(averageElapsedMillis))
        println("   TPS: %.2f".format(tps))

        // 재고 확인
        val updatedProduct = productRepository.findById(limitedStockProduct.id!!).get()

        // 검증: 재고만큼만 성공
        successCount shouldBe 5
        failedCount shouldBe 5
        updatedProduct.stock shouldBe 0 // 재고 완전 소진

        println("   상품 재고 변화: ${limitedStockProduct.stock} → ${updatedProduct.stock}")
    }

    @Test
    @DisplayName("포인트 부족 동시 주문 테스트 - 포인트 부족 시 주문이 실패해야 한다")
    fun `concurrent orders with insufficient points should fail`() = runBlocking {
        // Given: 포인트가 부족한 사용자들 생성
        val poorUsers = (1..5).map { i ->
            userRepository.save(
                UserEntity(
                    name = "포인트부족사용자$i",
                    point = 5000L // 상품 가격(10000원)보다 적음
                )
            )
        }

        val orderRequest = PostOrderRequestBody(
            orderItems = listOf(
                OrderItemRequest(
                    productId = testProduct.id!!,
                    quantity = 1
                )
            ),
            userCouponId = null
        )

        // When: 포인트 부족한 사용자들이 주문 시도
        val startTime = System.currentTimeMillis()
        val results = poorUsers.map { user ->
            async {
                try {
                    val response = createOrder(user.id!!, orderRequest)
                    "SUCCESS" to response
                } catch (e: Exception) {
                    "FAILED" to e.message
                }
            }
        }.awaitAll()
        val endTime = System.currentTimeMillis()

        // Then: 모든 주문이 실패해야 함
        val successCount = results.count { it.first == "SUCCESS" }
        val failedCount = results.count { it.first == "FAILED" }

        val requestSize = poorUsers.size
        val elapsedMillis = endTime - startTime
        val elapsedSeconds = elapsedMillis.toDouble() / 1000.0
        val tps = requestSize / elapsedSeconds
        val averageElapsedMillis = ( elapsedMillis * 1.0 ) / (requestSize)

        println("🔍 포인트 부족 동시 주문 테스트 결과:")
        println("   주문 시도: ${poorUsers.size}건")
        println("   성공: ${successCount}건")
        println("   실패: ${failedCount}건")
        println("   총 처리 시간: ${elapsedMillis}ms")
        println("   평균 처리 시간: %.2fms".format(averageElapsedMillis))
        println("   TPS: %.2f".format(tps))

        // 재고 변화 확인 (변화 없어야 함)
        val updatedProduct = productRepository.findById(testProduct.id!!).get()

        // 검증: 모든 주문 실패, 재고 변화 없음
        successCount shouldBe 0
        failedCount shouldBe 5
        updatedProduct.stock shouldBe testProduct.stock // 재고 변화 없음

        println("   상품 재고 변화: ${testProduct.stock} → ${updatedProduct.stock}")
    }

    @Test
    @DisplayName("복합 자원 경합 테스트 - 재고와 포인트 부족이 섞인 상황에서 정확한 처리")
    fun `mixed resource contention should handle correctly`() = runBlocking {
        // Given: 다양한 상황의 사용자들
        val richUsers = testUsers.take(3) // 충분한 포인트
        val poorUsers = (1..3).map { i ->
            userRepository.save(
                UserEntity(
                    name = "혼합테스트포인트부족$i",
                    point = 5000L
                )
            )
        }
        val mixedUsers = richUsers + poorUsers

        val orderRequest = PostOrderRequestBody(
            orderItems = listOf(
                OrderItemRequest(
                    productId = limitedStockProduct.id!!, // 재고 5개 상품
                    quantity = 1
                )
            ),
            userCouponId = null
        )

        // When: 6명이 재고 5개 상품에 동시 주문 (3명은 포인트 부족)
        val startTime = System.currentTimeMillis()
        val results = mixedUsers.map { user ->
            async {
                try {
                    val response = createOrder(user.id!!, orderRequest)
                    "SUCCESS" to response
                } catch (e: Exception) {
                    "FAILED" to e.message
                }
            }
        }.awaitAll()
        val endTime = System.currentTimeMillis()

        // Then: 포인트 충분한 사용자 중 재고만큼만 성공
        val successCount = results.count { it.first == "SUCCESS" }
        val failedCount = results.count { it.first == "FAILED" }

        val requestSize = mixedUsers.size
        val elapsedMillis = endTime - startTime
        val elapsedSeconds = elapsedMillis.toDouble() / 1000.0
        val tps = requestSize / elapsedSeconds
        val averageElapsedMillis = ( elapsedMillis * 1.0 ) / (requestSize)

        println("🔍 복합 자원 경합 테스트 결과:")
        println("   주문 시도: ${mixedUsers.size}건 (포인트 충분: 3명, 부족: 3명)")
        println("   성공: ${successCount}건")
        println("   실패: ${failedCount}건")
        println("   처리 시간: ${endTime - startTime}ms")
        println("   총 처리 시간: ${elapsedMillis}ms")
        println("   평균 처리 시간: %.2fms".format(averageElapsedMillis))
        println("   TPS: %.2f".format(tps))

        // 재고 확인
        val updatedProduct = productRepository.findById(limitedStockProduct.id!!).get()

        // 검증: 성공 건수는 재고 수량 이하이고, 포인트 부족은 반드시 실패
        successCount shouldBe 3 // 포인트 충분한 사용자 3명 모두 성공 (재고가 5개이므로)
        failedCount shouldBe 3 // 포인트 부족한 사용자 3명 모두 실패
        updatedProduct.stock shouldBe (limitedStockProduct.stock - successCount)

        println("   상품 재고 변화: ${limitedStockProduct.stock} → ${updatedProduct.stock}")
    }

    @Test
    @DisplayName("대량 동시 주문 성능 테스트 - 높은 동시성 상황에서의 처리 성능 검증")
    fun `high concurrency order performance test`() = runBlocking {
        // Given: 대량 주문을 위한 높은 재고 상품
        val highStockProduct = productRepository.save(
            ProductEntity(
                name = "대량재고상품",
                price = 5000L,
                stock = 50
            )
        )

        val orderRequest = PostOrderRequestBody(
            orderItems = listOf(
                OrderItemRequest(
                    productId = highStockProduct.id!!,
                    quantity = 1
                )
            ),
            userCouponId = null
        )

        // When: 15명이 동시에 주문
        val startTime = System.currentTimeMillis()
        val results = testUsers.map { user ->
            async {
                try {
                    val response = createOrder(user.id!!, orderRequest)
                    "SUCCESS" to response
                } catch (e: Exception) {
                    "FAILED" to e.message
                }
            }
        }.awaitAll()
        val endTime = System.currentTimeMillis()

        // Then: 성능 및 정확성 검증
        val successCount = results.count { it.first == "SUCCESS" }
        val failedCount = results.count { it.first == "FAILED" }

        val requestSize = testUsers.size
        val elapsedMillis = endTime - startTime
        val elapsedSeconds = elapsedMillis.toDouble() / 1000.0
        val tps = requestSize / elapsedSeconds
        val averageElapsedMillis = ( elapsedMillis * 1.0 ) / (requestSize)

        println("🔍 대량 동시 주문 성능 테스트 결과:")
        println("   주문 시도: ${testUsers.size}건")
        println("   성공: ${successCount}건")
        println("   실패: ${failedCount}건")
        println("   총 처리 시간: ${elapsedMillis}ms")
        println("   평균 처리 시간: %.2fms".format(averageElapsedMillis))
        println("   TPS: %.2f".format(tps))

        // 재고 확인
        val updatedProduct = productRepository.findById(highStockProduct.id!!).get()

        // 검증: 모든 주문 성공, 정확한 재고 차감
        successCount shouldBe 15
        failedCount shouldBe 0
        updatedProduct.stock shouldBe (highStockProduct.stock - 15)

        println("   상품 재고 변화: ${highStockProduct.stock} → ${updatedProduct.stock}")
    }

    // === Helper Methods ===

    private fun createOrder(userId: Long, orderRequest: PostOrderRequestBody): org.springframework.http.ResponseEntity<PostOrderResponse> {
        val headers = HttpHeaders().apply {
            set("userId", userId.toString())
            set("Content-Type", "application/json")
        }

        val response = restTemplate.exchange(
            baseUrl,
            HttpMethod.POST,
            HttpEntity(orderRequest, headers),
            PostOrderResponse::class.java
        )

        // 성공한 경우만 상태 코드 확인
        if (response.statusCode == HttpStatus.CREATED) {
            response.body shouldNotBe null
        }

        return response
    }
}