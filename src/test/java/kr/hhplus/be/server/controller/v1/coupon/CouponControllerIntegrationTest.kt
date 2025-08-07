package kr.hhplus.be.server.controller.v1.coupon

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import kr.hhplus.be.server.TestcontainersConfiguration
import kr.hhplus.be.server.config.jpa.JpaConfig
import kr.hhplus.be.server.controller.v1.coupon.response.PostCouponIssueResponse
import kr.hhplus.be.server.repository.jpa.entity.coupon.CouponEntity
import kr.hhplus.be.server.repository.jpa.entity.user.UserEntity
import kr.hhplus.be.server.repository.jpa.repository.coupon.CouponJpaRepository
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
import java.time.ZonedDateTime

/**
 * 쿠폰 발급 동시성 통합 테스트
 * 
 * 이 테스트는 실제 DB와 전체 애플리케이션 컨텍스트를 사용하여
 * 쿠폰 발급의 동시성 문제를 검증합니다.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@Import(TestcontainersConfiguration::class, JpaConfig::class)
@ActiveProfiles("test")
@DisplayName("쿠폰 발급 동시성 통합 테스트")
class CouponControllerIntegrationTest {

    @LocalServerPort
    private var port: Int = 0

    @Autowired
    private lateinit var restTemplate: TestRestTemplate

    @Autowired
    private lateinit var userRepository: UserJpaRepository

    @Autowired
    private lateinit var couponRepository: CouponJpaRepository

    private lateinit var testUsers: List<UserEntity>
    private lateinit var limitedCoupon: CouponEntity
    private val baseUrl get() = "http://localhost:$port/api/v1/coupons"

    @BeforeEach
    fun setUp() {
        // 테스트용 사용자들 생성 (동시성 테스트를 위해 여러 명)
        testUsers = (1..15).map { i ->
            userRepository.save(
                UserEntity(
                    name = "동시성테스트사용자$i",
                    point = 10000L
                )
            )
        }

        // 한정 수량 쿠폰 생성 (총 5개만 발급 가능)
        limitedCoupon = couponRepository.save(
            CouponEntity(
                name = "한정 할인쿠폰",
                discount = 5000L,
                totalQuantity = 5,
                issuedQuantity = 0,
                expiredAt = ZonedDateTime.now().plusDays(30)
            )
        )
    }

    @Test
    @DisplayName("한정 쿠폰 동시 발급 테스트 - 10명이 동시 발급 시도 시 5명만 성공해야 한다")
    fun `limited coupon concurrent issue should maintain quantity constraint`() = runBlocking {
        // Given: 5개 한정 쿠폰과 10명의 사용자
        val couponId = limitedCoupon.id!!
        val concurrentUsers = testUsers.take(10)
        
        // When: 10명이 동시에 쿠폰 발급 시도
        val startTime = System.currentTimeMillis()
        val results = concurrentUsers.map { user ->
            async {
                try {
                    val response = issueCoupon(user.id!!, couponId)
                    "SUCCESS" to response
                } catch (e: Exception) {
                    "FAILED" to e.message
                }
            }
        }.awaitAll()
        val endTime = System.currentTimeMillis()

        // Then: 정확히 5명만 성공해야 함
        val successCount = results.count { it.first == "SUCCESS" }
        val failedCount = results.count { it.first == "FAILED" }

        val elapsedMillis = endTime - startTime
        val elapsedSeconds = elapsedMillis.toDouble() / 1000.0
        val tps = 10 / elapsedSeconds
        val averageElapsedMillis = ( elapsedMillis * 1.0 ) / (10)

        println("🔍 한정 쿠폰 동시 발급 테스트 결과:")
        println("   발급 시도: ${concurrentUsers.size}명")
        println("   성공: ${successCount}명")
        println("   실패: ${failedCount}명")
        println("   총 처리 시간: ${elapsedMillis}ms")
        println("   평균 처리 시간: %.2fms".format(averageElapsedMillis))
        println("   TPS: %.2f".format(tps))
        
        // 쿠폰 발급 후 상태 확인
        val updatedCoupon = couponRepository.findById(couponId).get()

        // 검증: 성공은 5건, 실패는 5건이어야 함
        successCount shouldBe 5
        failedCount shouldBe 5
        updatedCoupon.issuedQuantity shouldBe 5

        println("   최종 발급 수량: ${updatedCoupon.issuedQuantity}/${updatedCoupon.totalQuantity}")
    }

    @Test
    @DisplayName("동일 사용자 중복 발급 방지 테스트 - 1명이 동시 발급 시도 시 1번만 성공해야 한다")
    fun `same user multiple concurrent requests should issue only once`() = runBlocking {
        // Given: 1명의 사용자가 동일 쿠폰을 10번 동시 발급 시도
        val user = testUsers.first()
        val couponId = limitedCoupon.id!!
        val concurrentRequests = 10

        // When: 동일 사용자가 10번 동시에 쿠폰 발급 시도
        val startTime = System.currentTimeMillis()
        val results = (1..concurrentRequests).map {
            async {
                try {
                    val response = issueCoupon(user.id!!, couponId)
                    "SUCCESS" to response
                } catch (e: Exception) {
                    "FAILED" to e.message
                }
            }
        }.awaitAll()
        val endTime = System.currentTimeMillis()

        // Then: 1번만 성공해야 함
        val successCount = results.count { it.first == "SUCCESS" }
        val failedCount = results.count { it.first == "FAILED" }

        val elapsedMillis = endTime - startTime
        val elapsedSeconds = elapsedMillis.toDouble() / 1000.0
        val tps = 10 / elapsedSeconds
        val averageElapsedMillis = ( elapsedMillis * 1.0 ) / (10)

        println("🔍 동일 사용자 중복 발급 방지 테스트 결과:")
        println("   발급 시도: ${concurrentRequests}번")
        println("   성공: ${successCount}번")
        println("   실패: ${failedCount}번")
        println("   총 처리 시간: ${elapsedMillis}ms")
        println("   평균 처리 시간: %.2fms".format(averageElapsedMillis))
        println("   TPS: %.2f".format(tps))
        
        // 쿠폰 발급 후 상태 확인
        val updatedCoupon = couponRepository.findById(couponId).get()

        // 검증: 1번만 성공해야 함
        successCount shouldBe 1
        failedCount shouldBe 9
        updatedCoupon.issuedQuantity shouldBe 1

        println("   최종 발급 수량: ${updatedCoupon.issuedQuantity}/${updatedCoupon.totalQuantity}")
    }

    @Test
    @DisplayName("쿠폰 초과 발급 시나리오 테스트 - 이미 소진된 쿠폰에 대한 동시 발급은 모두 실패해야 한다")
    fun `sold out coupon concurrent requests should all fail`() = runBlocking {
        // Given: 이미 소진된 쿠폰 (수동으로 발급 수량을 총 수량과 같게 설정)
        limitedCoupon.issuedQuantity = limitedCoupon.totalQuantity
        couponRepository.save(limitedCoupon)
        
        val couponId = limitedCoupon.id!!
        val concurrentUsers = testUsers.take(5)

        // When: 5명이 이미 소진된 쿠폰 발급 시도
        val startTime = System.currentTimeMillis()
        val results = concurrentUsers.map { user ->
            async {
                try {
                    val response = issueCoupon(user.id!!, couponId)
                    "SUCCESS" to response
                } catch (e: Exception) {
                    "FAILED" to e.message
                }
            }
        }.awaitAll()
        val endTime = System.currentTimeMillis()

        // Then: 모두 실패해야 함
        val successCount = results.count { it.first == "SUCCESS" }
        val failedCount = results.count { it.first == "FAILED" }

        val elapsedMillis = endTime - startTime
        val elapsedSeconds = elapsedMillis.toDouble() / 1000.0
        val tps = 5 / elapsedSeconds
        val averageElapsedMillis = ( elapsedMillis * 1.0 ) / (10)

        println("🔍 소진된 쿠폰 발급 시도 테스트 결과:")
        println("   발급 시도: ${concurrentUsers.size}명")
        println("   성공: ${successCount}명")
        println("   실패: ${failedCount}명")
        println("   총 처리 시간: ${elapsedMillis}ms")
        println("   평균 처리 시간: %.2fms".format(averageElapsedMillis))
        println("   TPS: %.2f".format(tps))
        
        // 쿠폰 발급 후 상태 확인
        val updatedCoupon = couponRepository.findById(couponId).get()

        // 검증: 모두 실패해야 함
        successCount shouldBe 0
        failedCount shouldBe 5
        updatedCoupon.issuedQuantity shouldBe 5 // 원래 값 유지

        println("   최종 발급 수량: ${updatedCoupon.issuedQuantity}/${updatedCoupon.totalQuantity}")
    }

    @Test
    @DisplayName("대량 동시 쿠폰 발급 테스트 - 많은 수의 동시 요청에서도 정확한 수량 제한이 유지되어야 한다")
    fun `high concurrency coupon issue should maintain quantity constraint`() = runBlocking {
        // Given: 10개 한정 쿠폰 생성
        val largeCoupon = couponRepository.save(
            CouponEntity(
                name = "대량 테스트 쿠폰",
                discount = 3000L,
                totalQuantity = 10,
                issuedQuantity = 0,
                expiredAt = ZonedDateTime.now().plusDays(30)
            )
        )
        
        val couponId = largeCoupon.id!!
        val concurrentUsers = testUsers // 15명 모두

        // When: 15명이 동시에 10개 한정 쿠폰 발급 시도
        val startTime = System.currentTimeMillis()
        val results = concurrentUsers.map { user ->
            async {
                try {
                    val response = issueCoupon(user.id!!, couponId)
                    "SUCCESS" to response
                } catch (e: Exception) {
                    "FAILED" to e.message
                }
            }
        }.awaitAll()
        val endTime = System.currentTimeMillis()

        // Then: 정확히 10명만 성공해야 함
        val successCount = results.count { it.first == "SUCCESS" }
        val failedCount = results.count { it.first == "FAILED" }

        val elapsedMillis = endTime - startTime
        val elapsedSeconds = elapsedMillis.toDouble() / 1000.0
        val tps = 15 / elapsedSeconds
        val averageElapsedMillis = ( elapsedMillis * 1.0 ) / (10)

        println("🔍 대량 동시성 쿠폰 발급 테스트 결과:")
        println("   발급 시도: ${concurrentUsers.size}명")
        println("   성공: ${successCount}명")
        println("   실패: ${failedCount}명")
        println("   총 처리 시간: ${elapsedMillis}ms")
        println("   평균 처리 시간: %.2fms".format(averageElapsedMillis))
        println("   TPS: %.2f".format(tps))
        
        // 쿠폰 발급 후 상태 확인
        val updatedCoupon = couponRepository.findById(couponId).get()

        // 검증: 10명 성공, 5명 실패
        successCount shouldBe 10
        failedCount shouldBe 5
        updatedCoupon.issuedQuantity shouldBe 10

        println("   최종 발급 수량: ${updatedCoupon.issuedQuantity}/${updatedCoupon.totalQuantity}")
    }

    // === Helper Methods ===

    private fun issueCoupon(userId: Long, couponId: Long): org.springframework.http.ResponseEntity<PostCouponIssueResponse> {
        val headers = HttpHeaders().apply {
            set("userId", userId.toString())
            set("Content-Type", "application/json")
        }

        val response = restTemplate.exchange(
            "$baseUrl/$couponId",
            HttpMethod.POST,
            HttpEntity<Any>(headers),
            PostCouponIssueResponse::class.java
        )

        // 성공한 경우만 상태 코드 확인
        if (response.statusCode == HttpStatus.CREATED) {
            response.body shouldNotBe null
        }

        return response
    }
}