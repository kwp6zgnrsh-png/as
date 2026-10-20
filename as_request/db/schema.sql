-- MySQL 8.0.16+ / 한국 시간 기준 AS 접수 DB
-- 기존 테이블을 삭제하지 않습니다. 기존 AS_REQUEST 구조를 변경하는 마이그레이션은 아닙니다.
SET NAMES utf8mb4;
SET SESSION time_zone = '+09:00';

CREATE DATABASE IF NOT EXISTS as_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_0900_ai_ci;

USE as_db;

CREATE TABLE IF NOT EXISTS AS_REQUEST (
    request_id     BIGINT NOT NULL AUTO_INCREMENT COMMENT '접수 ID',
    request_no     VARCHAR(30) NOT NULL COMMENT '외부 표시용 접수번호; 접수 서버에서 생성',
    customer_name  VARCHAR(50) NOT NULL COMMENT '신청자명',
    phone          VARCHAR(20) NOT NULL COMMENT '하이픈 없는 연락처',
    product_id     BIGINT NOT NULL COMMENT '제품 서버의 제품 참조 ID',
    region         VARCHAR(50) NOT NULL COMMENT '방문 지역; 시·도',
    symptom        TEXT NULL COMMENT '고장 증상 및 메모사항',
    technician_id  BIGINT NULL COMMENT '기사 서버의 배정 기사 참조 ID; 배정 전 NULL',
    status         VARCHAR(30) NOT NULL DEFAULT 'RECEIVED' COMMENT '접수 진행상태',
    created_at     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '신청 시각; 한국 시간',

    PRIMARY KEY (request_id),
    CONSTRAINT uq_as_request_no UNIQUE (request_no),
    CONSTRAINT ck_as_request_no CHECK (CHAR_LENGTH(TRIM(request_no)) > 0),
    CONSTRAINT ck_as_request_customer_name CHECK (CHAR_LENGTH(TRIM(customer_name)) > 0),
    CONSTRAINT ck_as_request_region CHECK (CHAR_LENGTH(TRIM(region)) > 0),
    CONSTRAINT ck_as_request_product_id CHECK (product_id > 0),
    CONSTRAINT ck_as_request_technician_id CHECK (technician_id IS NULL OR technician_id > 0),
    CONSTRAINT ck_as_request_status CHECK (
        status IN ('RECEIVED', 'WAITING_ASSIGNMENT', 'ASSIGNED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED')
    ),
    INDEX idx_as_request_created_at (created_at, request_id),
    INDEX idx_as_request_status_created_at (status, created_at),
    INDEX idx_as_request_product_id (product_id),
    INDEX idx_as_request_technician_id (technician_id)
) ENGINE = InnoDB
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_0900_ai_ci
  COMMENT = '가전제품 AS 접수';

-- 제품과 기사는 다른 서버의 DB에 있으므로 FOREIGN KEY를 선언하지 않습니다.
-- 제품/기사의 실제 존재 여부 및 배정 처리는 각 서버 API를 통해 검증합니다.
