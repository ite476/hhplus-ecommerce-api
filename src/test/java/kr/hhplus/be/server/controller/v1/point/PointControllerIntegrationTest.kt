package kr.hhplus.be.server.controller.v1.point

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import kr.hhplus.be.server.TestcontainersConfiguration
import kr.hhplus.be.server.config.jpa.JpaConfig
import kr.hhplus.be.server.controller.v1.point.request.PatchPointChargeRequestBody
import kr.hhplus.be.server.controller.v1.point.response.GetPointResponse
import kr.hhplus.be.server.repository.jpa.entity.user.UserEntity
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
import java.util.concurrent.CountDownLatch

/**
 * 포인트 동시성 통합 테스트
 * 
 * 이 테스트는 실제 DB와 전체 애플리케이션 컨텍스트를 사용하여
 * 포인트 충전의 동시성 문제를 검증합니다.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@Import(TestcontainersConfiguration::class, JpaConfig::class)
@ActiveProfiles("test")
@DisplayName("포인트 동시성 통합 테스트")
class PointControllerIntegrationTest {

    @LocalServerPort
    private var port: Int = 0

    @Autowired
    private lateinit var restTemplate: TestRestTemplate

    @Autowired
    private lateinit var userRepository: UserJpaRepository

    private lateinit var testUser: UserEntity
    private val baseUrl get() = "http://localhost:$port/api/v1/point"

    @BeforeEach
    fun setUp() {
        // 테스트용 사용자 생성
        testUser = userRepository.save(
            UserEntity(
                name = "동시성테스트사용자",
                point = 5000L
            )
        )
    }

    @Test
    @DisplayName("동시 충전 테스트 - 1000포인트씩 10번 동시 충전 → 10000포인트 정확 확인")
    fun `concurrent point charge should be accurate`() = runBlocking {
        // Given
        val initialPoint = getCurrentPoint()
        val chargeAmount = 1000L
        val concurrentRequests = 10

        // When
        val startTime = System.currentTimeMillis()
        val results = (1..concurrentRequests).map {
            async { chargePoint(chargeAmount) }
        }.awaitAll()
        val endTime = System.currentTimeMillis()

        // Then
        val finalPoint = getCurrentPoint()
        val expectedPoint = initialPoint + (chargeAmount * concurrentRequests)
        val successCount = results.count { it.statusCode == HttpStatus.CREATED }
        val failedCount = results.size - successCount

        val elapsedMillis = endTime - startTime
        val elapsedSeconds = elapsedMillis.toDouble() / 1000.0
        val tps = concurrentRequests / elapsedSeconds
        val averageElapsedMillis = ( elapsedMillis * 1.0 ) / (concurrentRequests)

        println("🔍 동시 충전 테스트 결과:")
        println("   충전 시도: $concurrentRequests 건")
        println("   성공: $successCount 건, 실패: $failedCount 건")
        println("   초기 포인트: $initialPoint → 최종 포인트: $finalPoint (예상: $expectedPoint)")

        println("   총 처리 시간: ${elapsedMillis}ms")
        println("   평균 처리 시간: %.2fms".format(averageElapsedMillis))
        println("   TPS: %.2f".format(tps))

        finalPoint shouldBe expectedPoint
        successCount shouldBe concurrentRequests
        Unit
    }

    @Test
    @DisplayName("동시 사용 테스트 - 5000포인트 보유, 200포인트씩 50번 동시 사용, 25건 성공 25건 실패, 최종 포인트 0")
    fun `concurrent point usage should be limited by balance`() = runBlocking {
        // Given
        val initialPoint = getCurrentPoint()
        val useAmount = 200L
        val latch = CountDownLatch(1)
        val concurrentRequests = 50

        // 보유 포인트 5000, 50번 사용 시 25건 성공, 25건 실패 예상
        // When
        val startTime = System.currentTimeMillis()
        val jobs = (1..concurrentRequests).map {
            async {
                latch.await()
                usePoint(useAmount)
            }
        }
        latch.countDown()
        val results = jobs.awaitAll()
        val endTime = System.currentTimeMillis()

        // Then
        val finalPoint = getCurrentPoint()
        val successCount = results.count { it.statusCode == HttpStatus.CREATED }
        val failedCount = results.size - successCount

        val elapsedMillis = endTime - startTime
        val elapsedSeconds = elapsedMillis.toDouble() / 1000.0
        val tps = concurrentRequests / elapsedSeconds
        val averageElapsedMillis = (elapsedMillis * 1.0) / concurrentRequests

        println("🔍 동시 사용 테스트 결과:")
        println("   사용 시도: $concurrentRequests 건")
        println("   성공: $successCount 건, 실패: $failedCount 건")
        println("   초기 포인트: $initialPoint → 최종 포인트: $finalPoint (예상: 0)")
        println("   총 처리 시간: ${elapsedMillis}ms")
        println("   평균 처리 시간: %.2fms".format(averageElapsedMillis))
        println("   TPS: %.2f".format(tps))

        finalPoint shouldBe 0L
        successCount shouldBe 25
        failedCount shouldBe 25
        Unit
    }

    @Test
    @DisplayName("혼합 연산 테스트 - 충전 5번(+500씩) + 사용 3번(-300씩) 동시 실행, 최종 포인트 = 초기값 + 1600")
    fun `concurrent charge and usage should be consistent`() = runBlocking {
        // Given
        val initialPoint = getCurrentPoint()
        val chargeAmount = 500L
        val useAmount = 300L
        val chargeRequests = 5
        val useRequests = 3

        // When
        val startTime = System.currentTimeMillis()
        val jobs = (1..chargeRequests).map {
            async { chargePoint(chargeAmount) }
        } + (1..useRequests).map {
            async { usePoint(useAmount) }
        }
        val results = jobs.awaitAll()
        val endTime = System.currentTimeMillis()

        // Then
        val finalPoint = getCurrentPoint()
        val expectedPoint = initialPoint + (chargeAmount * chargeRequests) - (useAmount * useRequests)
        val successCount = results.count { it.statusCode == HttpStatus.CREATED }
        val failedCount = results.size - successCount

        val elapsedMillis = endTime - startTime
        val elapsedSeconds = elapsedMillis.toDouble() / 1000.0
        val tps = (chargeRequests + useRequests) / elapsedSeconds
        val averageElapsedMillis = ( elapsedMillis * 1.0 ) / (chargeRequests + useRequests)

        println("🔍 혼합 연산 테스트 결과:")
        println("   충전 시도: $chargeRequests 건, 사용 시도: $useRequests 건")
        println("   성공: $successCount 건, 실패: $failedCount 건")
        println("   초기 포인트: $initialPoint → 최종 포인트: $finalPoint (예상: $expectedPoint)")
        println("   총 처리 시간: ${elapsedMillis}ms")
        println("   평균 처리 시간: %.2fms".format(averageElapsedMillis))
        println("   TPS: %.2f".format(tps))

        finalPoint shouldBe expectedPoint
        successCount shouldBe (chargeRequests + useRequests)
        Unit
    }

    // === Helper Methods ===

    private fun getCurrentPoint(): Long {
        val headers = HttpHeaders().apply {
            set("userId", testUser.id.toString())
        }
        val response = restTemplate.exchange(
            baseUrl,
            HttpMethod.GET,
            HttpEntity<Any>(headers),
            GetPointResponse::class.java
        )
        response.statusCode shouldBe HttpStatus.OK
        response.body shouldNotBe null
        return response.body!!.point
    }

    private fun chargePoint(amount: Long): org.springframework.http.ResponseEntity<Any> {
        val headers = HttpHeaders().apply {
            set("userId", testUser.id.toString())
            set("Content-Type", "application/json")
        }
        val requestBody = PatchPointChargeRequestBody(amount = amount)
        return restTemplate.exchange(
            "$baseUrl/charge",
            HttpMethod.PATCH,
            HttpEntity(requestBody, headers),
            Any::class.java
        )
    }

    private fun usePoint(amount: Long): org.springframework.http.ResponseEntity<Any> {
        val headers = HttpHeaders().apply {
            set("userId", testUser.id.toString())
            set("Content-Type", "application/json")
        }
        val requestBody = PatchPointChargeRequestBody(amount = amount)
        return restTemplate.exchange(
            "$baseUrl/use",
            HttpMethod.PATCH,
            HttpEntity(requestBody, headers),
            Any::class.java
        )
    }

    // 실패한 응답을 나타내는 데이터 클래스
    data class FailedResponse(val error: String)
}