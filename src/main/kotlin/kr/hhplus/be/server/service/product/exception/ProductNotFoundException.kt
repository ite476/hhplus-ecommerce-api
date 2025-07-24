package kr.hhplus.be.server.service.product.exception

import kr.hhplus.be.server.service.common.exception.ResourceNotFoundException

class ProductNotFoundException() : ResourceNotFoundException("상품이 존재하지 않습니다.")
