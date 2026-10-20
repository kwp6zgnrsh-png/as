# 제품 단건 조회 API

## JDBC 연결

`DB_PASSWORD` 환경변수를 설정하고 `ProductApplication`을 실행하면 MySQL의 실제 `product` 테이블을 조회한다.

- DB: `172.21.26.80:3306/service_db`
- 계정: `product_app`
- 설정 파일: `src/main/resources/application.yaml`
- 환경변수 `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`로 접속 설정을 변경할 수 있다.
- DB 비밀번호는 저장소에 포함하지 않으며 `DB_PASSWORD`로 지정한다.
- 임시 샘플 저장소는 삭제했다. 테이블 생성이나 DB 데이터 변경은 수행하지 않는다.

## 요청

- Method: `GET`
- URL: `/products/{productId}`
- 요청 본문(JSON): 없음
- `productId`: 1 이상인 Java Long 범위의 정수

예: `GET /products/1`

## 성공 응답

- HTTP Status: `200 OK`
- Content-Type: `application/json`
- DB 컬럼은 `product_id`, `product_name`, `product_type`을 사용한다.

```json
{
  "productId": 1,
  "productName": "공기청정기",
  "productType": "AIR_PURIFIER"
}
```

## 제품이 없는 경우

- HTTP Status: `404 Not Found`
- Content-Type: `application/json`

```json
{
  "code": "PRODUCT_NOT_FOUND",
  "message": "제품을 찾을 수 없습니다."
}
```

## 잘못된 제품 ID

문자, 0 이하의 값, Long 범위를 벗어난 값은 다음과 같이 응답한다.

- HTTP Status: `400 Bad Request`
- Content-Type: `application/json`

```json
{
  "code": "INVALID_PRODUCT_ID",
  "message": "제품 ID는 1 이상의 정수여야 합니다."
}
```

## 실행하고 Apidog에서 확인하기

1. IntelliJ 실행 설정의 환경변수에 `DB_PASSWORD`를 지정한 뒤 `ProductApplication`의 main 메서드를 실행한다.
2. 콘솔에 `Started ProductApplication`이 표시되면 Apidog에서 `GET http://localhost:8080/products/1`을 요청한다.
3. DB에 해당 ID가 있으면 200과 실제 제품 정보가 반환된다. 없으면 404가 반환된다.

터미널에서는 `./gradlew.bat bootRun`으로 실행할 수 있다.
DB 서버에 접근 가능한 네트워크에서 실행해야 한다.
다른 PC에서는 localhost 대신 제품 API 서버 PC의 IP를 사용한다.
목록 조회(`GET /products`)와 상태 변경 API는 아직 구현하지 않았다.
