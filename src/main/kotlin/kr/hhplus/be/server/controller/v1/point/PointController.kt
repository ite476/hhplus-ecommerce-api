package kr.hhplus.be.server.controller.v1.point
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import kr.hhplus.be.server.controller.v1.point.request.PatchPointChargeRequestBody
import kr.hhplus.be.server.controller.v1.point.response.GetPointResponse
import kr.hhplus.be.server.service.point.usecase.ChargePointUsecase
import kr.hhplus.be.server.service.user.entity.User
import kr.hhplus.be.server.service.user.usecase.FindUserByIdUsecase
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.net.URI

@RestController
@RequestMapping("/api/v1/point")
@Tag(name = "Point API", description = "포인트 API")
class PointController(
    val chargePointUsecase: ChargePointUsecase,
    val findUserByIdUsecase: FindUserByIdUsecase
    ) : PointApiSpec {

    @PatchMapping("charge")
    override fun chargePoint(
        @RequestHeader userId: Long,
        @RequestBody @Valid body: PatchPointChargeRequestBody
    ) : ResponseEntity<Object> {
        chargePointUsecase.chargePoint(userId, body.amount)

        return ResponseEntity.created(URI.create("/point"))
            .build()
    }

    @PatchMapping("use")
    fun usePoint(
        @RequestHeader userId: Long,
        @RequestBody @Valid body: PatchPointChargeRequestBody
    ) : ResponseEntity<Object> {
        // 음수 금액이 들어오면 예외 발생 (PatchPointChargeRequestBody에서 이미 @Min(1L)로 막음)
        // 실제 차감 로직
        chargePointUsecase.let {
            if (it is kr.hhplus.be.server.service.point.usecase.UsePointUsecase) {
                it.usePoint(userId, body.amount)
            } else {
                throw IllegalStateException("UsePointUsecase not implemented")
            }
        }
        return ResponseEntity.created(URI.create("/point")).build()
    }

    @GetMapping("")
    override fun readPoint(
        @RequestHeader userId: Long
    ) : ResponseEntity<GetPointResponse> {
        val user: User = findUserByIdUsecase.findUserById(userId)
        val userId: Long = user.requiresId()

        val pointResponse = GetPointResponse(
            userId = userId,
            point = user.point
        )

        return ResponseEntity.ok(pointResponse)
    }
}