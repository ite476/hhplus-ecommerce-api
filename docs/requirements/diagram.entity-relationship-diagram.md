# 🗂️ Entity Relationship Diagram

- 전체 도메인에 대한 영속성 저장용 `ER 다이어그램`

# 📋 다이어그램



```mermaid
erDiagram
    user {
        UUID id PK "회원 ID, 자동 생성"
        String login_id  "회원 로그인 ID, 입력받은 문자열"
        OffsetDateTime joined_at "회원가입 시각"
    }
    
    user_point_balance {
        UUID user_id PK,FK "회원 ID"
        Long amount "보유 포인트 잔고"
    }
        
    product {
        UUID id PK "상품 ID, 자동 생성"
        String name "상품명"
        Long price "가격"
        Long stock "재고 수"
    }
    
    order {
        UUID id PK "주문 ID, 자동 생성"
        UUID user_id FK "회원 ID"
        Long total_price "총 결제 금액"
    }
    
    order_item {        
        UUID id PK "주문 항목 ID, 자동 생성"
        UUID order_id FK "주문 ID"
        UUID product_id FK "상품 ID"
        Long quantity "주문 수량"
    }

    order_adjustment {
        UUID id PK "주문 금액 변동 ID, 자동 생성"
        UUID order_id FK "주문 ID"
        UUID user_coupon_id FK "회원 쿠폰 ID"
    }
    
    coupon {
        UUID id PK "쿠폰 ID, 자동 생성"
        Long discount "할인 규모 (포인트)"
    }
    
    user_coupon {
        UUID id "회원 쿠폰 ID"
        UUID user_id FK "회원 ID"
        UUID coupon_id FK "쿠폰 ID"
        OffsetDateTime issued_at "쿠폰 발급 일시"
        OffsetDateTime used_at "쿠폰 사용 일시"
    }

    order_purchase_history {
        UUID id PK "결제 이력 ID, 자동 생성"
        
    }

    user ||--|| user_point_balance : "보유함"
    user ||--o{ user_coupon: "보유함"
    user ||--o{ order: "생성함"
    
    coupon ||--o{ user_coupon: "1:N"
    user_coupon ||--o| order_adjustment: "1:1"

    order ||--|{ order_item: "포함함"
    order_item ||--|| product: "1:1"
    order ||--o{ order_adjustment: "포함함"
    
    order ||--o{ order_purchase_history: "1:1"
    user ||--o{ order_purchase_history: "생성함"
```