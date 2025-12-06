## Holiday Mini Service
#### 1. 서비스 설명
   
전 세계 공휴일 데이터를 외부 API(Nager API)로부터 수집·조회·관리하는 Spring Boot 기반 Mini Service입니다.

최근 5년(2020~2025) 간의 공휴일 정보를 저장하여 검색·재동기화·삭제할 수 있으며, Swagger UI를 통해 API 문서도 자동으로 제공합니다.
***
#### 2. 기술 스택

| Layer       | Tech                           |
| ----------- | ------------------------------ |
| Language    | Java 21                        |
| Framework   | Spring Boot 3.4.x              |
| Persistence | JPA(Hibernate) + Querydsl 5    |
| Database    | H2 인메모리 DB                     |
| HTTP Client | WebClient                      |
| Build       | Gradle (Groovy)                |
| Test        | JUnit 5 + Mockito              |
| API 문서      | Springdoc OpenAPI (Swagger UI) |

***
#### 3. ERD 설계
<img width="1075" height="305" alt="image" src="https://github.com/user-attachments/assets/03bec953-b6a1-4f2f-a631-cddf34565aaf" />

ERD 설계는 다음과 같은 관계를 중심으로 구성됩니다:

- 주요 테이블

Country (1) — (N) Holiday

HolidayType (1) — (N) Holiday

- Holiday 유니크 조건

외부 공휴일 데이터는 국가별로 동일 날짜에 동일 이름이 중복될 가능성이 있기 때문에
아래 3개 조합을 Unique Index로 사용했습니다.
```
(country_code, holiday_date, local_name)
```
***
#### 4. 빌드 & 실행 방법

실행
```
./gradlew bootRun
```

H2 콘솔 접속
```
http://localhost:8080/h2-console
JDBC URL: jdbc:h2:mem:holidays
```
***
#### 5. REST API 요약
1) 공휴일 검색(페이징) ->  GET /api/holidays

Query Parameters
| Name        | Type      | Required | Description |
| ----------- | --------- | -------- | ----------- |
| year        | Integer   | ❌        | 연도          |
| countryCode | String    | ❌        | 국가 코드       |
| from        | LocalDate | ❌        | 시작 날짜       |
| to          | LocalDate | ❌        | 종료 날짜       |
| typeCode    | String    | ❌        | 공휴일 타입      |
| page        | int       | ❌        | 페이지 번호      |
| size        | int       | ❌        | 페이지 크기      |

Example
```
GET /api/holidays?year=2025&countryCode=KR&page=0&size=20
```

Response
<img width="1792" height="467" alt="image" src="https://github.com/user-attachments/assets/b4d87855-ccaa-4815-9b46-b00579fac888" />


2) 공휴일 재동기화(Refresh) -> POST /api/holidays/refresh

Query parameters
| Name        | Required | Description |
| ----------- | -------- | ----------- |
| year        | ✔        | 갱신할 연도      |
| countryCode | ✔        | 국가 코드       |

Example
```
POST /api/holidays/refresh?year=2025&countryCode=KR
```

Response
<img width="565" height="178" alt="image" src="https://github.com/user-attachments/assets/bd83de7c-2fe1-4b97-af56-4f3d91808737" />


3) 공휴일 삭제(Delete) -> DELETE /api/holidays

Query Parameters
| Name        | Required | Description |
| ----------- | -------- | ----------- |
| year        | ✔        | 갱신할 연도      |
| countryCode | ✔        | 국가 코드       |

Example
```
DELETE /api/holidays?year=2025&countryCode=KR
```

Response
<img width="558" height="174" alt="image" src="https://github.com/user-attachments/assets/5c520e2e-3aaa-4c5c-9f76-dc1c71af0937" />


#### 6. Swagger UI / OpenAPI 문서

swagger ui
```
http://localhost:8080/swagger-ui/index.html
```

openapi Json
```
http://localhost:8080/v3/api-docs
```

#### 7. 테스트 성공 스크린샷







