# MSAProject

DDD(Domain-Driven Design)와 MSA(Microservice Architecture)를 고려하여 회원(Member)과 **글(Post)** 모듈을 분리한 프로젝트입니다.

회원과 글을 서로 독립적인 모듈로 구성하고, 모듈 간 데이터 동기화에는 **이벤트 기반 통신**, 외부 API 조회가 필요한 경우에는 **HTTP API 통신**을 사용합니다. 또한 `Post` 모듈에서는 `Member` Entity를 직접 참조하지 않고 `PostMember`라는 회원 복제본을 사용하여 모듈 간 결합도를 낮추도록 구성했습니다.

## 1. 실행 방법

### 1-1. 개발 환경

| **항목** | **설정** | 
| **JDK** | Java 25 | 
| **Framework** | Spring Boot | 
| **Build Tool** | Gradle | 
| **Database** | MySQL | 
| **ORM** | Spring Data JPA | 
| **실행 프로필** | `local` | 
| **서버 포트** | `8080` | 
| **IDE** | IntelliJ IDEA | 

### 1-2. MySQL 데이터베이스 생성

본 프로젝트는 H2를 사용하지 않고 MySQL을 사용합니다. 먼저 MySQL에서 프로젝트용 데이터베이스를 생성합니다.

```
CREATE DATABASE msa_project
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

```

데이터베이스가 정상적으로 생성되었는지 확인합니다.

```
SHOW DATABASES;

```

다음 항목이 존재하면 됩니다:

* `msa_project`

### 1-3. 로컬 DB 설정

DB 접속 정보에는 개인적인 비밀번호가 포함될 수 있기 때문에 해당 설정 파일은 Git에 커밋하지 않습니다. 현재 프로젝트에서는 다음과 같이 `local` 프로필을 사용합니다.

```
spring:
  profiles:
    active: local

```

실제 MySQL 접속 정보는 로컬 환경의 다음 파일에서 관리합니다.
`src/main/resources/application-local.yaml`

예시는 다음과 같습니다:

```
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/msa_project?serverTimezone=Asia/Seoul&characterEncoding=UTF-8
    username: root
    password: YOUR_MYSQL_PASSWORD
    driver-class-name: com.mysql.cj.jdbc.Driver

```

> **주의**
>
> * `YOUR_MYSQL_PASSWORD`에는 본인의 로컬 MySQL root 비밀번호를 입력합니다.
>
> * 실제 MySQL 비밀번호는 README 또는 Git 저장소에 작성하지 않습니다.
>
> * `application-local.yaml`은 `.gitignore`에 등록하여 Git에 업로드되지 않도록 관리합니다.

### 1-4. JDK 설정

프로젝트는 **Java 25**를 사용합니다. 터미널에서 다음 명령으로 현재 Java 버전을 확인합니다.

```
java -version

```

Java 25가 출력되어야 합니다.

```
java version "25"

```

IntelliJ에서도 다음과 같이 설정합니다.

1. `File` → `Project Structure` → `Project` → `SDK` → `JDK 25`

2. Gradle JVM 설정: `Settings` → `Build, Execution, Deployment` → `Build Tools` → `Gradle` → `Gradle JVM` → `JDK 25`

### 1-5. 프로젝트 실행

Gradle을 이용하여 프로젝트를 실행할 수 있습니다. Windows 환경에서는 다음 명령을 사용합니다.

```
gradlew.bat bootRun

```

또는 IntelliJ에서 다음 실행 클래스를 실행합니다:

* `MsaApplication`

프로젝트 실행 후 다음 주소에서 서버가 실행됩니다:

* `http://localhost:8080`

현재 프로젝트의 기본 프로필은 `local`이므로 실행 시 로컬 MySQL 설정을 사용합니다.

## 2. 프로젝트 구조

프로젝트는 크게 다음과 같이 구성되어 있습니다.

```
com.back
├── boundedContext
│   ├── member
│   │   ├── in
│   │   ├── app
│   │   ├── domain
│   │   └── out
│   │
│   └── post
│       ├── in
│       ├── app
│       ├── domain
│       └── out
│
├── global
│
└── shared
    ├── member
    └── post

```

### 2-1. Member 모듈

```
member
├── in
├── app
├── domain
└── out

```

* **`in`**: 외부에서 Member 모듈로 들어오는 진입점입니다. (주요 구성 요소: `Controller`, `EventListener`, `DataInit`)

* **`app`**: 애플리케이션의 업무 흐름을 담당합니다. (`MemberFacade`, `MemberJoinUseCase`)

* **`domain`**: 회원과 관련된 핵심 도메인 로직을 담당합니다. (`Member`, `MemberPolicy`)

