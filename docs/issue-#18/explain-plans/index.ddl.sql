-- =====================================================
-- 인기 상품 조회 쿼리 최적화를 위한 인덱스 DDL
-- 대상: popular.summary.sql 성능 개선
-- =====================================================

-- =====================================================
-- 1. order_item 테이블 최적화 인덱스
-- =====================================================

-- 기존 인덱스 확인 및 제거 (필요 시)
-- SHOW INDEX FROM order_item;
-- DROP INDEX IF EXISTS idx_order_item_optimized ON order_item;

-- 복합 인덱스: 논리삭제 + 날짜범위 + 그룹핑 최적화
CREATE INDEX idx_order_item_date_product 
ON order_item (created_at, product_id, deleted_at);

-- 인덱스 설계 근거:
-- 1. created_at (1순위): BETWEEN 조건 범위 스캔 최적화  
-- 2. product_id (2순위): GROUP BY 연산 최적화 + 커버링 인덱스 효과
-- 3. deleted_at (3순위): NULL 값 집중으로 필터링 효과 극대화

-- =====================================================
-- 2. product 테이블 조인 최적화 인덱스  
-- =====================================================

-- 기존 인덱스 확인 및 제거 (필요 시)
-- SHOW INDEX FROM product;
-- DROP INDEX IF EXISTS idx_product_join_optimized ON product;

-- 복합 인덱스: 조인 + 논리삭제 최적화
CREATE INDEX idx_product_join_optimized 
ON product (deleted_at, id);

-- 인덱스 설계 근거:
-- 1. id (1순위): PK 조인 연산 최적화
-- 2. deleted_at (2순위): 조인 시 논리삭제 필터링 우선 처리

-- =====================================================
-- 3. 추가 최적화 인덱스 (선택적 적용)
-- =====================================================

-- [선택사항] quantity 집계 최적화용 커버링 인덱스
-- 메모리 사용량이 증가하지만 I/O를 최소화할 수 있음
-- CREATE INDEX idx_order_item_covering 
-- ON order_item (deleted_at, created_at, product_id, quantity);

-- =====================================================
-- 4. 인덱스 적용 후 확인 쿼리
-- =====================================================

-- 인덱스 생성 확인
SHOW INDEX FROM order_item WHERE Key_name = 'idx_order_item_optimized';
SHOW INDEX FROM product WHERE Key_name = 'idx_product_join_optimized';

-- 실행 계획 확인용 쿼리 (실제 파라미터 값 대입 필요)
-- EXPLAIN FORMAT=JSON 
-- SELECT ... (popular.summary.sql 내용 복붙 후 파라미터 치환)

-- 인덱스 사용량 모니터링 쿼리
-- SELECT 
--     TABLE_NAME,
--     INDEX_NAME, 
--     CARDINALITY,
--     NON_UNIQUE
-- FROM information_schema.STATISTICS 
-- WHERE TABLE_SCHEMA = 'hhplus' 
--   AND INDEX_NAME IN ('idx_order_item_optimized', 'idx_product_join_optimized');
