# Crimson Citadel — Spring Boot 카드 로그라이크 게임 서버

> 내일배움캠프 게임서버 백엔드 부트캠프 **Spring 입문 과제** 제출용 프로젝트입니다.
>
> 원본 프로젝트인 [f-api/game-spring-basic-assignment](https://github.com/f-api/game-spring-basic-assignment)를 포크하여, 비어 있던 백엔드 API 서버를 Spring Boot + Spring Data JPA + MySQL(Docker)로 구현했습니다.
>
> 본 README는 **배포를 목적으로 작성된 문서가 아니며**, 과제 제출 및 튜터 평가를 위해 구현 내용과 설계 의도를 정리한 문서입니다.

- 원본(포크 대상) 게임 데모: https://nhahan.github.io/crimson-citadel/
- API 명세: https://f-api.github.io/game-spring-api-docs/basic/api-docs.html

## 과제 개요

| 항목 | 내용 |  
|---|---|  
| 과정 | 내일배움캠프 게임서버 백엔드 부트캠프 |  
| 과제 | Spring 입문 — Crimson Citadel 백엔드 API 구현 |  
| 제출 목적 | 과제 제출 및 튜터 코드 리뷰/평가 |  
| 원본 저장소 | [f-api/game-spring-basic-assignment](https://github.com/f-api/game-spring-basic-assignment) (Fork) |  

## 기술 스택

|구분|내용|  
|---|---|  
|Language / Framework|Java, Spring Boot|  
|Data Access|Spring Data JPA (Hibernate)|  
|Database|MySQL (Docker), H2|  
|Build Tool|Gradle|  
|API / 통합 테스트|Postman, JUnit5, MockMvc|  

## 로컬 실행 방법

1. Docker로 MySQL 컨테이너 실행

    ```bash  
    docker run --name crimson-citadel-mysql \  
      -e MYSQL_ROOT_PASSWORD=12345678 \      -e MYSQL_DATABASE=crimson_citadel \      -p 3306:3306 \      -d mysql:8.4  
    ```  
2. `src/main/resources/application.properties` 작성

    ```properties  
    spring.datasource.url=jdbc:mysql://localhost:3306/<DB_NAME>  
    spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver    spring.datasource.username=root    spring.datasource.password=<PASSWORD>    spring.jpa.hibernate.ddl-auto=update  
    ```  
3. 서버 실행

    ```bash  
    ./gradlew bootRun  
    ```  
4. 브라우저에서 `http://localhost:8080` 접속하여 동작 확인

## 아키텍처 / 설계 원칙

- **3 Layer Architecture**: Controller / Service / Repository로 역할을 분리했습니다.
- **단방향 연관관계만 사용**: 엔티티 간 연관관계는 단방향으로만 구성했습니다. `cascade`, `orphanRemoval`, 양방향 컬렉션은 사용하지 않았으며, 자식 엔티티(`RunCard`)의 조회·저장·삭제는 Repository를 통해 명시적으로 처리합니다.
- **API 명세 100% 준수**: 경로, JSON 필드명, enum 값을 명세와 정확히 일치시켰습니다.

## 구현 내용 (레벨별 PR)

### 필수 기능

|레벨|내용|PR|  
|---|---|---|  
|Lv1|설정 파일 작성 — Docker MySQL 연결|[#1](https://github.com/Eunseok/game-spring-basic-assignment/pull/1)|  
|Lv2|빈 등록 고치기 — 의존성 주입|[#2](https://github.com/Eunseok/game-spring-basic-assignment/pull/2)|  
|Lv3|RESTful 경로 맞추기 — 게임 목록 API|[#3](https://github.com/Eunseok/game-spring-basic-assignment/pull/3)|  
|Lv4|`@Transactional` 버그 고치기|[#4](https://github.com/Eunseok/game-spring-basic-assignment/pull/4)|  
|Lv5|요청 검증과 응답 DTO — 게임 생성|[#5](https://github.com/Eunseok/game-spring-basic-assignment/pull/5)|  
|Lv6|보상 카드 선택과 진행 저장|[#6](https://github.com/Eunseok/game-spring-basic-assignment/pull/6)|  
|Lv7|목록·상세 조회 — 저장된 여정 이어하기|[#7](https://github.com/Eunseok/game-spring-basic-assignment/pull/7)|  
|Lv8|변경 감지로 이름 수정, 자식부터 삭제|[#8](https://github.com/Eunseok/game-spring-basic-assignment/pull/8)|  

### 도전 기능

|레벨|내용|PR|  
|---|---|---|  
|Lv9|끝난 게임 덮어쓰기 막기 — 409|[#9](https://github.com/Eunseok/game-spring-basic-assignment/pull/9)|  
|Lv10|전역 예외 처리 — 404·409에 message 붙이기|[#10](https://github.com/Eunseok/game-spring-basic-assignment/pull/10)|  
|Lv11|N+1 없는 카드 수 집계와 저장 시간|[#11](https://github.com/Eunseok/game-spring-basic-assignment/pull/11)|  
|Lv12|랭킹|[#12](https://github.com/Eunseok/game-spring-basic-assignment/pull/12)|  

### 테스트

|레벨|내용|PR|  
|---|---|---|  
|선택|API 통합 테스트 작성하기|[#15](https://github.com/Eunseok/game-spring-basic-assignment/pull/15)|  

> 각 레벨의 상세한 구현 과정과 트러블슈팅(에러 로그 → 원인 분석 → 해결)은 위 PR을 참고해 주세요.

## 트러블슈팅 하이라이트

- **게임 목록 조회 시 ID 내림차순 정렬이 보장되지 않는 문제**: [#13](https://github.com/Eunseok/game-spring-basic-assignment/pull/13)
- **PlayerName 변경 요청 유효성 테스트 중 요청값이 NULL일 때 204를 반환하는 문제**: [#14](https://github.com/Eunseok/game-spring-basic-assignment/pull/14)

## 과제 제출 질문 답변

1. Controller, Service, Repository는 각각 어떤 역할을 맡나요?

   > Controller는 HTTP 통신을 통해 URL을 매핑하고, 요청 DTO를 Service에 전달한 뒤 반환된 응답 DTO를 클라이언트에 전달하는 역할입니다.

   > Service는 Repository에게 데이터를 요청하고 비즈니스 로직을 처리한 뒤 응답 DTO를 생성하는, 실제 데이터를 다루는 레이어입니다.

   > Repository는 데이터베이스에 저장되어 있는 데이터를 가져오는 역할입니다.

2. `@Service`를 붙이지 않으면 서버가 뜨지 않는 이유는 무엇인가요?

   > LV2에 대한 질문이라고 생각하고 답변하겠습니다. `GameController`가 `GameService`를 주입받기를 기대하고 있습니다. 스프링 환경에서 DI를 위해서는 해당 객체가 Bean Container에 Bean으로 등록되어 있어야 하는데, `@Service`가 없으면 `Required a bean of type ... could not be found`라는 에러가 발생합니다.

3. `@Transactional(readOnly = true)`는 무슨 뜻이며, 저장하는 메서드에 붙이면 왜 안 되나요?

   > 해당 트랜잭션에서는 데이터를 수정하지 않고 읽기 전용으로 사용하겠다는 뜻입니다. 읽기 전용 트랜잭션에서 데이터를 수정하려 하면 `Connection is read-only ...`와 같은 에러가 발생합니다.

4. `@NotBlank`, `@NotNull`, `@NotEmpty`는 각각 어떤 값을 걸러내나요?

| 검증 값  | @NotNull | @NotEmpty | @NotBlank |
| :---: | :------: | :-------: | :-------: |
| null  |    O     |     O     |     O     |
|  ""   |    X     |     O     |     O     |
|  " "  |    X     |     X     |     O     |
| 적용 대상 |  모든 객체   | 문자열, 컬렉션  |    문자열    |

2. 엔티티를 그대로 응답하지 않고 DTO로 바꿔서 응답하는 이유는 무엇인가요?

   > 보안상 문제 - 엔티티 구조는 결국 실제 DB의 구조를 나타냅니다. 이를 그대로 노출하면 보안상 문제가 발생할 수 있습니다.

   > 관심사 분리 - 요청마다 필요한 데이터가 다를 수 있습니다. 각 요청과 응답에 필요한 정보만 담기 위함입니다.

3. 이름 변경에서 `save()`를 호출하지 않았는데 DB에 반영되는 이유는 무엇인가요?
   > 영속성 컨텍스트가 관리 중인 영속 상태의 객체는, 트랜잭션이 끝날 때 최초 상태와 비교하는 더티 체킹(Dirty Checking)을 수행하여 변경 사항이 있으면 자동으로 DB에 반영합니다.
