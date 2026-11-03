SET NAMES utf8mb4;

-- 서버 C 전용 DB (technician_db). 컨테이너가 처음 만들어질 때 한 번만 실행됩니다.
-- 테이블 구조는 서버 C의 JPA 엔티티(Technician, TechnicianSkill, Assignment)에 맞췄습니다.
CREATE TABLE IF NOT EXISTS technician (
    technician_id   BIGINT       NOT NULL AUTO_INCREMENT,
    technician_name VARCHAR(255) NOT NULL,
    region          VARCHAR(255) NOT NULL,
    active          BIT(1)       NOT NULL,
    PRIMARY KEY (technician_id)
) DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS technician_skill (
    technician_id BIGINT       NOT NULL,
    product_type  VARCHAR(255) NOT NULL,
    PRIMARY KEY (technician_id, product_type)
) DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS assignment (
    assignment_id BIGINT       NOT NULL AUTO_INCREMENT,
    request_id    BIGINT       NOT NULL,
    technician_id BIGINT       NOT NULL,
    status        VARCHAR(255) NOT NULL,
    assigned_at   DATETIME(6)  NOT NULL,
    PRIMARY KEY (assignment_id),
    UNIQUE KEY uk_assignment_request (request_id)
) DEFAULT CHARSET=utf8mb4;

-- 더미 기사 (지역 값은 서버 A의 검증과 같은 SEOUL / BUSAN)
INSERT INTO technician (technician_id, technician_name, region, active) VALUES
    (1, '김민준', 'SEOUL', 1),
    (2, '이서연', 'SEOUL', 1),
    (3, '박도현', 'SEOUL', 1),
    (4, '정수빈', 'BUSAN', 1),
    (5, '최지훈', 'BUSAN', 1);

-- product_type 값은 서버 B의 제품 코드와 같아야 합니다.
INSERT INTO technician_skill (technician_id, product_type) VALUES
    (1,'AIR_PURIFIER'),(1,'AIR_CONDITIONER'),
    (2,'REFRIGERATOR'),(2,'MICROWAVE'),
    (3,'TV'),
    (4,'AIR_PURIFIER'),(4,'REFRIGERATOR'),(4,'TV'),
    (5,'AIR_CONDITIONER'),(5,'MICROWAVE');
