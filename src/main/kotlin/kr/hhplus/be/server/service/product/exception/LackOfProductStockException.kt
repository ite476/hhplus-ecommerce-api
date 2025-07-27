package kr.hhplus.be.server.service.product.exception

import kr.hhplus.be.server.service.exception.BusinessUnacceptableException

class LackOfProductStockException : BusinessUnacceptableException("상품 재고가 부족합니다.") {

}
