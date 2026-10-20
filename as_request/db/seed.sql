-- 개발/테스트 전용 가상 데이터 20건. 실제 개인정보가 아닙니다.
-- schema.sql을 먼저 실행합니다. 같은 AS-TEST 접수번호가 있으면 기존 행을 유지합니다.
-- 제품 101~106, 기사 701~706은 다른 서버와 맞춰야 하는 예시 참조 ID입니다.
SET NAMES utf8mb4;
SET SESSION time_zone = '+09:00';
USE as_db;
SET @as_request_seed_today = CURRENT_DATE();

START TRANSACTION;
INSERT INTO AS_REQUEST (
    request_no, customer_name, phone, product_id, region,
    symptom, technician_id, status, created_at
) VALUES
    ('AS-TEST-0001', '테스트고객01', '01000000001', 101, '서울특별시', '전원이 켜지지 않습니다.', NULL, 'RECEIVED', TIMESTAMP(DATE_SUB(@as_request_seed_today, INTERVAL 5 DAY), '10:00:00')),
    ('AS-TEST-0002', '테스트고객02', '01000000002', 102, '부산광역시', '정수기에서 물이 새고 있습니다.', NULL, 'WAITING_ASSIGNMENT', TIMESTAMP(DATE_SUB(@as_request_seed_today, INTERVAL 6 DAY), '11:00:00')),
    ('AS-TEST-0003', '테스트고객03', '01000000003', 103, '대구광역시', '필터 교체 후에도 알림이 계속 켜집니다.', 701, 'ASSIGNED', TIMESTAMP(DATE_SUB(@as_request_seed_today, INTERVAL 7 DAY), '12:00:00')),
    ('AS-TEST-0004', '테스트고객04', '01000000004', 104, '인천광역시', '작동 중 큰 소음이 납니다.', 702, 'IN_PROGRESS', TIMESTAMP(DATE_SUB(@as_request_seed_today, INTERVAL 8 DAY), '13:00:00')),
    ('AS-TEST-0005', '테스트고객05', '01000000005', 105, '광주광역시', '냉방이 되지 않아 점검을 요청했습니다.', 703, 'COMPLETED', TIMESTAMP(DATE_SUB(@as_request_seed_today, INTERVAL 4 DAY), '14:00:00')),
    ('AS-TEST-0006', '테스트고객06', '01000000006', 106, '대전광역시', '방문 일정을 변경하기 위해 접수를 취소했습니다.', NULL, 'CANCELLED', TIMESTAMP(DATE_SUB(@as_request_seed_today, INTERVAL 5 DAY), '15:00:00')),
    ('AS-TEST-0007', '테스트고객07', '01000000007', 101, '울산광역시', NULL, NULL, 'RECEIVED', TIMESTAMP(DATE_SUB(@as_request_seed_today, INTERVAL 6 DAY), '16:00:00')),
    ('AS-TEST-0008', '테스트고객08', '01000000008', 102, '세종특별자치시', '표시창에 오류 코드가 나타납니다.', NULL, 'WAITING_ASSIGNMENT', TIMESTAMP(DATE_SUB(@as_request_seed_today, INTERVAL 7 DAY), '09:00:00')),
    ('AS-TEST-0009', '테스트고객09', '01000000009', 103, '경기도', '세척 도중 작동이 멈춥니다.', 704, 'ASSIGNED', TIMESTAMP(DATE_SUB(@as_request_seed_today, INTERVAL 8 DAY), '10:00:00')),
    ('AS-TEST-0010', '테스트고객10', '01000000010', 104, '강원특별자치도', '전원을 켜면 차단기가 내려갑니다.', 705, 'IN_PROGRESS', TIMESTAMP(DATE_SUB(@as_request_seed_today, INTERVAL 4 DAY), '11:00:00')),
    ('AS-TEST-0011', '테스트고객11', '01000000011', 105, '충청북도', '온수가 나오지 않는 증상을 수리했습니다.', 706, 'COMPLETED', TIMESTAMP(DATE_SUB(@as_request_seed_today, INTERVAL 5 DAY), '12:00:00')),
    ('AS-TEST-0012', '테스트고객12', '01000000012', 106, '충청남도', '기사 배정 후 고객 요청으로 취소했습니다.', 701, 'CANCELLED', TIMESTAMP(DATE_SUB(@as_request_seed_today, INTERVAL 6 DAY), '13:00:00')),
    ('AS-TEST-0013', '테스트고객13', '01000000013', 101, '전북특별자치도', '제품 하단에서 물이 떨어집니다.', NULL, 'RECEIVED', TIMESTAMP(DATE_SUB(@as_request_seed_today, INTERVAL 7 DAY), '14:00:00')),
    ('AS-TEST-0014', '테스트고객14', '01000000014', 102, '전라남도', '버튼을 눌러도 반응이 없습니다.', NULL, 'WAITING_ASSIGNMENT', TIMESTAMP(DATE_SUB(@as_request_seed_today, INTERVAL 8 DAY), '15:00:00')),
    ('AS-TEST-0015', '테스트고객15', '01000000015', 103, '경상북도', '탈수 중 진동이 심합니다.', 702, 'ASSIGNED', TIMESTAMP(DATE_SUB(@as_request_seed_today, INTERVAL 4 DAY), '16:00:00')),
    ('AS-TEST-0016', '테스트고객16', '01000000016', 104, '경상남도', '냉장실 온도가 내려가지 않습니다.', 703, 'IN_PROGRESS', TIMESTAMP(DATE_SUB(@as_request_seed_today, INTERVAL 5 DAY), '09:00:00')),
    ('AS-TEST-0017', '테스트고객17', '01000000017', 105, '제주특별자치도', '점화가 되지 않는 증상을 수리했습니다.', 704, 'COMPLETED', TIMESTAMP(DATE_SUB(@as_request_seed_today, INTERVAL 6 DAY), '10:00:00')),
    ('AS-TEST-0018', '테스트고객18', '01000000018', 106, '서울특별시', '방문 전 연락 부탁드립니다.
제품 작동 중 소음이 납니다.', NULL, 'RECEIVED', TIMESTAMP(DATE_SUB(@as_request_seed_today, INTERVAL 7 DAY), '11:00:00')),
    ('AS-TEST-0019', '테스트고객19', '01000000019', 101, '경기도', '제품 작동 중 타는 냄새가 납니다.', NULL, 'WAITING_ASSIGNMENT', TIMESTAMP(DATE_SUB(@as_request_seed_today, INTERVAL 8 DAY), '12:00:00')),
    ('AS-TEST-0020', '테스트고객20', '01000000020', 102, '부산광역시', '도어가 제대로 닫히지 않습니다.', 705, 'ASSIGNED', TIMESTAMP(DATE_SUB(@as_request_seed_today, INTERVAL 4 DAY), '13:00:00'))
ON DUPLICATE KEY UPDATE request_no = AS_REQUEST.request_no;
COMMIT;
