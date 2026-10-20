# AS 접수 DB

첨부 이미지에 있는 10개 컬럼만 사용합니다. MySQL 8.0.16 이상 기준입니다.

| 컬럼명 | 타입 | 설명 |
| --- | --- | --- |
| request_id | BIGINT PK | 접수 ID |
| request_no | VARCHAR(30) | 외부 표시용 접수번호 |
| customer_name | VARCHAR(50) | 신청자명 |
| phone | VARCHAR(20) | 연락처 |
| product_id | BIGINT | 제품 참조 ID |
| region | VARCHAR(50) | 방문 지역 |
| symptom | TEXT | 고장 증상 |
| technician_id | BIGINT NULL | 배정 기사 참조 ID |
| status | VARCHAR(30) | 접수 진행상태 |
| created_at | DATETIME | 신청 시각 |

## 실행

`as_request` 폴더에서 실행합니다. MySQL 계정은 환경에 맞게 바꿉니다.

```sh
mysql --default-character-set=utf8mb4 -u root -p < db/schema.sql
mysql --default-character-set=utf8mb4 -u root -p < db/seed.sql
mysql --default-character-set=utf8mb4 -u root -p < db/verify.sql
```

`schema.sql`은 `as_db.AS_REQUEST`를 생성합니다. 기존 테이블을 삭제하거나 변경하지 않습니다. 접수 ID는 자동 증가하고, 접수번호는 중복을 허용하지 않습니다. 제품·기사 ID는 다른 서버가 관리하므로 외래키를 선언하지 않습니다.

`seed.sql`에는 가상의 테스트 데이터 20건이 있습니다. 17개 시·도와 접수, 배정 대기, 배정 완료, 수리 중, 완료, 취소 상태를 포함합니다. 배정 전/후 기사 ID NULL 여부와 고장 증상이 없는 사례 및 여러 줄 증상도 포함합니다.

접수번호는 `AS-TEST-0001`~`AS-TEST-0020`입니다. 다시 실행하면 같은 접수번호의 기존 행을 유지합니다. 제품 ID 101~106, 기사 ID 701~706은 예시 참조값입니다. 서버 연동 시 외부 서버의 데이터에 맞춰야 합니다.

날짜는 한국 시간 기준으로 저장합니다. SQL은 세션 시간대를 `+09:00`으로 설정합니다.

SQL 파일만 제공하며, 기존 메모리 기반 API의 DB 연결은 변경하지 않습니다.
