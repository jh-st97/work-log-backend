# work-log

업무를 관리하면서 기록을 쌓고, 이직할 때 이력서와 면접에서 그대로 꺼내 쓰는 **개인용 업무 기록 서비스**입니다.

시간이 지나면 "내가 어떤 기술로, 어떤 문제를, 어떻게 풀고, 결과가 어땠는지"가 기억나지 않습니다.
그래서 이력서를 쓸 때마다 처음부터 기억을 더듬어야 했습니다.
이를 해결하려고, 일을 하는 동안 **기술 · 문제와 해결 · 결과(개선 수치)** 를 바로 남길 수 있게 만들었습니다.

- 프론트엔드: [work-log-frontend](https://github.com/jh-st97/work-log-frontend)
- 이 저장소: 백엔드(REST API)

> 회사 문서나 코드는 담지 않고, 기억 속의 기술·문제·해결·수치만 본인 말로 적는 용도입니다.
> 아래 예시와 데이터는 모두 가상입니다.

## 주요 기능

**회원가입 / 로그인**

- JWT 인증
- 내 데이터만 조회·수정

**프로젝트 · 업무 관리**

- 상태: 예정 / 진행 / 완료
- 우선순위, 마감일
- 삭제 대신 보관 처리 (보관함에서 복구하거나 영구 삭제 가능)
- 영구 삭제는 보관한 항목만 가능하고, 업무가 남은 프로젝트는 삭제되지 않음
- 프로젝트를 보관하면 그 안의 업무도 목록에서 함께 숨김

**기술 태그 · 업무 시스템**

- 업무에 사용한 기술과 시스템으로 분류
- 한 업무에 여러 개 지정 가능

**성과 항목**

- 지표명, 개선 전, 개선 후를 문자열로 기록
- 예: 배포 시간 `30분` → `5분`, 배포 방식 `수동` → `자동`

**일일 기록 · 진행 메모**

- 하루 회고
- 업무별로 겪은 문제, 해결 방법, 소요 시간

**조회**

- 필터: 상태, 우선순위, 프로젝트, 시스템, 태그, 마감일 범위, 키워드
- 페이징

**이력서용 내보내기**

- 고른 업무들을 마크다운 문서로 만들어 복사하거나 `.md` 파일로 저장
- 업무마다 프로젝트, 기간, 사용 기술, 성과(개선 전 → 후), 진행 과정(겪은 문제와 해결)을 담음

같은 진행 메모를 **날짜별**로 보면 "그날 무엇을 했는지"가 됩니다.
**업무별**로 보면 "이 업무를 어떤 과정으로 풀었는지"가 됩니다.

## 기술 스택

- Java 17, Spring Boot 4.1
- Spring Data JPA (Hibernate), Spring Data JPA Specification
- Spring Security + JWT (jjwt)
- PostgreSQL
- springdoc-openapi (Swagger UI)
- JUnit 5, AssertJ

## 실행 방법

### 1. 준비

- JDK 17 이상, PostgreSQL
- DB 두 개 생성: 실제 사용 `work_log`, 테스트 전용 `work_log_test`

```sql
CREATE DATABASE work_log;
CREATE DATABASE work_log_test;
```

### 2. 테이블 만들기

`ddl-auto`가 `validate`라서 테이블은 직접 만들어야 합니다.
10개 테이블을 만드는 스크립트가 [`src/test/resources/schema.sql`](src/test/resources/schema.sql)에 있습니다.
**빈 `work_log` DB에서** 이 파일 내용을 실행하세요.

> 이 스크립트는 맨 앞에서 기존 테이블을 `DROP`합니다.
> 데이터가 있는 DB에서는 실행하지 마세요.

### 3. 환경변수

- `DB_PASSWORD`: PostgreSQL 비밀번호 (사용자는 `postgres`)
- `JWT_SECRET`: JWT 서명 키 (32바이트 이상의 임의 문자열)

### 4. 실행

```bash
./mvnw spring-boot:run
```

- 서버: `http://localhost:8080`
- **Swagger UI: `http://localhost:8080/swagger-ui.html`**
  1. `POST /api/auth/signup`으로 가입한 뒤 `POST /api/auth/login`으로 토큰을 받습니다.
  2. 오른쪽 위 **Authorize**에 토큰을 입력합니다.
  3. 이후 모든 API를 화면에서 바로 호출할 수 있습니다.

## 테스트

실제 PostgreSQL의 테스트 전용 DB(`work_log_test`)에서 서비스 계층 테스트를 실행합니다.
테스트가 시작될 때마다 `schema.sql`로 테이블을 새로 만들고, 각 테스트 전에 데이터를 비웁니다.

```bash
# DB_PASSWORD 환경변수를 설정한 터미널에서
./mvnw test
```

H2 같은 메모리 DB 대신 실제 PostgreSQL을 쓴 이유는 다음과 같습니다.
이 프로젝트에서 잡고 싶은 문제가 DB 제약과 JPA 동작(아래 "겪은 문제")에 있기 때문입니다.

## 데이터 구조

모든 테이블은 `id`, `created_at`, `updated_at`(JPA Auditing)을 공통으로 가집니다.

**`member`**

- 회원
- 모든 데이터의 주인

**`project`**

- 프로젝트
- 보관 처리: `archived_at`

**`task`**

- 업무
- 상태, 우선순위, 마감일, 완료일
- 보관 처리: `archived_at`

**`task_result`**

- 성과 항목
- 지표명, 개선 전, 개선 후

**`tag`, `task_tag`**

- 기술 태그
- 업무와 태그를 잇는 연결 테이블

**`work_system`, `task_work_system`**

- 업무 시스템
- 업무와 업무 시스템을 잇는 연결 테이블

**`daily_log`**

- 일일 기록
- 회원당 하루 하나 (`(member_id, log_date)` 유니크)

**`task_log`**

- 진행 메모
- `daily_log`와 `task`를 잇는 테이블

설계 결정:

- 다대다 관계(업무↔태그, 업무↔시스템)는 `@ManyToMany`를 쓰지 않고 **중간 엔티티**로 풀었습니다.
- 연관관계는 모두 지연 로딩(LAZY), 다대일 단방향이 기본입니다.
- 업무·프로젝트는 삭제하지 않고 **보관 처리**합니다.
  진행 메모, 성과, 태그 연결은 남고 목록에서만 숨깁니다.

## API

공통 규칙:

- 가입·로그인을 제외한 모든 API는 `Authorization: Bearer <토큰>` 헤더가 필요합니다. (토큰 만료 2시간)
- 에러 응답은 `{ "code": "...", "message": "..." }` 형식으로 통일합니다.
  검증 실패 400, 미인증 401, 다른 회원의 데이터 403, 없는 데이터 404, 중복 409, 그 외 500입니다.
- 목록 조회는 `page`, `size`, `sort`를 받습니다. (기본 20개)
- 없는 주소는 404, 지원하지 않는 요청 방식은 405, 깨진 JSON·잘못된 값은 400으로 응답합니다.

**인증**

- `POST /api/auth/signup` 회원가입
- `POST /api/auth/login` 로그인 (토큰 발급)
- `GET /api/members/me` 내 정보

**프로젝트**

- `GET /api/projects` 목록 (`archived=true`면 보관함)
- `POST /api/projects` 등록
- `GET /api/projects/{id}` 상세
- `PATCH /api/projects/{id}` 수정
- `DELETE /api/projects/{id}` 보관 처리
- `POST /api/projects/{id}/restore` 보관 복구
- `DELETE /api/projects/{id}/permanent` 영구 삭제 (보관한 프로젝트만, 업무가 남아 있으면 409)

**태그**

- `GET /api/tags` 목록
- `POST /api/tags` 등록
- `PATCH /api/tags/{id}` 수정
- `DELETE /api/tags/{id}` 삭제

**업무 시스템**

- `GET /api/systems` 목록
- `POST /api/systems` 등록
- `PATCH /api/systems/{id}` 수정
- `DELETE /api/systems/{id}` 삭제

**업무**

- `GET /api/tasks` 목록 (필터, 페이징, 정렬, `archived=true`면 보관함)
- `POST /api/tasks` 등록
- `GET /api/tasks/{id}` 상세
- `PATCH /api/tasks/{id}` 수정
- `PATCH /api/tasks/{id}/status` 상태 변경
- `DELETE /api/tasks/{id}` 보관 처리
- `POST /api/tasks/{id}/restore` 보관 복구 (프로젝트가 보관 중이면 409)
- `DELETE /api/tasks/{id}/permanent` 영구 삭제 (보관한 업무만, 성과·진행 메모도 함께)

**성과 항목**

- `GET /api/tasks/{taskId}/results` 목록
- `POST /api/tasks/{taskId}/results` 추가
- `PATCH /api/tasks/{taskId}/results/{resultId}` 수정
- `DELETE /api/tasks/{taskId}/results/{resultId}` 삭제

**일일 기록**

- `GET /api/daily-logs` 기간별 목록 (`from`, `to`, 페이징)
- `GET /api/daily-logs/{date}` 하루 일지 (회고 + 그날의 진행 메모)
- `PUT /api/daily-logs/{date}` 회고 저장 (없으면 생성, 있으면 수정)

**진행 메모**

- `POST /api/daily-logs/{date}/task-logs` 등록
- `PATCH /api/task-logs/{id}` 수정
- `DELETE /api/task-logs/{id}` 삭제
- `GET /api/tasks/{taskId}/task-logs` 업무별 조회 (시간순)

**내보내기**

- `GET /api/exports/resume?taskIds=1&taskIds=2` 고른 업무들을 이력서용 마크다운으로 (`{"markdown": "..."}`)

업무 목록(`GET /api/tasks`)은 아래 필터를 자유롭게 조합할 수 있습니다.
`status`, `priority`, `projectId`, `systemId`, `tagId`, `dueDateFrom`, `dueDateTo`, `keyword`

## 기술적으로 고민한 점과 겪은 문제

### 1. 조건 조합 조회: Specification과 서브쿼리

필터가 7개이고 "있는 것만" 걸어야 해서, 메서드 이름 기반 쿼리(`findByXxx`)로는 표현할 수 없었습니다.
새 의존성이 필요 없는 **Specification**을 골랐습니다.
QueryDSL은 빌드 설정이 무겁고, 문자열 JPQL 조립은 조건이 늘수록 지저분해져서 제외했습니다.

업무가 태그·시스템을 직접 모르는 **단방향 설계**라서, 이 두 조건은 **서브쿼리**(`task.id IN (SELECT ...)`)로 걸렀습니다.

### 2. N+1 개선

업무 목록에서 업무마다 태그·시스템을 따로 조회해서 쿼리가 `1 + 2N`개 나갔습니다.
이번 페이지 업무 id 목록으로 `findByTaskIdIn`을 **한 번씩만** 호출하고, `groupingBy`로 업무별로 묶어 메모리에서 매칭했습니다.
그 결과 업무 수와 상관없이 고정 3개 쿼리(목록 + 태그 + 시스템)로 줄었습니다.

### 3. Hibernate는 INSERT를 DELETE보다 먼저 실행한다

업무를 수정할 때 태그 연결을 "전부 삭제 후 다시 생성"하는 방식이었습니다.
기존과 같은 태그를 유지하면 `task_tag (task_id, tag_id)` 유니크 제약 위반이 났습니다.
같은 트랜잭션에서 Hibernate가 **코드 순서와 무관하게 INSERT를 DELETE보다 먼저** 실행하기 때문입니다.

삭제 직후 `flush()`로 먼저 반영해서 해결했습니다.
같은 상황을 **회귀 테스트**로 남겨 두었습니다.

### 4. `getMemberId()` 편의 메서드가 Spring Data 쿼리를 깨뜨림

엔티티에 만들어 둔 `getMemberId()` 때문에 문제가 생겼습니다.
`findByMemberIdAndLogDate`의 `MemberId`를 `member.id`로 쪼개지 않고, 존재하지 않는 `memberId` 속성으로 해석해 JPQL 오류가 났습니다.
연관관계 필드가 있는 엔티티에는 `getXxxId()` 같은 편의 메서드를 만들지 않는 것으로 정리했습니다.

### 5. 처리하지 못한 예외가 401로 보이던 문제

`GlobalExceptionHandler`가 일부 예외만 잡고 있었습니다.
그래서 그 밖의 예외(위 JPQL 오류, 필수 파라미터 누락 등)가 클라이언트에는 엉뚱하게 "인증 필요(401)"로 보여 원인을 찾기 어려웠습니다.
필수 파라미터 누락은 400, 나머지는 500으로 응답하도록 처리를 추가했습니다.

### 6. 테스트가 찾아낸 지연 로딩 문제

서비스 테스트를 쓰자 조회 메서드에서 `LazyInitializationException`이 발생했습니다.
실제 서버에서는 요청이 끝날 때까지 DB 세션을 열어 두는 OSIV 덕분에 드러나지 않던 약점이었습니다.
연관 데이터를 읽는 조회 메서드에 `@Transactional(readOnly = true)`를 붙여 해결했습니다.

테스트 클래스에는 일부러 `@Transactional`을 붙이지 않았습니다.
붙이면 테스트가 끝나며 롤백되어, 위 3번 같은 DB 제약 문제가 실제로 실행되지 않기 때문입니다.

## 앞으로 할 일

- Refresh Token (현재는 만료되면 다시 로그인)
- 알림(저녁 기록, 마감일)
- Docker, GitHub Actions
