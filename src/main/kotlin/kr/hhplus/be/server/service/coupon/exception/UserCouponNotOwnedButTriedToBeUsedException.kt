package kr.hhplus.be.server.service.coupon.exception

import kr.hhplus.be.server.service.exception.BusinessUnacceptableException

class UserCouponNotOwnedButTriedToBeUsedException : BusinessUnacceptableException("사용할 수 없는 쿠폰입니다."){

}
