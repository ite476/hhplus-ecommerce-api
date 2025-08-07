package kr.hhplus.be.server.service.coupon.exception

import kr.hhplus.be.server.service.exception.ResourceNotFoundException

class CouponNotFoundException () : ResourceNotFoundException("쿠폰이 존재하지 않습니다.")
