SET NAMES utf8mb4;

-- 서버 B 전용 DB (product_db). 컨테이너가 처음 만들어질 때 한 번만 실행됩니다.
-- 데이터베이스와 계정은 compose의 MYSQL_DATABASE / MYSQL_USER 로 만들어집니다.
CREATE TABLE IF NOT EXISTS product (
    product_id   BIGINT       NOT NULL AUTO_INCREMENT,
    product_name VARCHAR(50)  NOT NULL,
    product_type VARCHAR(30)  NOT NULL,
    PRIMARY KEY (product_id),
    UNIQUE KEY uk_product_type (product_type),
    CONSTRAINT chk_product_type CHECK (product_type IN
        ('AIR_PURIFIER','AIR_CONDITIONER','REFRIGERATOR','TV','MICROWAVE'))
) DEFAULT CHARSET=utf8mb4;

INSERT INTO product (product_id, product_name, product_type) VALUES
    (1, '공기청정기', 'AIR_PURIFIER'),
    (2, '에어컨',     'AIR_CONDITIONER'),
    (3, '냉장고',     'REFRIGERATOR'),
    (4, 'TV',         'TV'),
    (5, '전자레인지', 'MICROWAVE');
