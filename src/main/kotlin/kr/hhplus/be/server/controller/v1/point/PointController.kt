package kr.hhplus.be.server.controller.v1.point

import io.swagger.v3.oas.annotations.tags.Tag
import kr.hhplus.be.server.controller.v1.point.request.PatchPointChargeRequestBody
import kr.hhplus.be.server.controller.v1.point.response.GetPointResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.net.URI

@RestController
@RequestMapping("/api/v1/point")
@Tag(name = "Point API", description = "포인트 API")
class PointController
    : PointApiSpec {

    @PatchMapping("charge")
    override fun chargePoint(
        @RequestHeader userId: String,
        @RequestBody body: PatchPointChargeRequestBody
    ) : ResponseEntity<Object> = ResponseEntity.created(URI.create("/point"))
        .build()

    @GetMapping("")
    override fun readPoint(
        @RequestHeader userId: String
    ) : ResponseEntity<GetPointResponse> = ResponseEntity.ok(GetPointResponse(
        userId = 1,
        point = 2_755_003_000,
    ))
}