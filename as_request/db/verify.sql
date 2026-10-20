-- schema.sql, seed.sql 실행 후의 조회용 검증 쿼리입니다.
SET NAMES utf8mb4;
USE as_db;

-- 예상: seed_count=20, region_count=17, status_count=6
SELECT COUNT(*) AS seed_count,
       COUNT(DISTINCT region) AS region_count,
       COUNT(DISTINCT status) AS status_count
FROM AS_REQUEST
WHERE request_no LIKE 'AS-TEST-%';

-- 예상: RECEIVED 4 / WAITING_ASSIGNMENT 4 / ASSIGNED 4 /
--       IN_PROGRESS 3 / COMPLETED 3 / CANCELLED 2
SELECT status, COUNT(*) AS request_count
FROM AS_REQUEST
WHERE request_no LIKE 'AS-TEST-%'
GROUP BY status
ORDER BY status;

-- 예상: 0행. 배정 전에는 기사 ID가 없고, 배정·진행·완료에는 기사 ID가 있습니다.
-- 취소 건은 배정 전/후 사례를 모두 포함합니다.
SELECT request_no, status, technician_id
FROM AS_REQUEST
WHERE request_no LIKE 'AS-TEST-%'
  AND ((status IN ('RECEIVED', 'WAITING_ASSIGNMENT') AND technician_id IS NOT NULL)
    OR (status IN ('ASSIGNED', 'IN_PROGRESS', 'COMPLETED') AND technician_id IS NULL));

-- 최신순 접수 목록 예시
SELECT request_id, request_no, customer_name, product_id, region,
       symptom, technician_id, status, created_at
FROM AS_REQUEST
WHERE request_no LIKE 'AS-TEST-%'
ORDER BY created_at DESC, request_id DESC;
