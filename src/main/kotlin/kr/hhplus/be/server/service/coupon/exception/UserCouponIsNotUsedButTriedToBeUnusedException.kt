package kr.hhplus.be.server.service.coupon.exception

import kr.hhplus.be.server.service.common.exception.BusinessConflictException

class UserCouponIsNotUsedButTriedToBeUnusedException : BusinessConflictException("사용 처리된 쿠폰만 사용 취소할 수 있습니다."){

}
