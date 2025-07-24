package kr.hhplus.be.server.controller.v1.point

import com.fasterxml.jackson.databind.ObjectMapper
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import kr.hhplus.be.server.controller.common.advise.GlobalExceptionHandler
import kr.hhplus.be.server.controller.v1.point.request.PatchPointChargeRequestBody
import kr.hhplus.be.server.service.point.entity.PointChange
import kr.hhplus.be.server.service.point.entity.PointChangeType
import kr.hhplus.be.server.service.point.service.PointService
import kr.hhplus.be.server.service.user.entity.User
import kr.hhplus.be.server.service.user.service.UserService
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import java.time.ZoneId
import java.time.ZonedDateTime

class PointControllerTest : BehaviorSpec({

    val pointService = mockk<PointService>()
    val userService = mockk<UserService>()
    val objectMapper = ObjectMapper()
    val fixedTime = ZonedDateTime.of(2024, 1, 15, 10, 30, 0, 0, ZoneId.of("Asia/Seoul"))
    
    lateinit var mockMvc: MockMvc
    lateinit var apiResult: ResultActions

    beforeTest {
        val controller = PointController(pointService, userService)
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
            .setControllerAdvice(GlobalExceptionHandler())
            .build()
    }

    Given("PATCH /api/v1/point/charge 포인트 충전 요청이 들어올 때") {
        val endpoint = "/api/v1/point/charge"
        val userId = 1L
        val chargeAmount = 10000L
        val requestBody = PatchPointChargeRequestBody(chargeAmount)

        When("정상적인 충전 요청이면") {
            beforeTest {
                val pointChange = PointChange(1L, userId, chargeAmount, PointChangeType.Charge, fixedTime)
                every { pointService.chargePoint(userId, chargeAmount) } returns pointChange

                apiResult = mockMvc.perform(
                    patch(endpoint)
                        .header("userId", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody))
                )
            }

            Then("201 Created를 반환한다") {
                apiResult.andExpect(status().isCreated)
            }

            Then("Location 헤더에 /point를 포함한다") {
                apiResult.andExpect(header().string("Location", "/point"))
            }
        }
    }

    Given("GET /api/v1/point 포인트 조회 요청이 들어올 때") {
        val endpoint = "/api/v1/point"
        val userId = 1L

        When("포인트를 보유한 사용자를 조회하면") {
            val userPoint = 15000L
            val user = User(userId, "김철수", userPoint)

            beforeTest {
                every { userService.readSingleUser(userId) } returns user

                apiResult = mockMvc.perform(
                    get(endpoint).header("userId", userId)
                )
            }

            Then("200 OK를 반환한다") {
                apiResult.andExpect(status().isOk)
            }

            Then("JSON 응답을 반환한다") {
                apiResult.andExpect(content().contentType(MediaType.APPLICATION_JSON))
            }

            Then("사용자 ID와 포인트를 포함한다") {
                apiResult
                    .andExpect(jsonPath("$.userId").value(userId))
                    .andExpect(jsonPath("$.point").value(userPoint))
            }
        }

        When("포인트가 0인 사용자를 조회하면") {
            val zeroPointUserId = 3L
            val zeroPoint = 0L
            val zeroPointUser = User(zeroPointUserId, "박철수", zeroPoint)

            beforeTest {
                every { userService.readSingleUser(zeroPointUserId) } returns zeroPointUser

                apiResult = mockMvc.perform(
                    get(endpoint).header("userId", zeroPointUserId)
                )
            }

            Then("200 OK를 반환한다") {
                apiResult.andExpect(status().isOk)
            }

            Then("0 포인트를 반환한다") {
                apiResult.andExpect(jsonPath("$.point").value(0))
            }
        }
    }

    Given("API 응답 구조를 검증할 때") {
        When("포인트 조회 응답을 파싱하면") {
            val userId = 1L
            val userPoint = 25000L
            val user = User(userId, "김철수", userPoint)

            beforeTest {
                every { userService.readSingleUser(userId) } returns user
            }

            Then("응답 구조가 올바르다") {
                val result = mockMvc.perform(
                    get("/api/v1/point").header("userId", userId)
                )
                    .andExpect(status().isOk)
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.userId").exists())
                    .andExpect(jsonPath("$.point").exists())
                    .andReturn()

                val responseContent = result.response.contentAsString
                val response = objectMapper.readValue(responseContent, Map::class.java)
                
                response["userId"].toString().toLong() shouldBe userId
                response["point"].toString().toLong() shouldBe userPoint
            }
        }
    }
}) 