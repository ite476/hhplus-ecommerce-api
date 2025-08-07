package kr.hhplus.be.server.controller.v1.point

import com.ninjasquad.springmockk.MockkBean
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.clearAllMocks
import io.mockk.every
import kr.hhplus.be.server.controller.advise.GlobalExceptionHandler
import kr.hhplus.be.server.controller.v1.point.request.PatchPointChargeRequestBody
import kr.hhplus.be.server.controller.v1.point.response.GetPointResponse
import kr.hhplus.be.server.service.point.entity.PointChange
import kr.hhplus.be.server.service.point.entity.PointChangeType
import kr.hhplus.be.server.service.point.exception.PointChargeMustBeGreaterThanZeroException
import kr.hhplus.be.server.service.point.service.PointService
import kr.hhplus.be.server.service.user.entity.User
import kr.hhplus.be.server.service.user.exception.UserNotFoundException
import kr.hhplus.be.server.service.user.service.UserService
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

@WebMvcTest(PointController::class)
@Import(GlobalExceptionHandler::class)
@DisplayName("PointController 테스트")
class PointControllerTest {
    @MockkBean
    lateinit var pointService: PointService
    
    @MockkBean
    lateinit var userService: UserService

    private val getPointEndpoint = "/api/v1/point"
    private val chargePointEndpoint = "/api/v1/point/charge"
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
    @DisplayName("정상적인 포인트 조회 요청 - 포인트가 성공적으로 반환된다")
    fun getPoint_ValidRequest_ReturnsSuccessResponse() {
        // Given
        val userId = 1L
        val userPoint = 15000L
        val user = User(userId, "김철수", userPoint)

        every { userService.findUserById(userId) } returns user

        // When & Then
        val response = restClient.get()
            .uri(getPointEndpoint)
            .header("userId", userId.toString())
            .retrieve()
            .toEntity(GetPointResponse::class.java)

        response.statusCode shouldBe HttpStatus.OK
        response.body.let { body ->
            body shouldNotBe null
            body?.run {
                userId shouldBe userId
                point shouldBe userPoint
            }
        }
    }

    @Test
    @DisplayName("포인트가 0인 사용자 조회 - 0 포인트가 성공적으로 반환된다")
    fun getPoint_ZeroPointUser_ReturnsZeroPoint() {
        // Given
        val userId = 3L
        val zeroPoint = 0L
        val user = User(userId, "박철수", zeroPoint)

        every { userService.findUserById(userId) } returns user

        // When & Then
        val response = restClient.get()
            .uri(getPointEndpoint)
            .header("userId", userId.toString())
            .retrieve()
            .toEntity(GetPointResponse::class.java)

        response.statusCode shouldBe HttpStatus.OK
        response.body.let { body ->
            body shouldNotBe null
            body?.run {
                point shouldBe 0L
            }
        }
    }

    @Test
    @DisplayName("정상적인 포인트 충전 요청 - 포인트가 성공적으로 충전되고 201 Created를 반환한다")
    fun chargePoint_ValidRequest_ReturnsSuccessResponse() {
        // Given
        val userId = 1L
        val chargeAmount = 10000L
        val requestBody = PatchPointChargeRequestBody(chargeAmount)
        val pointChange = PointChange(1L, userId, chargeAmount, PointChangeType.Charge, fixedTime)

        every { pointService.chargePoint(userId, chargeAmount) } returns pointChange

        // When & Then
        val response = restClient.patch()
            .uri(chargePointEndpoint)
            .header("userId", userId.toString())
            .body(requestBody)
            .retrieve()
            .toEntity(String::class.java)

        response.statusCode shouldBe HttpStatus.CREATED
        response.headers.location?.toString() shouldBe "/point"
    }

