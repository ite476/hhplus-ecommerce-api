package kr.hhplus.be.server.controller.v1.coupon

import com.ninjasquad.springmockk.MockkBean
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import kr.hhplus.be.server.controller.advise.GlobalExceptionHandler
import kr.hhplus.be.server.controller.v1.coupon.response.GetMyCouponsResponse
import kr.hhplus.be.server.controller.v1.coupon.response.PostCouponIssueResponse
import kr.hhplus.be.server.service.coupon.entity.UserCoupon
import kr.hhplus.be.server.service.coupon.entity.UserCouponStatus
import kr.hhplus.be.server.service.coupon.usecase.FindPagedUserCouponsUsecase
import kr.hhplus.be.server.service.coupon.usecase.IssueCouponUsecase
import kr.hhplus.be.server.service.exception.BusinessConflictException
import kr.hhplus.be.server.service.exception.BusinessUnacceptableException
import kr.hhplus.be.server.service.pagination.PagedList
import kr.hhplus.be.server.service.pagination.PagingOptions
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

@WebMvcTest(CouponController::class)
@Import(GlobalExceptionHandler::class)
@DisplayName("CouponController 테스트")
class CouponControllerTest {
    @MockkBean
    lateinit var findPagedUserCouponsUsecase: FindPagedUserCouponsUsecase
    
    @MockkBean
    lateinit var issueCouponUsecase: IssueCouponUsecase

    private val getMyCouponsEndpoint = "/api/v1/mycoupons"
    private val issueCouponEndpoint = "/api/v1/coupons"
    private val fixedTime = ZonedDateTime.of(2024, 1, 15, 10, 30, 0, 0, ZoneId.of("Asia/Seoul"))
    private val pagingOptions = PagingOptions(0, 10)

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
    @DisplayName("정상적인 쿠폰 목록 조회 요청 - 쿠폰 목록이 성공적으로 반환된다")
    fun getMyCoupons_ValidRequest_ReturnsSuccessResponse() {
        // Given
        val userId = 1L
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
        
        every { findPagedUserCouponsUsecase.findPagedUserCoupons(userId, any()) } returns PagedList(
            items = userCoupons,
            page = pagingOptions.page,
            size = pagingOptions.size,
            totalCount = 1
        )

        // When & Then
        val response = restClient.get()
            .uri {
                it.path(getMyCouponsEndpoint)
                    .queryParam("page", pagingOptions.page)
                    .queryParam("size", pagingOptions.size)
                    .build()
            }
            .header("userId", userId.toString())
            .retrieve()
            .toEntity(GetMyCouponsResponse::class.java)

        response.statusCode shouldBe HttpStatus.OK
        response.body.let { body ->
            body shouldNotBe null
            body?.run {
                coupons.size shouldBe 1
                coupons[0].userCouponId shouldBe 1L
                coupons[0].couponName shouldBe "신규가입쿠폰"
                coupons[0].discountAmount shouldBe 2000L
                coupons[0].isUsable shouldBe true
            }
        }

        coVerify(exactly = 1) {
            findPagedUserCouponsUsecase.findPagedUserCoupons(userId, any())
        }
    }

    @Test
    @DisplayName("쿠폰이 없는 사용자의 목록 조회 요청 - 빈 목록이 성공적으로 반환된다")
    fun getMyCoupons_UserWithoutCoupons_ReturnsEmptyList() {
        // Given
        val userId = 1L
        val pagingOptions = PagingOptions(0, 10)
        
        every { findPagedUserCouponsUsecase.findPagedUserCoupons(userId, any()) } returns PagedList(
            items = emptyList(),
            page = pagingOptions.page,
            size = pagingOptions.size,
            totalCount = 0
        )

        // When & Then
        val response = restClient.get()
            .uri {
                it.path(getMyCouponsEndpoint)
                    .queryParam("page", pagingOptions.page)
                    .queryParam("size", pagingOptions.size)
                    .build()
            }
            .header("userId", userId.toString())
            .retrieve()
            .toEntity(GetMyCouponsResponse::class.java)

        response.statusCode shouldBe HttpStatus.OK
        response.body.let { body ->
            body shouldNotBe null
            body?.run {
                coupons.isEmpty() shouldBe true
                totalCount shouldBe 0
            }
        }

        coVerify(exactly = 1) {
            findPagedUserCouponsUsecase.findPagedUserCoupons(userId, any())
        }
    }

    @Test
    @DisplayName("정상적인 쿠폰 발급 요청 - 쿠폰이 성공적으로 발급되고 201 Created를 반환한다")
    fun issueCoupon_ValidRequest_ReturnsSuccessResponse() {
        // Given
        val userId = 1L
        val couponId = 1L
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

        coEvery { issueCouponUsecase.issueCoupon(userId, couponId) } returns issuedCoupon

        // When & Then
        val response = restClient.post()
            .uri("$issueCouponEndpoint/$couponId")
            .header("userId", userId.toString())
            .retrieve()
            .toEntity(PostCouponIssueResponse::class.java)

        response.statusCode shouldBe HttpStatus.CREATED
        response.body.let { body ->
            body shouldNotBe null
            body?.run {
                couponId shouldBe 1L
                couponName shouldBe "신규가입쿠폰"
                discountAmount shouldBe 2000L
                issuedAt shouldNotBe null
                validUntil shouldNotBe null
            }
        }

        coVerify(exactly = 1) {
            issueCouponUsecase.issueCoupon(userId, couponId)
        }
    }

