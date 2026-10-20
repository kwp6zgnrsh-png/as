# AS 접수 API

Spring Boot / Java 21 기반 REST API입니다. 현재는 DB 및 다른 서버와 연결하지 않는 API 준비 단계입니다.

## 실행

```sh
./gradlew bootRun
```

기본 주소는 `http://localhost:8080`입니다. 로컬 React 개발 서버(`http://localhost:5173`, `http://127.0.0.1:5173`)의 CORS 요청을 허용합니다.

## API

| Method | URL | 기능 | 정상 응답 |
| --- | --- | --- | --- |
| GET | `/api/products` | 제품 목록 조회 | 200, JSON 배열 |
| POST | `/api/repairs` | 신청 접수 | 201, 접수 정보 |
| GET | `/api/repairs` | 접수 목록 조회 | 200, JSON 배열 (최근 접수부터) |

### 제품 목록

현재 응답은 `[]`입니다. `ProductService.getProducts()`에서 제품 서버의 API를 호출하도록 교체합니다. 제품 서버가 자체 제품 DB를 조회하고, 이 서버는 그 API의 결과를 프론트에 전달하는 구조입니다.

연동할 제품 응답의 항목 구조는 다음과 같습니다. 아래 값은 명세 예시이며 실제 API에서 반환하는 데이터는 아닙니다.

```json
[
  {
    "productId": 101,
    "modelName": "정수기 A100",
    "category": "PURIFIER",
    "manufacturer": "제조사"
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

성공 응답에는 입력값과 함께 `requestId`, `status: "RECEIVED"`, `createdAt`이 포함됩니다. 현재는 메모리에 임시 저장하므로 서버 재시작 시 사라집니다. 제품 ID의 실제 존재 여부는 제품 서버 연동 후 확인하도록 TODO로 남겨 두었습니다.

### 접수 목록

`GET /api/repairs`는 POST 성공 응답과 같은 구조의 접수 배열을 반환합니다. 접수 전에는 `[]`입니다. 추후 `RepairService`의 임시 저장소를 접수 DB Repository로 교체하면 됩니다.

현재 목록 API는 개발용이며 이름과 연락처가 포함됩니다. 실제 운영 연동 시 목록 접근 권한과 응답 항목을 정해야 합니다.

## 검증

```sh
./gradlew test
```

제품 목록, 접수 후 목록 조회, 입력 오류, 과거 방문 일시, 로컬 CORS를 검증합니다.
