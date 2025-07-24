package kr.hhplus.be.server.service.coupon.exception

import kr.hhplus.be.server.service.common.exception.BusinessUnacceptableException

class CouponNotAvailableException(): BusinessUnacceptableException("쿠폰을 사용할 수 없습니다.")