    @Test
    @DisplayName("중복 쿠폰 발급 요청 - 409 Conflict 오류를 반환한다")
    fun issueCoupon_DuplicateIssue_Returns409Error() {
        // Given
        val userId = 1L
        val couponId = 1L

        coEvery { issueCouponUsecase.issueCoupon(userId, couponId) } throws 
            BusinessConflictException("이미 발급받은 쿠폰입니다.")

        // When & Then
        try {
            restClient.post()
                .uri("$issueCouponEndpoint/$couponId")
                .header("userId", userId.toString())
                .retrieve()
                .toEntity(PostCouponIssueResponse::class.java)
        } catch (e: RestClientResponseException) {
            e.statusCode shouldBe HttpStatus.CONFLICT
            e.responseBodyAsString.contains("이미 발급받은 쿠폰입니다.") shouldBe true
        }
    }

    @Test
    @DisplayName("쿠폰 재고 부족으로 발급 실패 - 422 Unprocessable Entity 오류를 반환한다")
    fun issueCoupon_InsufficientStock_Returns422Error() {
        // Given
        val userId = 1L
        val couponId = 1L

        coEvery { issueCouponUsecase.issueCoupon(userId, couponId) } throws 
            BusinessUnacceptableException("쿠폰 재고가 부족합니다.")

        // When & Then
        try {
            restClient.post()
                .uri("$issueCouponEndpoint/$couponId")
                .header("userId", userId.toString())
                .retrieve()
                .toEntity(PostCouponIssueResponse::class.java)
        } catch (e: RestClientResponseException) {
            e.statusCode shouldBe HttpStatus.UNPROCESSABLE_ENTITY
            e.responseBodyAsString.contains("쿠폰 재고가 부족합니다.") shouldBe true
        }
    }

    @Test
    @DisplayName("userId 헤더가 누락된 쿠폰 목록 조회 - 400 Bad Request를 반환한다")
    fun getMyCoupons_MissingUserIdHeader_Returns400Error() {
        // When & Then
        try {
            restClient.get()
                .uri {
                    it.path(getMyCouponsEndpoint)
                        .queryParam("page", pagingOptions.page)
                        .queryParam("size", pagingOptions.size)
                        .build()
                }
                .retrieve()
                .toEntity(GetMyCouponsResponse::class.java)
        } catch (e: RestClientResponseException) {
            e.statusCode shouldBe HttpStatus.BAD_REQUEST
        }
    }

    @Test
    @DisplayName("userId 헤더가 누락된 쿠폰 발급 요청 - 400 Bad Request를 반환한다")
    fun issueCoupon_MissingUserIdHeader_Returns400Error() {
        // Given
        val couponId = 1L

        // When & Then
        try {
            restClient.post()
                .uri("$issueCouponEndpoint/$couponId")
                .retrieve()
                .toEntity(PostCouponIssueResponse::class.java)
        } catch (e: RestClientResponseException) {
            e.statusCode shouldBe HttpStatus.BAD_REQUEST
        }
    }

    @Test
    @DisplayName("시스템 오류로 쿠폰 목록 조회 실패 - 500 Internal Server Error를 반환한다")
    fun getMyCoupons_SystemError_Returns500Error() {
        // Given
        val userId = 1L

        every { findPagedUserCouponsUsecase.findPagedUserCoupons(userId, any()) } throws 
            RuntimeException("시스템 오류가 발생했습니다.")

        // When & Then
        try {
            restClient.get()
                .uri {
                    it.path(getMyCouponsEndpoint)
                        .queryParam("page", pagingOptions.page)
                        .queryParam("size", pagingOptions.size)
                        .build()
                }
                .header("userId", userId.toString())
                .retrieve()
                .toEntity(GetMyCouponsResponse::class.java)
        } catch (e: RestClientResponseException) {
            e.statusCode shouldBe HttpStatus.INTERNAL_SERVER_ERROR
        }
    }

    @Test
    @DisplayName("시스템 오류로 쿠폰 발급 실패 - 500 Internal Server Error를 반환한다")
    fun issueCoupon_SystemError_Returns500Error() {
        // Given
        val userId = 1L
        val couponId = 1L

        coEvery { issueCouponUsecase.issueCoupon(userId, couponId) } throws 
            RuntimeException("시스템 오류가 발생했습니다.")

        // When & Then
        try {
            restClient.post()
                .uri("$issueCouponEndpoint/$couponId")
                .header("userId", userId.toString())
                .retrieve()
                .toEntity(PostCouponIssueResponse::class.java)
        } catch (e: RestClientResponseException) {
            e.statusCode shouldBe HttpStatus.INTERNAL_SERVER_ERROR
        }
    }
}