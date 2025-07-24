package kr.hhplus.be.server.controller.v1.product

import com.fasterxml.jackson.databind.ObjectMapper
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import kr.hhplus.be.server.controller.common.advise.GlobalExceptionHandler
import kr.hhplus.be.server.service.product.entity.Product
import kr.hhplus.be.server.service.product.entity.ProductSaleSummary
import kr.hhplus.be.server.service.product.service.ProductService
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import java.time.ZoneId
import java.time.ZonedDateTime

class ProductControllerTest : BehaviorSpec({

    val productService = mockk<ProductService>()
    val objectMapper = ObjectMapper()
    val fixedTime = ZonedDateTime.of(2024, 1, 15, 10, 30, 0, 0, ZoneId.of("Asia/Seoul"))
    
    lateinit var mockMvc: MockMvc
    lateinit var apiResult: ResultActions

    beforeTest {
        val controller = ProductController(productService)
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
            .setControllerAdvice(GlobalExceptionHandler())
            .build()
    }

    Given("GET /api/v1/products 상품 목록 조회 요청이 들어올 때") {
        val endpoint = "/api/v1/products"

        When("상품이 여러 개 있으면") {
            beforeTest {
                val products = listOf(
                    Product(1L, "아메리카노", 4500L, 100, fixedTime),
                    Product(2L, "라떼", 5000L, 50, fixedTime),
                    Product(3L, "카푸치노", 5500L, 30, fixedTime)
                )
                every { productService.readProducts() } returns products

                apiResult = mockMvc.perform(get(endpoint))
            }

            Then("200 OK를 반환한다") {
                apiResult.andExpect(status().isOk)
            }

            Then("JSON 응답을 반환한다") {
                apiResult.andExpect(content().contentType(MediaType.APPLICATION_JSON))
            }

            Then("모든 상품 정보를 포함한다") {
                apiResult
                    .andExpect(jsonPath("$.length()").value(3))
                    .andExpect(jsonPath("$[0].id").value(1))
                    .andExpect(jsonPath("$[0].name").value("아메리카노"))
                    .andExpect(jsonPath("$[0].price").value(4500))
                    .andExpect(jsonPath("$[0].stock").value(100))
                    .andExpect(jsonPath("$[1].id").value(2))
                    .andExpect(jsonPath("$[1].name").value("라떼"))
                    .andExpect(jsonPath("$[2].id").value(3))
                    .andExpect(jsonPath("$[2].name").value("카푸치노"))
            }
        }

        When("상품이 하나도 없으면") {
            beforeTest {
                every { productService.readProducts() } returns emptyList()

                apiResult = mockMvc.perform(get(endpoint))
            }

            Then("200 OK를 반환한다") {
                apiResult.andExpect(status().isOk)
            }

            Then("빈 배열을 응답한다") {
                apiResult
                    .andExpect(jsonPath("$.length()").value(0))
                    .andExpect(jsonPath("$").isArray)
            }
        }
    }

    Given("GET /api/v1/products/popular 인기 상품 조회 요청이 들어올 때") {
        val endpoint = "/api/v1/products/popular"

        When("인기 상품이 여러 개 있으면") {
            beforeTest {
                val popularProducts = listOf(
                    ProductSaleSummary(
                        Product(1L, "아메리카노", 4500L, 100, fixedTime),
                        1, 1200, fixedTime.minusDays(3), fixedTime
                    ),
                    ProductSaleSummary(
                        Product(2L, "라떼", 5000L, 50, fixedTime),
                        2, 800, fixedTime.minusDays(3), fixedTime
                    ),
                    ProductSaleSummary(
                        Product(3L, "카푸치노", 5500L, 30, fixedTime),
                        3, 600, fixedTime.minusDays(3), fixedTime
                    )
                )
                every { productService.readPopularProducts() } returns popularProducts

                apiResult = mockMvc.perform(get(endpoint))
            }

            Then("200 OK를 반환한다") {
                apiResult.andExpect(status().isOk)
            }

            Then("JSON 응답을 반환한다") {
                apiResult.andExpect(content().contentType(MediaType.APPLICATION_JSON))
            }

            Then("인기 상품 정보를 순위 별로 포함한다") {
                apiResult
                    .andExpect(jsonPath("$.length()").value(3))
                    .andExpect(jsonPath("$[0].id").value(1))
                    .andExpect(jsonPath("$[0].name").value("아메리카노"))
                    .andExpect(jsonPath("$[0].rank").value(1))
                    .andExpect(jsonPath("$[0].sold").value(1200))
                    .andExpect(jsonPath("$[1].rank").value(2))
                    .andExpect(jsonPath("$[1].sold").value(800))
                    .andExpect(jsonPath("$[2].rank").value(3))
                    .andExpect(jsonPath("$[2].sold").value(600))
            }
        }

        When("인기 상품이 하나도 없으면") {
            beforeTest {
                every { productService.readPopularProducts() } returns emptyList()

                apiResult = mockMvc.perform(get(endpoint))
            }

            Then("200 OK를 반환한다") {
                apiResult.andExpect(status().isOk)
            }

            Then("빈 배열을 응답한다") {
                apiResult
                    .andExpect(jsonPath("$.length()").value(0))
                    .andExpect(jsonPath("$").isArray)
            }
        }
    }

    Given("API 응답 구조를 검증할 때") {
        When("상품 목록 응답을 파싱하면") {
            beforeTest {
                val products = listOf(
                    Product(1L, "테스트상품", 10000L, 20, fixedTime)
                )
                every { productService.readProducts() } returns products
            }

            Then("응답 구조가 올바르다") {
                val result = mockMvc.perform(get("/api/v1/products"))
                    .andExpect(status().isOk)
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$[0].id").exists())
                    .andExpect(jsonPath("$[0].name").exists())
                    .andExpect(jsonPath("$[0].price").exists())
                    .andExpect(jsonPath("$[0].stock").exists())
                    .andReturn()

                val responseContent = result.response.contentAsString
                val response = objectMapper.readValue(responseContent, List::class.java)
                
                response.size shouldBe 1
                val product = response[0] as Map<*, *>
                product["id"].toString().toLong() shouldBe 1L
                product["name"] shouldBe "테스트상품"
                product["price"].toString().toLong() shouldBe 10000L
                product["stock"].toString().toInt() shouldBe 20
            }
        }

        When("인기 상품 응답을 파싱하면") {
            beforeTest {
                val popularProducts = listOf(
                    ProductSaleSummary(
                        Product(1L, "인기상품", 8000L, 15, fixedTime),
                        1, 500, fixedTime.minusDays(3), fixedTime
                    )
                )
                every { productService.readPopularProducts() } returns popularProducts
            }

            Then("응답 구조가 올바르다") {
                val result = mockMvc.perform(get("/api/v1/products/popular"))
                    .andExpect(status().isOk)
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$[0].id").exists())
                    .andExpect(jsonPath("$[0].name").exists())
                    .andExpect(jsonPath("$[0].price").exists())
                    .andExpect(jsonPath("$[0].stock").exists())
                    .andExpect(jsonPath("$[0].rank").exists())
                    .andExpect(jsonPath("$[0].sold").exists())
                    .andReturn()

                val responseContent = result.response.contentAsString
                val response = objectMapper.readValue(responseContent, List::class.java)
                
                response.size shouldBe 1
                val popularProduct = response[0] as Map<*, *>
                popularProduct["id"].toString().toLong() shouldBe 1L
                popularProduct["name"] shouldBe "인기상품"
                popularProduct["rank"].toString().toInt() shouldBe 1
                popularProduct["sold"].toString().toInt() shouldBe 500
            }
        }
    }
}) 