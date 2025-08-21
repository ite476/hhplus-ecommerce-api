package kr.hhplus.be.server.service.coupon.exception

import kr.hhplus.be.server.service.exception.BusinessConflictException
import kr.hhplus.be.server.service.exception.BusinessUnacceptableException

class CouponAlreadyIssuedException : BusinessConflictException("이미 발급받은 쿠폰입니다.")
class CouponOutOfStockException : BusinessUnacceptableException("쿠폰 재고가 소진되었습니다.")
class CouponNotReadyException : BusinessUnacceptableException("쿠폰 준비 중입니다. 잠시 후 다시 시도해주세요.")


