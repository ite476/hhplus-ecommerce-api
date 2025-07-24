package kr.hhplus.be.server.controller.v1.coupon

import com.fasterxml.jackson.databind.ObjectMapper
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk

import kr.hhplus.be.server.controller.common.advise.GlobalExceptionHandler
import kr.hhplus.be.server.service.coupon.entity.UserCoupon
import kr.hhplus.be.server.service.coupon.entity.UserCouponStatus
import kr.hhplus.be.server.service.coupon.service.CouponService
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody
import java.time.ZoneId
import java.time.ZonedDateTime

class CouponControllerTest : BehaviorSpec({

    val couponService = mockk<CouponService>()
    val objectMapper = ObjectMapper()
    val fixedTime = ZonedDateTime.of(2024, 1, 15, 10, 30, 0, 0, ZoneId.of("Asia/Seoul"))
    
    lateinit var webTestClient: WebTestClient

    beforeTest {
        val controller = CouponController(couponService)
        webTestClient = WebTestClient.bindToController(controller)
            .controllerAdvice(GlobalExceptionHandler())
            .build()
    }

    Given("GET /api/v1/mycoupons 쿠폰 목록 조회 요청이 들어올 때") {
        val endpoint = "/api/v1/mycoupons"
        val userId = 1L

        When("사용자에게 발급된 쿠폰이 있으면") {
            beforeTest {
                val userCoupons = listOf(
                    UserCoupon(
                        id = 1L,
                        userId = userId,
                        couponId = 1L,
                        couponName = "신규가입쿠폰",
                        discount = 2000L,
                        status = UserCouponStatus.ACTIVE,
                        issuedAt = fixedTime,
                        usedAt = null,
                        validUntil = fixedTime.plusDays(30)
                    )
                )
                every { couponService.readUserCoupons(userId) } returns userCoupons
            }

            Then("200 OK와 쿠폰 정보를 반환한다") {
                webTestClient.get()
                    .uri(endpoint)
                    .header("userId", userId.toString())
                    .exchange()
                    .expectStatus().isOk
                    .expectHeader().contentType(MediaType.APPLICATION_JSON)
                    .expectBody()
                    .jsonPath("$.coupons").isArray
                    .jsonPath("$.coupons[0].userCouponId").isEqualTo(1)
                    .jsonPath("$.coupons[0].couponName").isEqualTo("신규가입쿠폰")
                    .jsonPath("$.coupons[0].discountAmount").isEqualTo(2000)
                    .jsonPath("$.coupons[0].isUsable").isEqualTo(true)
            }
        }

        When("사용자에게 발급된 쿠폰이 하나도 없으면") {
            beforeTest {
                every { couponService.readUserCoupons(userId) } returns emptyList()
            }

            Then("200 OK와 빈 리스트를 반환한다") {
                webTestClient.get()
                    .uri(endpoint)
                    .header("userId", userId.toString())
                    .exchange()
                    .expectStatus().isOk
                    .expectHeader().contentType(MediaType.APPLICATION_JSON)
                    .expectBody()
                    .jsonPath("$.coupons").isArray
                    .jsonPath("$.coupons").isEmpty
            }
        }
    }

    Given("POST /api/v1/coupon/{couponId} 쿠폰 발급 요청이 들어올 때") {
        val userId = 1L
        val couponId = 1L
        val endpoint = "/api/v1/coupon/$couponId"

        When("정상적인 쿠폰 발급 요청이면") {
            beforeTest {
                val issuedCoupon = UserCoupon(
                    id = 1L,
                    userId = userId,
                    couponId = couponId,
                    couponName = "신규가입쿠폰",
                    discount = 2000L,
                    status = UserCouponStatus.ACTIVE,
                    issuedAt = fixedTime,
                    usedAt = null,
                    validUntil = fixedTime.plusDays(30)
                )
                coEvery { couponService.issueCoupon(userId, couponId) } returns issuedCoupon
            }

            Then("200 OK와 발급된 쿠폰 정보를 반환한다") {
                webTestClient.post()
                    .uri(endpoint)
                    .header("userId", userId.toString())
                    .exchange()
                    .expectStatus().isOk
                    .expectHeader().contentType(MediaType.APPLICATION_JSON)
                    .expectBody()
                    .jsonPath("$.couponId").isEqualTo(1)
                    .jsonPath("$.couponName").isEqualTo("신규가입쿠폰")
                    .jsonPath("$.discountAmount").isEqualTo(2000)
                    .jsonPath("$.issuedAt").exists()
                    .jsonPath("$.validUntil").exists()
            }
        }

        When("중복 발급으로 실패하면") {
            beforeTest {
                coEvery { couponService.issueCoupon(userId, couponId) } throws 
                    kr.hhplus.be.server.service.common.exception.BusinessConflictException("이미 발급받은 쿠폰입니다.")
            }

            Then("409 Conflict를 반환한다") {
                webTestClient.post()
                    .uri(endpoint)
                    .header("userId", userId.toString())
                    .exchange()
                    .expectStatus().isEqualTo(409)
            }
        }

        When("쿠폰 재고 부족으로 실패하면") {
            beforeTest {
                coEvery { couponService.issueCoupon(userId, couponId) } throws 
                    kr.hhplus.be.server.service.common.exception.BusinessUnacceptableException("쿠폰 재고가 부족합니다.")
            }

            Then("422 Unprocessable Entity를 반환한다") {
                webTestClient.post()
                    .uri(endpoint)
                    .header("userId", userId.toString())
                    .exchange()
                    .expectStatus().isEqualTo(422)
            }
        }
    }

    Given("API 응답 구조를 검증할 때") {
        When("쿠폰 목록 응답을 파싱하면") {
            val userId = 1L
            
            beforeTest {
                val userCoupons = listOf(
                    UserCoupon(
                        id = 1L,
                        userId = userId,
                        couponId = 1L,
                        couponName = "테스트쿠폰",
                        discount = 1000L,
                        status = UserCouponStatus.ACTIVE,
                        issuedAt = fixedTime,
                        usedAt = null,
                        validUntil = fixedTime.plusDays(7)
                    )
                )
                every { couponService.readUserCoupons(userId) } returns userCoupons
            }

            Then("응답 구조가 올바르다") {
                val responseBody = webTestClient.get()
                    .uri("/api/v1/mycoupons")
                    .header("userId", userId.toString())
                    .exchange()
                    .expectStatus().isOk
                    .expectHeader().contentType(MediaType.APPLICATION_JSON)
                    .expectBody<String>()
                    .returnResult()
                    .responseBody

                responseBody?.let { content ->
                    println("파싱 테스트 응답: $content")
                    val response = objectMapper.readValue(content, Map::class.java)
                    response.containsKey("coupons") shouldBe true
                }
            }
        }
    }
})