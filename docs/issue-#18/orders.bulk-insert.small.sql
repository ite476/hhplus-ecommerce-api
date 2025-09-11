-- =====================================================
-- 경량 주문/주문아이템 데이터 벌크 인서트 (로컬용)
-- Order: 20,000건 + OrderItem: ~50,000건 (대략 수백 MB 내)
-- 목적: 로컬 부하 테스트/개발 환경에서의 현실적인 샘플 데이터 제공
-- 주의: 대용량 스크립트(orders.bulk-insert.sql) 대비 리소스 사용량을 크게 축소
-- =====================================================

SET sql_mode = '';
SET foreign_key_checks = 0;
SET unique_checks = 0;
SET autocommit = 0;

START TRANSACTION;

-- =====================================================
-- 1단계: ORDER 테이블 벌크 인서트 (20,000건)
--  - 사용자 ID: 1~10,000에서 순환
--  - 쿠폰: 미사용(NULL)
--  - 주문 시간: 최근 30일 내 랜덤 분포
-- =====================================================
INSERT INTO hhplus.`order` (user_id, user_coupon_id, ordered_at, created_at, updated_at, `version`)
SELECT
    ((seq.n - 1) % 10000) + 1            AS user_id,
    NULL                                 AS user_coupon_id,
    DATE_SUB(NOW(), INTERVAL FLOOR(RAND() * 30) DAY)
      + INTERVAL FLOOR(RAND() * 86400) SECOND AS ordered_at,
    NOW(6)                               AS created_at,
    NOW(6)                               AS updated_at,
    0                                    AS `version`
FROM (
    -- 1..20000 시퀀스 생성 (10^4 기반 + LIMIT)
    SELECT @row := @row + 1 AS n
    FROM (
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
    (SELECT @row := 0) r
) seq
LIMIT 20000;

COMMIT;

-- =====================================================
-- 2단계: ORDER_ITEM 테이블 벌크 인서트 (~50,000건)
--  - 각 주문당 평균 ~2.5개 수준이 되도록 확률적 생성
--  - product_id: 상위 인기/롱테일 혼합 분포 단순화 버전
--  - unit_price: product 테이블 규칙과 동일(1000 + (product_id % 9000))
-- =====================================================
START TRANSACTION;

INSERT INTO hhplus.order_item (order_id, product_id, unit_price, quantity, created_at, updated_at, `version`)
SELECT
    o.id AS order_id,
    -- 상품 ID: 간단한 해싱으로 1..100,000 범위에 분포
    (
      ((o.id * 7919) + seq.n * 104729) % 100000
    ) + 1 AS product_id,
    1000 + (
      (
        ((o.id * 7919) + seq.n * 104729) % 100000
      ) % 9000
    ) AS unit_price,
    -- 수량 분포: 1개(60%), 2개(25%), 3개(10%), 4~6개(5%)
    CASE 
      WHEN (o.id + seq.n) % 100 < 60 THEN 1
      WHEN (o.id + seq.n) % 100 < 85 THEN 2
      WHEN (o.id + seq.n) % 100 < 95 THEN 3
      ELSE 4 + FLOOR(RAND() * 3)
    END AS quantity,
    NOW(6) AS created_at,
    NOW(6) AS updated_at,
    0      AS `version`
FROM (
  -- 방금 생성된 최근 20,000건 주문만 대상으로 아이템 생성
  SELECT id
  FROM hhplus.`order`
  WHERE id > (SELECT MAX(id) - 20000 FROM hhplus.`order`)
) o
CROSS JOIN (
  -- 각 주문당 시퀀스 1..3 (최대 3개 아이템 후보)
  SELECT 1 AS n UNION ALL SELECT 2 UNION ALL SELECT 3
) seq
WHERE 
  -- 평균 개수 제어: n=1은 항상, n=2는 80%, n=3은 40%
  (
    seq.n = 1 OR
    (seq.n = 2 AND (o.id % 10) < 8) OR 
    (seq.n = 3 AND (o.id % 10) < 4)
  );

COMMIT;

-- =====================================================
-- 최종 정리
-- =====================================================
SET sql_mode = DEFAULT;
SET foreign_key_checks = 1;
SET unique_checks = 1;
SET autocommit = 1;

-- 선택: 인덱스 통계 갱신 (환경에 따라 수행 시간 고려)
-- ANALYZE TABLE hhplus.`order`;
-- ANALYZE TABLE hhplus.order_item;


