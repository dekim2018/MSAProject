# p-14116-1-0022 과제 제출본

## 1. 프로젝트 개요

Spring Boot + Spring Data JPA를 사용해 `member`와 `post` 두 bounded context를 분리한 미션 구현입니다.

- 회원 모듈: `boundedContext.member`
- 글 모듈: `boundedContext.post`
- 모듈 공용 계약: `shared`
- 공통 인프라: `global`
- 각 모듈은 `in / app / domain / out` 구조를 사용합니다.
- 모듈 간 Entity/Repository 직접 의존을 제거하고 `shared`의 DTO/Event/ApiClient를 사용합니다.
- 글 모듈은 회원 원본 Entity 대신 `PostMember` 복제본을 사용합니다.

## 2. 실행 환경

- JDK 25
- Spring Boot 4.0.1
- Gradle 9.2.1
- Spring Data JPA
- 기본 개발 DB: H2
- 선택 DB: MySQL 8.x
- 서버 포트: 8080

### IntelliJ 실행

1. JDK 25를 Project SDK와 Gradle JVM으로 지정합니다.
2. `BackApplication`을 실행합니다.
3. 기본 프로필은 `dev`이며 H2 파일 DB `db_dev`를 사용합니다.

### MySQL 사용

`application-mysql.yml`을 사용하려면 MySQL에 `msa_project` 데이터베이스를 생성하고 `MYSQL_PASSWORD` 환경변수를 설정합니다.

예:

```sql
CREATE DATABASE msa_project
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;
```

실행 프로필:

```text
mysql
```

기본 H2를 사용할 때는 H2 콘솔도 사용할 수 있습니다.

```text
http://localhost:8080/h2-console
```

- JDBC URL: `jdbc:h2:./db_dev`
- User Name: `sa`
- Password: 비워 둠

IntelliJ의 Run Configuration > Active profiles에 `mysql`을 입력합니다.

## 3. 초기 데이터

애플리케이션 최초 실행 시:

- 회원 6명
  - system
  - holding
  - admin
  - user1
  - user2
  - user3
- 글 6개
  - user1 3개
  - user2 2개
  - user3 1개
- 댓글 8개

활동점수는 이벤트로 계산됩니다.

- 글 작성: +3
- 댓글 작성: +1

최초 데이터 생성 이후 같은 DB로 재실행해도 초기화 코드가 다시 데이터를 추가하지 않습니다.

## 4. 모듈 구조

```text
boundedContext/
├── member/
│   ├── in/
│   ├── app/
│   ├── domain/
│   └── out/
└── post/
    ├── in/
    ├── app/
    ├── domain/
    └── out/
```

### member

`Member`가 회원 원본(Source of Truth)입니다.

### post

`PostMember`가 회원 복제본입니다.

회원 가입 이벤트가 발생하면 `PostMember`를 만들고, 회원 활동점수 변경 이벤트가 발생하면 같은 ID의 `PostMember`를 UPDATE 합니다.

## 5. 이벤트

모듈 간 이벤트 계약은 `shared`에 둡니다.

```text
MemberJoinedEvent
MemberModifiedEvent
PostCreatedEvent
PostCommentCreatedEvent
```

이벤트에는 Entity 대신 DTO가 전달됩니다.

회원 원본 → `MemberDto`
글 → `PostDto`
댓글 → `PostCommentDto`

작성 트랜잭션이 커밋된 후 활동점수 변경 이벤트를 별도 `REQUIRES_NEW` 트랜잭션에서 처리합니다.

따라서 글/댓글 작성 트랜잭션이 롤백되면 해당 활동점수 이벤트도 처리되지 않습니다.

## 6. HTTP API

회원 모듈에서 보안 팁 API를 제공합니다.

```text
GET /api/v1/member/members/randomSecureTip
```

글 모듈의 `MemberApiClient`가 위 API를 실제 HTTP로 호출합니다.

비밀번호 변경 주기 90일은 `MemberPolicy`가 관리합니다.

## 7. 확인 SQL

H2 콘솔 또는 MySQL Workbench에서 다음을 확인할 수 있습니다.

```sql
SELECT * FROM MEMBER_MEMBER;
SELECT * FROM POST_MEMBER;
SELECT * FROM POST_POST;
SELECT * FROM POST_POST_COMMENT;
```

초기 실행 후 기대 행 수:

```text
MEMBER_MEMBER      6
POST_MEMBER        6
POST_POST          6
POST_POST_COMMENT  8
```

회원 활동점수 기대값:

```text
user1 = 11
user2 = 9
user3 = 6
```

계산:

```text
user1: 글 3 × 3 + 댓글 2 × 1 = 11
user2: 글 2 × 3 + 댓글 3 × 1 = 9
user3: 글 1 × 3 + 댓글 3 × 1 = 6
```

`MEMBER_MEMBER`와 `POST_MEMBER`의 `id`, `username`, `nickname`, `activity_score`, 생성/수정 시각을 비교하여 복제 상태를 확인합니다.

비밀번호는 `PostMember`에 복제하지 않습니다.

## 8. 중복 실행 확인

애플리케이션을 종료한 뒤 같은 DB로 다시 실행합니다.

다음 행 수가 그대로인지 확인합니다.

```text
회원 6
글 6
댓글 8
PostMember 6
```

활동점수도 다시 증가하지 않아야 합니다.

## 9. 중복 username

`MemberJoinUseCase`는 이미 존재하는 username을 검사하고 다음 예외를 발생시킵니다.

```text
resultCode = 409-1
msg = 이미 존재하는 username 입니다.
```

예외 타입:

```text
DomainException
```

## 10. 선택 통합 테스트

과제에서 요구하는 핵심 흐름은 다음 두 종류의 통합 테스트로 확장할 수 있습니다.

1. 글 +3 / 댓글 +1 / 작성 롤백 시 점수 미증가
2. 회원 가입 및 활동점수 변경 시 `PostMember`가 같은 ID로 동기화되고 행이 추가되지 않는지 검증
