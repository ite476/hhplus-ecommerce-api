package kr.hhplus.be.server.service.coupon.exception

import kr.hhplus.be.server.service.exception.ResourceNotFoundException

class UserCouponNotFoundException() : ResourceNotFoundException("발급된 쿠폰이 존재하지 않습니다.")
