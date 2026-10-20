DROP TABLE IF EXISTS as_request;
CREATE TABLE as_request (
    request_id BIGINT PRIMARY KEY,
    request_no VARCHAR(30) NOT NULL,
    customer_name VARCHAR(50) NOT NULL,
    phone VARCHAR(20) NOT NULL,
    product_id BIGINT NOT NULL,
    region VARCHAR(50) NOT NULL,
    symptom TEXT,
    technician_id BIGINT,
    status VARCHAR(30) NOT NULL,
    created_at DATETIME NOT NULL
);
INSERT INTO as_request VALUES
    (1, 'AS-TEST-0001', '첫접수', '01000000001', 101, '서울특별시', '소음이 납니다.', NULL, 'RECEIVED', '2026-10-19 10:00:00'),
    (2, 'AS-TEST-0002', '배정접수', '01000000002', 102, '부산광역시', '전원이 켜지지 않습니다.', 701, 'ASSIGNED', '2026-10-20 10:00:00'),
    (3, 'AS-TEST-0003', '최근접수', '01000000003', 103, '경기도', NULL, NULL, 'WAITING_ASSIGNMENT', '2026-10-20 10:00:00');
