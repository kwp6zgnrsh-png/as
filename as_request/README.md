# AS 접수 API

Spring Boot / Java 21 기반 REST API입니다. `GET /api/repairs`는 MySQL을 조회합니다. 제품 목록은 제품 서버 API에서 받아오며, POST의 DB 저장은 아직 준비 단계입니다.

## 실행

```sh
./gradlew bootRun
```

MySQL 연결 대상은 `localhost:3306/as_db`, 테이블은 `as_request`입니다.

개인 DB 설정은 `src/main/resources/local.yaml`에 입력합니다. URL의 `as_db`를 DB 이름으로 바꾸고 `username`, `password`를 설정하세요. `application.yaml`에서 자동으로 불러오므로 별도 프로필 설정은 필요 없습니다. `local.yaml`은 `.gitignore`에 등록되어 커밋되지 않습니다.

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/as_db?serverTimezone=Asia/Seoul&characterEncoding=UTF-8
    username: root
    password: ""
```

새로 클론한 환경에서는 위 내용으로 `local.yaml`을 직접 만드세요. 파일이 없으면 `application.yaml`의 기본 설정과 `DB_USERNAME`, `DB_PASSWORD` 환경변수를 사용합니다. 파일이 있으면 로컬 파일의 설정을 우선합니다.

앱 실행 시 테이블 생성 및 테스트 데이터 삽입은 자동 실행하지 않습니다.

기본 주소는 `http://localhost:8080`입니다. 로컬 React 개발 서버(`http://localhost:5173`, `http://127.0.0.1:5173`)의 CORS 요청을 허용합니다.

## API

| Method | URL | 기능 | 정상 응답 |
| --- | --- | --- | --- |
| GET | `/api/products` | 제품 목록 조회 | 200, JSON 배열 |
| POST | `/api/repairs` | 신청 접수 | 201, 접수 정보 |
| GET | `/api/repairs` | 접수 목록 조회 | 200, JSON 배열 (최근 접수부터) |

### 제품 목록

프론트는 로컬 AS 접수 서버의 `GET /api/products`를 호출합니다. `AsRequestController → ProductService → RestClient`가 `http://172.21.33.68:8080/api/products`에 GET 요청을 보내고, 받은 제품 목록을 프론트로 반환합니다. Vite 개발 프록시도 원격 제품 서버가 아닌 `http://localhost:8080`으로 연결합니다.

제품 서버 주소는 `product-service.base-url` 설정 또는 `PRODUCT_SERVICE_URL` 환경변수로 변경할 수 있습니다. 연결 제한은 3초, 응답 읽기 제한은 5초입니다. 제품 서버 오류나 빈 응답 본문은 전역 예외 처리기가 502와 오류 메시지로 반환합니다. 정상적인 빈 배열은 `[]`로 전달합니다.

실제 제품 서버의 `productId`, `productName`, `productType` 필드를 사용합니다. 프론트에서도 `productName`을 표시하고 `productId`를 선택값으로 사용합니다. 응답 항목 예시는 다음과 같습니다.

```json
[
  {
    "productId": 1,
    "productName": "공기청정기",
    "productType": "AIR_PURIFIER"
  }
]
```

### 신청 접수

`POST /api/repairs`의 JSON 본문:

```json
{
  "productId": 101,
  "customerName": "테스트",
  "phone": "01000000000",
  "region": "서울특별시",
  "visitDateTime": "2099-01-01T14:00",
  "memo": "전원이 켜지지 않습니다."
}
```

- 제품명 대신 제품 목록에서 선택한 `productId`를 보냅니다.
- `memo`는 선택 항목이며 나머지는 필수입니다.
- 연락처는 하이픈 없는 9~11자리 숫자이며 0으로 시작해야 합니다.
- 지역은 시·도 문자열로 보냅니다.
- `visitDateTime`은 한국 방문 일정을 기준으로 한 로컬 날짜·시간입니다. 서버도 한국 시간대에서 실행하는 것을 전제로 하며, 현재 이후의 시간을 입력합니다.
- 잘못된 입력은 400과 `{"message": "오류 안내"}`를 반환합니다.

POST는 아직 DB 저장 없이 `requestId`, `status: "RECEIVED"`, `createdAt`이 포함된 임시 응답만 반환합니다. 입력 `memo`는 응답의 `symptom`에 매핑합니다. DB 접수번호와 기사 ID는 미정이므로 `requestNo`, `technicianId`는 NULL입니다. 이 POST 결과는 DB 목록 조회에 나타나지 않습니다. POST 입력 검증 후 `ProductService.getProduct(productId)`가 제품 서버의 `GET /api/products/{productId}`를 호출합니다. 제품 상세 조회가 성공해야 기존 임시 접수 응답을 반환하며, 실패하면 전역 예외 처리기의 502 응답을 반환합니다. 접수 DB 저장과 기사 배정은 이번 단계에 포함하지 않습니다.

### 접수 목록

`GET /api/repairs`는 `RepairService → RepairRepository → JdbcTemplate`을 통해 MySQL의 `as_request` 테이블을 조회합니다. `created_at DESC, request_id DESC`로 정렬하며, 결과가 없으면 `[]`를 반환합니다.

사진의 10개 컬럼을 그대로 읽어 다음 JSON 필드로 반환합니다:
`requestId`, `requestNo`, `customerName`, `phone`, `productId`, `region`, `symptom`, `technicianId`, `status`, `createdAt`.

`technician_id`와 `symptom`이 NULL이면 응답도 NULL입니다. DB 연결 또는 조회 실패 시 전역 예외 처리기가 503과 오류 메시지를 반환합니다.

현재 목록 API는 개발용이며 이름과 연락처가 포함됩니다. 실제 운영 연동 시 목록 접근 권한과 응답 항목을 정해야 합니다.

## 검증

```sh
./gradlew test
```

H2의 MySQL 호환 모드에서 실제 SQL 조회, 10개 필드 매핑, 최신순 정렬, NULL 값, 빈 목록과 기존 API 입력 검증을 확인합니다. 테스트는 로컬 MySQL에 접속하거나 데이터를 변경하지 않습니다.
