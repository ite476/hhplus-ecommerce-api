-- =====================================================
-- 중간 규모 주문 데이터 벌크 인서트 (성능 측정용)
-- Order: 200만 건 + OrderItem: 500만 건 (약 3-4GB)
-- =====================================================

SET sql_mode = '';
SET foreign_key_checks = 0;
SET unique_checks = 0;
SET autocommit = 0;

START TRANSACTION;

-- =====================================================
-- 1단계: ORDER 테이블 벌크 인서트 (200만 건)
-- =====================================================
INSERT INTO hhplus.`order` (user_id, user_coupon_id, ordered_at, created_at, updated_at)
SELECT
    -- 사용자 ID: 1~10,000 사용자 순환 사용
    ((t.n - 1) % 10000) + 1 AS user_id,
    
    -- 쿠폰 사용 안 함 (FK 제약 회피)
    NULL AS user_coupon_id,
    
    -- 주문 시간: 최근 90일 분산 (최근일로 갈수록 주문량 증가)
    DATE_SUB(NOW(), INTERVAL 
        FLOOR(
            -- Pareto 분포 시뮬레이션: 최근 30일에 70% 집중
            CASE 
                WHEN (t.n % 100) < 70 THEN RAND() * 30
                ELSE 30 + (RAND() * 60)
            END
        ) DAY
    ) + INTERVAL FLOOR(RAND() * 86400) SECOND AS ordered_at,
    
    NOW(6) AS created_at,
    NOW(6) AS updated_at
FROM (
    SELECT @row := @row + 1 AS n
    FROM (
        -- 4단계 CROSS JOIN으로 200만 시퀀스 생성
        SELECT 0 UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
        UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9
    ) a,  -- 10
    (
        SELECT 0 UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
        UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9
    ) b,  -- 100
    (
        SELECT 0 UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
        UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9
    ) c,  -- 1,000
    (
        SELECT 0 UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
        UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9
    ) d,  -- 10,000
    (
        SELECT 0 UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
        UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9
    ) e,  -- 100,000
    (
        SELECT 0 UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
        UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9
    ) f   -- 1,000,000
    CROSS JOIN (SELECT @row := 0) r
) t
LIMIT 2000000;

COMMIT;

-- =====================================================
-- 2단계: ORDER_ITEM 테이블 벌크 인서트 (500만 건)
-- =====================================================
START TRANSACTION;

INSERT INTO hhplus.order_item (order_id, product_id, unit_price, quantity, created_at, updated_at)
SELECT
    -- Order ID: 방금 생성된 주문들의 ID 범위 사용
    -- 각 주문당 평균 2.5개 아이템 (1~5개 랜덤)
    order_sequence.order_id,
    
    -- 상품 ID: Pareto 분포 (상위 20% 상품이 80% 판매)
    CASE 
        WHEN (item_gen.n % 100) < 80 THEN 
            -- 80% 확률로 인기 상품 (1~20,000)
            ((item_gen.n * 7919) % 20000) + 1  -- 소수 곱셈으로 분산
        ELSE 
            -- 20% 확률로 나머지 상품 (20,001~100,000)
            ((item_gen.n * 7919) % 80000) + 20001
    END AS product_id,
    
    -- 단가: 상품 테이블과 일치하도록 계산 (1000 + (product_id % 9000))
    1000 + (
        CASE 
            WHEN (item_gen.n % 100) < 80 THEN 
                ((item_gen.n * 7919) % 20000) + 1
            ELSE 
                ((item_gen.n * 7919) % 80000) + 20001
        END % 9000
    ) AS unit_price,
    
    -- 수량: 1~10개 (평균 2.8개, 소량 주문 집중)
    CASE 
        WHEN (item_gen.n % 100) < 60 THEN 1  -- 60% 확률로 1개
        WHEN (item_gen.n % 100) < 85 THEN 2  -- 25% 확률로 2개
        WHEN (item_gen.n % 100) < 95 THEN 3  -- 10% 확률로 3개
        ELSE FLOOR(4 + (RAND() * 7))         -- 5% 확률로 4~10개
    END AS quantity,
    
    NOW(6) AS created_at,
    NOW(6) AS updated_at

FROM (
    -- 각 주문당 아이템 생성을 위한 서브쿼리
    SELECT 
        o.id as order_id,
        o.ordered_at
    FROM hhplus.`order` o 
    WHERE o.id > (SELECT MAX(id) - 2000000 FROM hhplus.`order`)  -- 최근 생성된 주문들만
) order_sequence
CROSS JOIN (
    -- 각 주문당 평균 2.5개 아이템 생성을 위한 시퀀스
    SELECT @item_row := @item_row + 1 AS n
    FROM (
        SELECT 0 UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
    ) item_a,
    (
        SELECT 0 UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
        UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9
    ) item_b
    CROSS JOIN (SELECT @item_row := 0) item_r
    LIMIT 3  -- 주문당 최대 3개 아이템 (일부는 조건부 제외되어 평균 2.5개)
) item_gen
WHERE 
    -- 아이템 생성 조건: 확률적으로 일부 제외
    (
        (item_gen.n = 1) OR  -- 첫 번째 아이템은 항상 생성
        (item_gen.n = 2 AND (order_sequence.order_id % 100) < 80) OR  -- 80% 확률로 두 번째
        (item_gen.n = 3 AND (order_sequence.order_id % 100) < 40)     -- 40% 확률로 세 번째
    );

COMMIT;

-- =====================================================
-- 최종 정리 및 최적화
-- =====================================================
SET sql_mode = DEFAULT;
SET foreign_key_checks = 1;
SET unique_checks = 1;
SET autocommit = 1;

-- 인덱스 통계 갱신 (성능 최적화)
ANALYZE TABLE hhplus.`order`;
ANALYZE TABLE hhplus.order_item;

-- 결과 확인
SELECT 
    'Order' as table_name,
    COUNT(*) as total_count,
    MIN(ordered_at) as earliest_order,
    MAX(ordered_at) as latest_order
FROM hhplus.`order`
WHERE id > (SELECT MAX(id) - 2000000 FROM hhplus.`order`)

UNION ALL

SELECT 
    'OrderItem' as table_name,
    COUNT(*) as total_count,
    MIN(created_at) as earliest_created,
    MAX(created_at) as latest_created
FROM hhplus.order_item oi
JOIN hhplus.`order` o ON oi.order_id = o.id
WHERE o.id > (SELECT MAX(id) - 2000000 FROM hhplus.`order`);
