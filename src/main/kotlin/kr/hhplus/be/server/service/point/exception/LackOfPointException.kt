package kr.hhplus.be.server.service.point.exception

import kr.hhplus.be.server.service.exception.BusinessUnacceptableException

class LackOfPointException() : BusinessUnacceptableException("포인트 잔액이 부족합니다.")