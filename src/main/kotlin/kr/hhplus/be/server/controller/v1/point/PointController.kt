package kr.hhplus.be.server.controller.v1.point

import io.swagger.v3.oas.annotations.tags.Tag
import kr.hhplus.be.server.controller.v1.point.request.PatchPointChargeRequestBody
import kr.hhplus.be.server.controller.v1.point.response.GetPointResponse
import kr.hhplus.be.server.service.point.service.PointService
import kr.hhplus.be.server.service.user.service.UserService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.net.URI

@RestController
@RequestMapping("/api/v1/point")
@Tag(name = "Point API", description = "포인트 API")
class PointController(
    val pointService: PointService,
    val userService: UserService
    ) : PointApiSpec {

    @PatchMapping("charge")
    override fun chargePoint(
        @RequestHeader userId: Long,
        @RequestBody body: PatchPointChargeRequestBody
    ) : ResponseEntity<Object> {
        pointService.chargePoint(userId, body.amount)

        return ResponseEntity.created(URI.create("/point"))
            .build()
    }

    @GetMapping("")
    override fun readPoint(
        @RequestHeader userId: Long
    ) : ResponseEntity<GetPointResponse> {
        val userPoint = userService.readSingleUser(userId)

        val pointResponse = GetPointResponse(
            userPoint.id,
            userPoint.point
        )

        return ResponseEntity.ok(pointResponse)
    }
}