* **`out`**: 외부 저장소와 연결되는 영역입니다. (`MemberRepository`)

### 2-2. Post 모듈

`Post` 역시 `Member`와 동일한 구조를 사용합니다. 주요 구성 요소는 다음과 같습니다.

* `PostFacade`, `PostWriteUseCase`, `Post`, `PostComment`, `PostMember`, `PostRepository`, `PostMemberRepository`, `PostEventListener`

### 2-3. Member와 Post를 모듈로 분리한 이유

Member와 Post가 서로 상대 모듈의 Entity를 직접 참조하도록 만들면 모듈 간 결합도가 높아집니다. 예를 들어 다음과 같은 직접 참조 구조는 사용하지 않습니다.

```
Post ──(X)──> Member Entity

```

대신 `Post` 모듈에서는 자신의 모듈에 존재하는 회원 복제본을 사용합니다.

```
Post ────> PostMember

```

따라서 `Post` 모듈은 `Member` 모듈의 `Member` Entity에 직접 의존하지 않습니다.

### 2-4. PostMember의 역할

`PostMember`는 `Post` 모듈에서 사용하는 `Member`의 복제본입니다. 회원이 생성되면 다음과 같은 흐름으로 `PostMember`가 생성됩니다.

```
Member 생성 
  ↓
MemberJoinedEvent 
  ↓
PostEventListener 
  ↓
PostFacade.syncMember() 
  ↓
PostMember 생성

```

따라서 Member DB와 Post DB를 실제 MSA 환경에서 분리하더라도 Post 모듈은 자신의 회원 정보를 가지고 동작할 수 있는 구조입니다.

### 2-5. 회원 정보 변경 시 복제본 동기화

회원의 활동점수가 변경되면 새로운 `PostMember`를 생성하지 않고, 기존 `PostMember`를 조회한 후 값을 업데이트합니다.

```
Member 활동점수 변경 
  ↓
MemberModifiedEvent 
  ↓
PostEventListener 
  ↓
PostFacade.syncMember() 
  ↓
기존 PostMember 조회 
  ↓
syncFrom() 
  ↓
UPDATE

```

따라서 `PostMember`가 `1개 → 2개 → 3개 → ...` 처럼 무한히 늘어나지 않고, 항상 기존 회원과 동일한 ID의 `PostMember`를 업데이트합니다.

## 3. 이벤트와 HTTP API를 구분한 이유

이번 프로젝트에서는 모듈 간 통신을 하나의 방식으로 통일하지 않고 이벤트 통신과 HTTP 통신을 목적에 따라 구분했습니다.

### 3-1. 이벤트 통신

회원과 Post 모듈의 데이터 동기화에는 이벤트를 사용합니다. (대표 이벤트: `MemberJoinedEvent`, `MemberModifiedEvent`, `PostCreatedEvent`, `PostCommentCreatedEvent`)

* **회원 가입 시:**

  ```
  Member ──> MemberJoinedEvent ──> PostEventListener ──> PostMember 생성
  
  ```

* **활동점수 변경 시:**

  ```
  Member ──> MemberModifiedEvent ──> PostEventListener ──> PostMember UPDATE
  
  ```

### 3-2. HTTP API 통신

반면 글을 작성하는 과정에서 회원 모듈의 보안 팁을 조회해야 하는 경우에는 HTTP API를 사용합니다.

```
PostWriteUseCase 
  ↓
MemberApiClient 
  ↓ (HTTP 요청)
Member Controller 
  ↓
MemberFacade 
  ↓
MemberPolicy 
  ↓ (보안 팁 반환)

```

> **요약 기준**
>
> * 데이터 동기화 ──> **Event**
>
> * 현재 필요한 데이터 조회 ──> **HTTP API**

## 4. 회원 복제 흐름

### 4-1. 회원 가입

```
회원 가입 요청 
  ↓
MemberController 
  ↓
MemberFacade 
  ↓
MemberJoinUseCase 
  ↓
Member 저장 
  ↓
MemberJoinedEvent 
  ↓
Transaction Commit 
  ↓
PostEventListener 
  ↓
PostFacade.syncMember() 
  ↓
PostMember 생성

```

### 4-2. 활동점수 변경

* **글 작성 시:**

  ```
  Post 생성 ──> PostCreatedEvent ──> Transaction Commit ──> MemberEventListener ──> Member 활동점수 +3 ──> MemberModifiedEvent ──> PostEventListener ──> PostMember UPDATE
  
  ```

