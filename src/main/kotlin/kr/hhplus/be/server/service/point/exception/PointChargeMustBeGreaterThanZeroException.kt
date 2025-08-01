package kr.hhplus.be.server.service.point.exception

import kr.hhplus.be.server.service.exception.BusinessUnacceptableException

class PointChargeMustBeGreaterThanZeroException : BusinessUnacceptableException("충전 포인트는 0보다 커야 합니다."){

}
