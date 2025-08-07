INSERT INTO hhplus.coupon
(
    name,
    issued_quantity,
    total_quantity,
    discount,
    created_at,
    expired_at,
    updated_at
)
SELECT
    CONCAT('테스트쿠폰_', t.n) AS name,
    0 AS issued_quantity,
    1000 + (t.n * 1000) AS total_quantity,  -- 1000, 2000, ..., 10000
    500 + (t.n * 100) AS discount,          -- 500, 600, ..., 1400 (샘플)
    NOW(6) AS created_at,
    DATE_ADD(NOW(6), INTERVAL 30 DAY) AS expired_at,  -- 30일 뒤 만료
    NOW(6) AS updated_at
FROM (
    SELECT 1 AS n UNION ALL
    SELECT 2 UNION ALL
    SELECT 3 UNION ALL
    SELECT 4 UNION ALL
    SELECT 5 UNION ALL
    SELECT 6 UNION ALL
    SELECT 7 UNION ALL
    SELECT 8 UNION ALL
    SELECT 9 UNION ALL
    SELECT 10
) t;
