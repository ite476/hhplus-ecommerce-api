package kr.hhplus.be.server.repository.jpa.enums

/**
 * 상품 변경 유형을 나타내는 열거형
 */
enum class ProductChangeType(val description: String) {
    PRICE_CHANGE("가격 변경"),
    STOCK_CHANGE("재고 변경"),
    NAME_CHANGE("상품명 변경"),
    CREATED("상품 생성"),
    DELETED("상품 삭제")
} 