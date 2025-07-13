# 📌 요구사항 개요 (Overview)

이 문서는 e-커머스 상품 주문 서비스의 주요 기능 및 요구사항을 요약합니다.  
도메인은 `User`, `Product`, `Order`, `Coupon` 으로 구분되며, 각각의 기능은 분리된 문서로 관리됩니다.

---

## ✅ 구현 대상 기능

| 번호 | 기능             | 설명                    | 도메인     |
|----|----------------|-----------------------|---------|
| 1  | 잔액 충전 / 조회 API | 사용자의 잔액 충전 및 조회       | User    |
| 2  | 상품 조회 API      | 상품 목록 및 재고 정보 조회      | Product |
| 3  | 주문 / 결제 API    | 상품 주문 및 결제 처리         | Order   |
| 4  | 선착순 쿠폰 API     | 쿠폰 발급, 조회 및 주문 시 사용   | Coupon  |
| 5  | 인기 상품 조회 API   | 최근 3일간 인기 판매 상품 5개 조회 | Product |

---

## 💡 개발 조건

- 각 기능은 **하나 이상의 단위 테스트** 작성 필수
- 재고 관리 문제 없이 동작해야 함
- **동시성 이슈** 고려 (락, 트랜잭션 등)
- **다수 인스턴스 환경**에서도 기능 무결성 보장
- 외부 시스템(데이터 플랫폼) 연동은 **Mock 또는 Fake**로 대체 가능

---

## 📁 요구사항 상세 문서

| 도메인     | 문서                                                   |
|---------|------------------------------------------------------|
| User    | [user-requirements.md](./user-requirements.md)       |
| Point   | [point-requirements.md](./point-requirements.md)     |
| Product | [product-requirements.md](./product-requirements.md) |
| Order   | [order-policy.md](./order-policy.md)                 |
| Coupon  | [coupon-policy.md](./coupon-policy.md)               |

---

## 🧪 예외 처리 및 공통 정책

- 응답 코드 정의, 유효성 검증 정책 등은 [📌 Main README](../README.md#-http-응답-설계-정책) 참고
