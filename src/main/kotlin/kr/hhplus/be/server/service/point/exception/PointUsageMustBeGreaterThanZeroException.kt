package kr.hhplus.be.server.service.point.exception

import kr.hhplus.be.server.service.exception.BusinessUnacceptableException

class PointUsageMustBeGreaterThanZeroException : BusinessUnacceptableException("사용 포인트는 0보다 커야 합니다.") {
}