* **댓글 작성 시:**

  ```
  Post.addComment() ──> PostCommentCreatedEvent ──> Transaction Commit ──> MemberEventListener ──> Member 활동점수 +1 ──> MemberModifiedEvent ──> PostEventListener ──> PostMember UPDATE
  
  ```

## 5. 확인 결과

### 5-1. 초기 회원 데이터

애플리케이션 최초 실행 후 다음 SQL을 실행합니다.

```
SELECT COUNT(*) FROM MEMBER_MEMBER;

```

* **예상 결과:** `6`

* **생성되는 회원 목록:** `system`, `holding`, `admin`, `user1`, `user2`, `user3`

### 5-2. 초기 Post 데이터

```
SELECT COUNT(*) FROM POST_POST;

```

* **예상 결과:** `6`

* **회원별 글 개수:** `user1` (3개), `user2` (2개), `user3` (1개)

**확인용 SQL:**

```
SELECT
    pm.username,
    COUNT(p.id) AS post_count
FROM POST_POST p
JOIN POST_MEMBER pm
    ON p.author_id = pm.id
GROUP BY pm.username
ORDER BY pm.username;

```

* **예상 결과:**

  ```
  user1    3
  user2    2
  user3    1
  
  ```

### 5-3. 초기 댓글 데이터

```
SELECT COUNT(*) FROM POST_POST_COMMENT;

```

* **예상 결과:** `8`

### 5-4. 활동점수 확인

* **활동점수 규칙:** 글 작성 (`+3점`), 댓글 작성 (`+1점`)

* **회원별 활동량 계산:**

  * `user1`: 글 3개 (`+9`) + 댓글 2개 (`+2`) = **총점 11**

  * `user2`: 글 2개 (`+6`) + 댓글 3개 (`+3`) = **총점 9**

  * `user3`: 글 1개 (`+3`) + 댓글 3개 (`+3`) = **총점 6**

**확인용 SQL:**

```
SELECT
    username,
    activity_score
FROM MEMBER_MEMBER
ORDER BY id;

```

* **예상 결과:**

  ```
  system     0
  holding    0
  admin      0
  user1     11
  user2      9
  user3      6
  
  ```

### 5-5. Member와 PostMember 일치 여부 확인

```
SELECT
    m.id,
    m.username,
    m.nickname,
    m.activity_score AS member_score,
    pm.id AS post_member_id,
    pm.username AS post_member_username,
    pm.nickname AS post_member_nickname,
    pm.activity_score AS post_member_score
FROM MEMBER_MEMBER m
JOIN POST_MEMBER pm
    ON m.id = pm.id
ORDER BY m.id;

```

* **예상 결과:**

  * Member ID와 PostMember ID가 동일

  * Member username = PostMember username

  * Member nickname = PostMember nickname

  * Member activity_score = PostMember activity_score (`user1` → 11/11, `user2` → 9/9, `user3` → 6/6)

### 5-6. 재실행 시 중복 여부 확인

애플리케이션을 종료한 후 다시 실행하고 SQL을 수행합니다.

```
SELECT COUNT(*) FROM MEMBER_MEMBER; -- 6
SELECT COUNT(*) FROM POST_MEMBER;   -- 6
SELECT COUNT(*) FROM POST_POST;     -- 6
SELECT COUNT(*) FROM POST_POST_COMMENT; -- 8

```

활동점수와 데이터가 중복 증가하거나 누적되지 않고 초기 데이터 그대로 유지되어야 합니다.

### 5-7. 보안 팁 API 확인

다음 API를 호출합니다:

* `GET /api/v1/member/members/randomSecureTip`

* 예시 주소: `http://localhost:8080/api/v1/member/members/randomSecureTip`

* **응답:** `비밀번호의 유효기간은 90일 입니다.`

글 작성 시 최종 메시지에도 보안 팁이 포함됩니다:

* 예시: `1번 글이 생성되었습니다. 보안 팁 : 비밀번호의 유효기간은 90일 입니다.`

## 6. 최종 확인

프로젝트 실행 후 다음 항목이 모두 만족되어야 합니다.

* **\[초기 데이터\]**

  * Member → 6개

  * PostMember → 6개

  * Post → 6개

  * Comment → 8개

* **\[회원별 글\]**

  * user1 → 3개 / user2 → 2개 / user3 → 1개

* **\[활동점수\]**

  * user1 → 11점 / user2 → 9점 / user3 → 6점

* **\[복제본\]**

  * Member와 PostMember의 ID, username, nickname, activity_score가 모두 일치

* **\[재실행\]**

  * 중복 데이터 생성 및 활동점수 중복 증가 없음

* **\[HTTP API\]**

  * 보안 팁 API 정상 작동 확인