    @Test
    @DisplayName("존재하지 않는 사용자의 포인트 조회 - 404 Not Found 오류를 반환한다")
    fun getPoint_UserNotFound_Returns404Error() {
        // Given
        val userId = 999L

        every { userService.findUserById(userId) } throws UserNotFoundException()

        // When & Then
        try {
            restClient.get()
                .uri(getPointEndpoint)
                .header("userId", userId.toString())
                .retrieve()
                .toEntity(GetPointResponse::class.java)
        } catch (e: RestClientResponseException) {
            e.statusCode shouldBe HttpStatus.NOT_FOUND
            e.responseBodyAsString.contains("회원이 존재하지 않습니다.") shouldBe true
        }
    }

    @Test
    @DisplayName("0 이하의 포인트 충전 요청 - 400 Bad Request 오류를 반환한다")
    fun chargePoint_InvalidAmount_Returns400Error() {
        // Given
        val userId = 1L
        val invalidAmount = 0L
        val requestBody = PatchPointChargeRequestBody(invalidAmount)

        every { pointService.chargePoint(userId, invalidAmount) } throws 
            PointChargeMustBeGreaterThanZeroException()

        // When & Then
        try {
            restClient.patch()
                .uri(chargePointEndpoint)
                .header("userId", userId.toString())
                .body(requestBody)
                .retrieve()
                .toEntity(String::class.java)
        } catch (e: RestClientResponseException) {
            e.statusCode shouldBe HttpStatus.BAD_REQUEST
        }
    }

    @Test
    @DisplayName("userId 헤더가 누락된 포인트 조회 - 400 Bad Request를 반환한다")
    fun getPoint_MissingUserIdHeader_Returns400Error() {
        // When & Then
        try {
            restClient.get()
                .uri(getPointEndpoint)
                .retrieve()
                .toEntity(GetPointResponse::class.java)
        } catch (e: RestClientResponseException) {
            e.statusCode shouldBe HttpStatus.BAD_REQUEST
        }
    }

    @Test
    @DisplayName("userId 헤더가 누락된 포인트 충전 - 400 Bad Request를 반환한다")
    fun chargePoint_MissingUserIdHeader_Returns400Error() {
        // Given
        val requestBody = PatchPointChargeRequestBody(10000L)

        // When & Then
        try {
            restClient.patch()
                .uri(chargePointEndpoint)
                .body(requestBody)
                .retrieve()
                .toEntity(String::class.java)
        } catch (e: RestClientResponseException) {
            e.statusCode shouldBe HttpStatus.BAD_REQUEST
        }
    }

    @Test
    @DisplayName("시스템 오류로 포인트 조회 실패 - 500 Internal Server Error를 반환한다")
    fun getPoint_SystemError_Returns500Error() {
        // Given
        val userId = 1L

        every { userService.findUserById(userId) } throws 
            RuntimeException("시스템 오류가 발생했습니다.")

        // When & Then
        try {
            restClient.get()
                .uri(getPointEndpoint)
                .header("userId", userId.toString())
                .retrieve()
                .toEntity(GetPointResponse::class.java)
        } catch (e: RestClientResponseException) {
            e.statusCode shouldBe HttpStatus.INTERNAL_SERVER_ERROR
        }
    }

    @Test
    @DisplayName("시스템 오류로 포인트 충전 실패 - 500 Internal Server Error를 반환한다")
    fun chargePoint_SystemError_Returns500Error() {
        // Given
        val userId = 1L
        val chargeAmount = 10000L
        val requestBody = PatchPointChargeRequestBody(chargeAmount)

        every { pointService.chargePoint(userId, chargeAmount) } throws 
            RuntimeException("시스템 오류가 발생했습니다.")

        // When & Then
        try {
            restClient.patch()
                .uri(chargePointEndpoint)
                .header("userId", userId.toString())
                .body(requestBody)
                .retrieve()
                .toEntity(String::class.java)
        } catch (e: RestClientResponseException) {
            e.statusCode shouldBe HttpStatus.INTERNAL_SERVER_ERROR
        }
    }
}