# 업무 관리 & 커리어 기록 서비스 기획서

작성일: 2026-09-21

## 개요

업무를 관리하면서 기록을 쌓고, 나중에 이력서와 면접에서 꺼내 쓰는 개인용 웹 서비스를 만든다.

- **용도**: 직접 쓰는 업무 관리·기록·업무일지 도구이자 이직용 포트폴리오
- **기술 방향**: Java/Spring Boot 백엔드(JPA, JWT, PostgreSQL)와 React 화면
- **비용**: 이미 설치된 PostgreSQL과 무료 도구만 사용

## 문제 정의

시간이 지나면 내가 했던 일이 기억나지 않아서, 이력서를 쓸 때마다 처음부터 기억을 더듬어야 한다.

특히 기억이 나지 않아 아쉬웠던 것은 세 가지다.

- 어떤 기술을 썼는지
- 문제를 어떻게 풀었는지
- 결과와 개선 수치

이 세 가지는 이력서 문장의 재료(상황·과제, 행동, 결과)와 그대로 이어진다. 그래서 업무를 하는 동안 바로 이 항목들을 남기는 것이 서비스의 핵심이다.

## 사용 시나리오

퇴근 후 집에서 하루를 돌아보며 일일 기록과 업무별 진행 메모를 함께 남긴다.

1. 그날 날짜의 일일 기록을 열고 하루 회고를 한두 줄 적는다.
2. 오늘 손댄 업무를 골라 진행 메모(겪은 문제, 해결 방법)와 그날 쓴 소요 시간을 남긴다. 목록에 없는 업무는 그 자리에서 새로 등록한다.
3. 업무를 끝내면 상태를 완료로 바꾸고 성과(지표명, 개선 전, 개선 후)를 적는다.

같은 메모를 날짜별로 보면 "그날 무엇을 했는지", 업무별로 보면 "이 업무를 어떤 과정으로 풀었는지"가 된다. 이력서에는 업무별 흐름이 필요하다.

기록은 회사 밖에서 쓰므로, 회사 문서나 코드는 붙여 넣지 않고 기억 속의 기술·문제·해결·수치만 본인 말로 적는다.

## 기능 범위

첫 버전(MVP)은 로그인, 업무 관리, 기록, 조회까지이고, 알림과 내보내기는 그 뒤에 붙인다.

| 기능 | 내용 | 단계 |
| --- | --- | --- |
| 회원가입/로그인 | JWT 인증, 내 데이터만 조회·수정 | MVP |
| 프로젝트·업무 관리 | 상태(예정/진행/완료), 마감일, 우선순위, 보관 처리 | MVP |
| 기술 태그 | 업무에 사용한 기술을 태그로 분류 | MVP |
| 업무 시스템 | 업무를 시스템별로 분류하고, 한 업무에 여러 시스템 지정 | MVP |
| 성과 항목 | 지표명, 개선 전, 개선 후를 문자열로 입력 | MVP |
| 일일 기록·진행 메모 | 하루 회고와 업무별 문제·해결 메모, 소요 시간 | MVP |
| 조회 | 날짜별, 업무별, 상태별, 시스템별 조회와 페이징 | MVP |
| 보관 항목 복구 | 보관 처리한 업무·프로젝트 되살리기 | 이후 |
| 알림 | 저녁 기록 알림, 마감일 알림(스케줄러, 슬랙/디스코드 웹훅) | 이후 |
| 이력서용 내보내기 | 선택한 업무와 성과를 마크다운으로 출력 | 이후 |
| GitHub 연동 | 커밋·PR을 기록 후보로 제안 | 이후 |
| Docker·CI | 앱 컨테이너화, GitHub Actions 자동 빌드·테스트 | 이후 |

## 데이터 구조

엔티티는 10개이고, 업무(Task)를 중심으로 프로젝트·태그·성과·진행 메모가 연결된다. 아래는 초안이며 ERD 단계에서 확정한다.

| 엔티티 | 주요 필드 | 관계 |
| --- | --- | --- |
| Member | 이메일(중복 불가), 비밀번호, 닉네임 | 모든 데이터의 주인 |
| Project | 이름, 설명, 시작일, 종료일 | Member 1:N Project |
| Task | 제목, 설명, 상태, 우선순위, 마감일, 완료일 | Project 1:N Task |
| TaskResult | 지표명, 개선 전, 개선 후(모두 문자열) | Task 1:N TaskResult |
| Tag | 기술 이름(회원별 중복 불가) | Member 1:N Tag |
| TaskTag | 업무와 태그를 잇는 중간 엔티티 | Task N:M Tag를 1:N, N:1로 풀어서 연결 |
| DailyLog | 날짜, 하루 회고(회원당 하루 하나) | Member 1:N DailyLog |
| TaskLog | 진행 메모(겪은 문제, 해결 방법), 소요 시간(분) | DailyLog 1:N TaskLog, Task 1:N TaskLog |
| WorkSystem | 업무 시스템 이름(회원별 중복 불가), 설명 | Member 1:N WorkSystem |
| TaskWorkSystem | 업무와 업무 시스템을 잇는 중간 엔티티 | Task N:M WorkSystem을 1:N, N:1로 풀어서 연결 |

**설계 결정**

- **다대다는 중간 엔티티로 푼다.** 업무와 태그(TaskTag), 업무와 업무 시스템(TaskWorkSystem)의 관계를 @ManyToMany로 바로 잇지 않고 중간 엔티티로 분리한다.
- **TaskLog가 일일 기록과 업무를 잇는다.** 같은 메모를 날짜별로도, 업무별로도 조회할 수 있다.
- **삭제는 보관 처리한다.** 업무를 삭제해도 진행 메모, 성과, 태그 연결은 남고 목록에서만 숨긴다. 프로젝트를 보관하면 하위 업무도 목록에서 함께 숨긴다.
- **성과는 문자열 항목형이다.** 수치가 아닌 성과(예: 수동 배포에서 자동 배포로)도 담을 수 있다. 개선율 자동 계산은 하지 않는다.

## ERD

테이블은 10개이고, 모든 테이블은 `id`(BIGINT, 자동 증가, 기본키)와 `created_at`, `updated_at`(TIMESTAMP, NOT NULL, JPA Auditing으로 자동 기록)을 공통으로 가진다. 아래 표는 그 외 컬럼과 제약 조건이다.

**공통 규칙**

- 테이블·컬럼 이름은 snake_case로 한다. 회원 테이블은 PostgreSQL 예약어 `user`를 피해 `member`로 하고, 업무 시스템은 자바 기본 클래스 `java.lang.System`과 이름이 겹치지 않도록 엔티티를 `WorkSystem`(테이블 `work_system`)으로 한다.
- 상태와 우선순위는 문자열로 저장한다. `status`는 TODO, IN_PROGRESS, DONE이고 `priority`는 HIGH, MEDIUM, LOW다.
- 보관 처리는 `archived_at`으로 한다. 비어 있으면 활성이고 값이 있으면 보관한 시각이다. 프로젝트와 업무에 적용한다.
- 연관관계는 모두 지연 로딩(LAZY)으로 하고, 다대일 단방향을 기본으로 한다.
- `task.member_id`는 프로젝트의 주인과 일치하는지 서비스에서 검증한다.

| 테이블 | 컬럼 | 타입 | 제약·설명 |
| --- | --- | --- | --- |
| `member` | `email` | VARCHAR(100) | NOT NULL, UNIQUE |
| `member` | `password` | VARCHAR(255) | NOT NULL, 암호화한 값만 저장 |
| `member` | `nickname` | VARCHAR(30) | NOT NULL |
| `project` | `member_id` | BIGINT | NOT NULL, FK → member |
| `project` | `name` | VARCHAR(100) | NOT NULL |
| `project` | `description` | TEXT | NULL 허용 |
| `project` | `start_date`, `end_date` | DATE | NULL 허용 |
| `project` | `archived_at` | TIMESTAMP | NULL이면 활성 |
| `task` | `member_id` | BIGINT | NOT NULL, FK → member |
| `task` | `project_id` | BIGINT | NOT NULL, FK → project |
| `task` | `title` | VARCHAR(200) | NOT NULL |
| `task` | `description` | TEXT | NULL 허용 |
| `task` | `status` | VARCHAR(20) | NOT NULL, 기본값 TODO |
| `task` | `priority` | VARCHAR(10) | NOT NULL, 기본값 MEDIUM |
| `task` | `due_date` | DATE | NULL 허용 |
| `task` | `completed_at` | TIMESTAMP | NULL 허용, 완료로 바꿀 때 기록 |
| `task` | `archived_at` | TIMESTAMP | NULL이면 활성 |
| `task_result` | `task_id` | BIGINT | NOT NULL, FK → task |
| `task_result` | `metric_name` | VARCHAR(100) | NOT NULL, 지표명 |
| `task_result` | `before_value` | VARCHAR(200) | NULL 허용, 개선 전 |
| `task_result` | `after_value` | VARCHAR(200) | NOT NULL, 개선 후 |
| `tag` | `member_id` | BIGINT | NOT NULL, FK → member |
| `tag` | `name` | VARCHAR(50) | NOT NULL, (member_id, name) UNIQUE |
| `task_tag` | `task_id` | BIGINT | NOT NULL, FK → task |
| `task_tag` | `tag_id` | BIGINT | NOT NULL, FK → tag, (task_id, tag_id) UNIQUE |
| `work_system` | `member_id` | BIGINT | NOT NULL, FK → member |
| `work_system` | `name` | VARCHAR(100) | NOT NULL, (member_id, name) UNIQUE |
| `work_system` | `description` | TEXT | NULL 허용 |
| `task_work_system` | `task_id` | BIGINT | NOT NULL, FK → task |
| `task_work_system` | `work_system_id` | BIGINT | NOT NULL, FK → work_system, (task_id, work_system_id) UNIQUE |
| `daily_log` | `member_id` | BIGINT | NOT NULL, FK → member |
| `daily_log` | `log_date` | DATE | NOT NULL, (member_id, log_date) UNIQUE |
| `daily_log` | `summary` | TEXT | NULL 허용, 하루 회고 |
| `task_log` | `daily_log_id` | BIGINT | NOT NULL, FK → daily_log |
| `task_log` | `task_id` | BIGINT | NOT NULL, FK → task |
| `task_log` | `content` | TEXT | NOT NULL, 겪은 문제와 해결 방법 |
| `task_log` | `spent_minutes` | INTEGER | NULL 허용, 0 이상, 소요 시간(분) |

`task_log`에는 유니크 제약을 걸지 않는다. 같은 날 같은 업무에 진행 메모를 여러 개 남길 수 있고, 소요 시간은 일지 화면에서 업무별로 합산한다.

**인덱스**

- `task (member_id, status, due_date)`: 내 업무를 상태와 마감일로 조회할 때 쓴다.
- `task (project_id)`: 프로젝트별 업무 조회에 쓴다.
- `task_log (daily_log_id)`, `task_log (task_id)`: 날짜별, 업무별 진행 메모 조회에 쓴다.
- `task_tag (tag_id)`, `task_work_system (work_system_id)`: 태그별, 시스템별 업무 조회에 쓴다.
- 유니크 제약이 걸린 컬럼은 인덱스가 자동으로 생기므로 따로 만들지 않는다.

## 진행 방식과 일정

백엔드와 React를 기능 단위로 함께 만들고, 가장 어려운 인증부터 시작해 약 2~3주를 예상한다. 하루 4~5시간 이상 쓸 수 있다는 전제이며, 태그·성과·보관 처리가 더해져 처음 계획보다 길어졌다.

| 순서 | 작업 | 기간 |
| --- | --- | --- |
| 0 | 프로젝트 세팅(Spring, React), DB 생성, 엔티티·ERD 설계 | 1일 |
| 1 | 회원가입/로그인(JWT)과 로그인 화면, CORS, 토큰 저장 방식 | 2~3일 |
| 2 | 프로젝트·업무·태그·성과·시스템 CRUD와 화면 | 4~5일 |
| 3 | 일일 기록과 업무별 진행 메모, 화면 | 2~3일 |
| 4 | 날짜별·업무별·상태별·시스템별 조회, 페이징, N+1 개선과 화면 | 2일 |
| 5 | 테스트 코드, Swagger 정리, README | 1~2일 |

**인증을 먼저 하는 이유**: 모든 기능이 "내 데이터만 조회"에 의존하므로, 나중에 붙이면 전부 수정해야 한다.

**진행 규칙**

- 각 기능은 API를 먼저 완성하고 Swagger로 검증한 뒤 화면을 붙인다.
- 에러가 나면 브라우저 개발자도구의 Network 탭에서 요청과 응답부터 확인해 프론트와 백엔드 문제를 가른다.
- 화면은 기능 위주로만 만들고 디자인은 마지막에 다듬는다.
- 백엔드 핵심(엔티티, 서비스, Security 설정)은 직접 작성하고 리뷰를 받는다. React 화면은 초안을 더 많이 받아도 된다.
- 시간이 밀리면 태그와 성과 화면을 뒤로 미룰 수 있도록 순서를 유연하게 둔다.

## API 목록

API는 7개 리소스, 32개 엔드포인트로 구성하고, 인증은 Access Token 하나로 처리한다.

**공통 규칙**

- 경로는 자원 중심으로 잡는다. 조회는 GET, 등록은 POST, 수정은 PATCH, 삭제는 DELETE이며, 업무·프로젝트의 DELETE는 보관 처리로 동작한다.
- 로그인 후 받은 Access Token을 `Authorization: Bearer` 헤더로 보낸다. 가입과 로그인을 제외한 모든 API가 대상이다. 토큰이 만료되면 다시 로그인하고, Refresh Token은 첫 버전 이후에 붙인다.
- 업무를 등록·수정할 때 `projectId`, `tagIds`, `systemIds`를 함께 받는다. TaskTag와 TaskWorkSystem은 서버 내부에서만 다룬다.
- 일일 기록은 날짜(`2026-09-21` 형식)로 접근한다. 회원당 하루에 하나라서 날짜가 곧 식별자다.
- 목록 조회는 `page`, `size`, `sort`를 받고, 업무 목록은 상태·우선순위·프로젝트·시스템·태그·마감일 범위·키워드 필터를 조합할 수 있다.
- 에러 응답은 `code`와 `message`를 담는 하나의 형식으로 통일한다. 검증 실패는 400, 미인증은 401, 다른 회원의 데이터 접근은 403, 없는 데이터는 404, 중복은 409로 응답한다.

| 구분 | 메서드·경로 | 설명 |
| --- | --- | --- |
| 인증 | POST /api/auth/signup | 회원가입 |
| 인증 | POST /api/auth/login | 로그인, Access Token 발급 |
| 인증 | GET /api/members/me | 내 정보 조회 |
| 프로젝트 | GET /api/projects | 프로젝트 목록(보관 제외) |
| 프로젝트 | POST /api/projects | 프로젝트 등록 |
| 프로젝트 | GET /api/projects/{id} | 프로젝트 상세 |
| 프로젝트 | PATCH /api/projects/{id} | 프로젝트 수정 |
| 프로젝트 | DELETE /api/projects/{id} | 보관 처리(하위 업무도 목록에서 숨김) |
| 업무 시스템 | GET /api/systems | 시스템 목록 |
| 업무 시스템 | POST /api/systems | 시스템 등록 |
| 업무 시스템 | PATCH /api/systems/{id} | 시스템 수정 |
| 업무 시스템 | DELETE /api/systems/{id} | 시스템 삭제 |
| 태그 | GET /api/tags | 태그 목록 |
| 태그 | POST /api/tags | 태그 등록 |
| 태그 | PATCH /api/tags/{id} | 태그 수정 |
| 태그 | DELETE /api/tags/{id} | 태그 삭제 |
| 업무 | GET /api/tasks | 업무 목록(필터, 페이징, 정렬) |
| 업무 | POST /api/tasks | 업무 등록(프로젝트, 태그, 시스템 포함) |
| 업무 | GET /api/tasks/{id} | 업무 상세(태그, 시스템, 성과 항목, 진행 메모 포함) |
| 업무 | PATCH /api/tasks/{id} | 업무 수정 |
| 업무 | PATCH /api/tasks/{id}/status | 상태 변경(완료로 바꾸면 완료일 기록) |
| 업무 | DELETE /api/tasks/{id} | 보관 처리 |
| 성과 항목 | GET /api/tasks/{taskId}/results | 성과 항목 목록 조회(2026-09-23, 기획서 초안엔 없던 것을 화면에서 필요해 추가) |
| 성과 항목 | POST /api/tasks/{taskId}/results | 성과 항목 추가(지표명, 개선 전, 개선 후) |
| 성과 항목 | PATCH /api/tasks/{taskId}/results/{resultId} | 성과 항목 수정 |
| 성과 항목 | DELETE /api/tasks/{taskId}/results/{resultId} | 성과 항목 삭제 |
| 일일 기록 | GET /api/daily-logs | 기간별 일일 기록 목록(from, to, 페이징) |
| 일일 기록 | GET /api/daily-logs/{date} | 하루 일지 조회(회고와 그날의 진행 메모) |
| 일일 기록 | PUT /api/daily-logs/{date} | 하루 회고 저장(없으면 생성, 있으면 수정) |
| 진행 메모 | POST /api/daily-logs/{date}/task-logs | 진행 메모 등록(업무, 내용, 소요 시간) |
| 진행 메모 | PATCH /api/task-logs/{id} | 진행 메모 수정 |
| 진행 메모 | DELETE /api/task-logs/{id} | 진행 메모 삭제 |
| 진행 메모 | GET /api/tasks/{taskId}/task-logs | 업무별 진행 메모 조회(시간순) |

**아직 정하지 않은 것**

- 태그·시스템·성과 항목·진행 메모를 삭제할 때의 처리(완전 삭제 또는 보관, 이미 연결된 업무가 있을 때의 동작)
- 조건 조합 조회의 구현 방식(QueryDSL, Specification, JPQL 중 선택)
- 토큰 저장 위치(localStorage 또는 httpOnly 쿠키)와 만료 시간

## 주의사항과 다음 단계

회사 기밀은 기록하지 않고, 다음 단계로 ERD 확정과 API 세부 설계를 진행한다.

**주의사항**

- 고객 정보, 내부 시스템 주소, 비공개 수치, 회사 문서와 코드는 적지 않는다. 기술, 문제, 해결, 수치를 본인 말로 요약하는 수준으로만 쓴다.
- 데이터는 로컬 PostgreSQL에만 두고 공개 배포하지 않는다. 포트폴리오 시연에는 가상 데이터를 쓰고, 업무 시스템 이름도 가상 이름이나 일반화한 이름으로 바꾼다.
- 회사에서 만들거나 쓰는 시스템의 코드·화면·양식은 이 프로젝트에 가져오지 않는다.

**다음 단계**

1. API 세부 설계: 위 목록의 요청·응답 필드와 에러 코드
2. ERD 확정: 위 엔티티 초안의 필드 타입과 제약 조건
3. 프로젝트 세팅과 인증 기능부터 구현 시작

---

## 진행 상황 (이 섹션은 작업하며 계속 갱신)

### 진행 방식
- 백엔드 핵심(엔티티, 서비스, Security 설정)은 사용자가 직접 코드를 치고, Claude는 소스와 설명을 먼저 보여준 뒤 사용자가 "다 썼어"라고 하면 파일을 읽고 `./mvnw -q compile`로 컴파일을 확인해 리뷰한다. (사용자가 직접 "너가 수정해줘"라고 요청하면 그때는 Claude가 Edit/Write로 직접 고친다.)
- 수정·삭제·추가(새 파일·의존성·설정 포함)는 먼저 무엇을 어떻게 바꿀지 말하고, 사용자가 하라고 해야 실행한다. 읽기·조회는 바로 해도 된다.
- 앱은 STS(Spring Tools for Eclipse)에서 실행한다. `DB_PASSWORD`, `JWT_SECRET`은 STS 실행 설정(Run Configurations → Environment)에 있고 시스템 환경변수에는 없다. 콘솔 로그는 STS 안에만 보이므로, 동작 확인은 8080 포트 프로세스 시작 시각과 클래스 컴파일 시각을 비교해 재시작 여부를 먼저 확인한 뒤 curl로 요청을 보내는 방식으로 한다.
- Lombok은 쓰지 않는다. DTO는 Java record로 만든다.
- 패키지는 도메인별로 구성한다(`common`, `config`, `member`, `auth`, `security`, 하위 `dto`, `common/exception`).
- `member` 테이블은 사용자가 SQL로 직접 만들었고, `application.yml`의 `ddl-auto`는 `validate`다(엔티티와 테이블이 맞는지 검사만 하고 자동으로 만들지 않음).
- 이 프로젝트는 `spring-boot-starter-webmvc`를 쓰는 Spring Boot 4 구성이라 **Jackson(`ObjectMapper`)이 기본 포함되지 않는다.** 필터 단계 등 Jackson이 필요해 보이는 곳에서는 라이브러리를 추가하기보다 간단한 문자열 조립으로 대체했다(예: `JwtAuthenticationEntryPoint`).
- GitHub 저장소: https://github.com/jh-st97/work-log (Public). STS(EGit)에서 Team → Commit, Team → Push to Upstream(원격 `origin`으로 고정 저장됨)으로 관리한다. 원격 연결 후 최초 1회는 "Push Branch main" 화면에서 URI·계정·토큰을 입력해 `origin`으로 저장해 둬야 다음부터 재입력 없이 push된다.
- **저장소 이름을 `work-log`에서 `work-log-backend`로 바꿀 예정(2026-09-22 결정, 아직 실행 안 함).** 바꾸면 이 문서의 주소와 로컬 STS의 원격 URL도 같이 고쳐야 한다.
- 프론트엔드 프로젝트가 별도로 생겼다: `C:\workspace\side-projects\work-log\work-log-frontend` (React + TypeScript + Vite). 자세한 내용은 그 프로젝트의 CLAUDE.md 참고. GitHub: https://github.com/jh-st97/work-log-frontend

### 완료 (2026-09-21~22, 기획서 1단계 대부분)
- `BaseEntity`(id, createdAt, updatedAt + JPA Auditing), `Member` 엔티티, `MemberRepository`(existsByEmail, findByEmail)
- `SecurityConfig`: signup·login만 permitAll, 나머지는 401, STATELESS, csrf 비활성, BCrypt `PasswordEncoder` Bean
- 회원가입 API `POST /api/auth/signup`: `SignupRequest`(record, 검증), 이메일 중복 시 409, 응답 `SignupResponse`(id, email, nickname만, 비밀번호 제외)
- 공통 에러 응답: `ErrorCode`(enum), `BusinessException`, `ErrorResponse`(code, message), `GlobalExceptionHandler`(`@RestControllerAdvice`)
- JWT 발급·검증: `JwtTokenProvider`(`jjwt` 0.12.6, HS256, `jwt.secret`은 `JWT_SECRET` 환경변수)
- 로그인 API `POST /api/auth/login`: `LoginRequest`/`LoginResponse`(record), 이메일 없음과 비밀번호 틀림을 구분 없이 401 `LOGIN_FAILED`로 응답(보안 관례)
- `JwtAuthenticationFilter`(`OncePerRequestFilter` 상속, `Authorization: Bearer` 헤더 해석 후 `SecurityContextHolder`에 회원 번호 등록), `SecurityConfig`에 `addFilterBefore`로 등록
- `GET /api/members/me`: `@AuthenticationPrincipal Long memberId`로 인증된 회원 조회, 없으면 404 `MEMBER_NOT_FOUND`
- `JwtAuthenticationEntryPoint`: 미인증(401) 응답도 `{code, message}` JSON 형식으로 통일 (Jackson 없이 문자열 직접 조립)
- 실제 요청으로 회원가입(201/409/400), 로그인(200/401), 인증 필터(토큰 없음·가짜 토큰 401, 유효한 토큰 200) 전부 확인 완료
- DB에 시험용 회원 `test@example.com`(id 1, 비밀번호 `test-password-1234`)이 남아 있음
- GitHub에 push 완료 (커밋: Initial commit, JWT 로그인 인증 구현 ×2)

### 1단계 완료
CORS까지 포함해 기획서 1단계(회원가입, 로그인, JWT 인증, 에러 응답 통일, CORS)가 전부 끝났다. `SecurityConfig`에 `.cors(cors -> cors.configurationSource(corsConfigurationSource()))` 추가, `CorsConfigurationSource` Bean에서 `http://localhost:5173`(Vite 기본 포트) 허용, 메서드는 `GET/POST/PATCH/PUT/DELETE`(PATCH 포함 필수), 헤더는 `*`. curl로는 브라우저 CORS 차단을 재현할 수 없어 preflight(OPTIONS + Origin 헤더)를 흉내 내 확인했다. 실제 검증은 React가 생긴 뒤에.

### 완료 (2026-09-22, 기획서 2단계 진행 중)
- `Project` 엔티티(`@ManyToOne Member`, LAZY), `ProjectRepository`(findByMemberIdAndArchivedAtIsNull, findByIdAndMemberId), `ProjectRequest`/`ProjectResponse`(record)
- `ProjectService`/`ProjectController`: 목록(보관 제외)/등록/상세/수정(더티 체킹, save 호출 안 함)/보관 처리(DELETE가 실제로는 archive) — `findMyProject`로 소유권 확인 공통화(내 것 아니면 403)
- `Tag` 엔티티(보관 개념 없음, `rename()`만 있음), `TagRepository`(findByMemberId, findByIdAndMemberId, existsByMemberIdAndName, existsByMemberIdAndNameAndIdNot), `TagRequest`/`TagResponse`
- `TagService`/`TagController`: 목록/등록(이름 중복 409)/수정(자기 자신 제외 중복 확인)/삭제(진짜 DELETE, 보관 아님) — `findByIdAndMemberId`로 소유권까지 한 번에 확인(TAG_ACCESS_DENIED는 정의만 해두고 미사용)
- `ErrorCode`에 `PROJECT_NOT_FOUND`, `PROJECT_ACCESS_DENIED`, `TAG_NOT_FOUND`, `TAG_ACCESS_DENIED`, `TAG_DUPLICATED` 추가
- `WorkSystem` 엔티티(Tag와 동일 패턴 + description 필드), `WorkSystemRepository`, `WorkSystemRequest`/`WorkSystemResponse`, `WorkSystemService`/`WorkSystemController` — `GET/POST /api/systems`, `PATCH/DELETE /api/systems/{id}`
- `ErrorCode`에 `PROJECT_NOT_FOUND`, `PROJECT_ACCESS_DENIED`, `TAG_NOT_FOUND`, `TAG_ACCESS_DENIED`, `TAG_DUPLICATED`, `WORK_SYSTEM_NOT_FOUND`, `WORK_SYSTEM_ACCESS_DENIED`, `WORK_SYSTEM_DUPLICATED` 추가
- 프로젝트·태그·업무 시스템 CRUD 전부 실제 요청으로 검증 완료(등록/중복/수정/삭제/404/403/400)
- `project`, `tag`, `work_system` 테이블은 사용자가 SQL로 직접 생성(`UNIQUE` 제약은 별도 인덱스 생성 안 함 — 기획서 규칙)
- **[테스트 방법, 2026-09-23 재확인·최종 결론] curl로 한글이 포함된 body를 Windows 명령줄 인자(`-d '...'`)로 직접 넘기면 인코딩이 깨져서 401(`UNAUTHORIZED`)이 난다. 이건 확정된 사실이다.** (9/22에 이 결론을 냈다가, 9/23 초반에 "사실 무작위 현상이었다"고 잘못 정정했었는데, 같은 날 늦게 직접 방식 5회·파일 방식 5회를 같은 내용·같은 토큰으로 바로 이어서 비교하는 통제된 테스트로 **직접 방식 5/5 실패, 파일 방식 5/5 성공**을 확인해서 원래(9/22) 결론이 맞았음을 재확인함. 사이에 있었던 "무작위" 관찰은 여러 테스트를 순서 없이 섞어서 하다가 우연히 ASCII 테스트와 한글 테스트 결과를 같은 원인으로 착각한 것.) **한글이 든 body는 무조건 JSON을 UTF-8 파일로 써서(Write 도구) `curl --data-binary "@파일경로"`로 보낸다. 이게 습관이 아니라 필수다 — 직접 `-d`로 한글 보내는 건 매번 실패한다고 가정할 것.**
- **[별개 발견, 사소함, 2026-09-23] 존재하지 않는 (메서드+경로) 조합에 요청하면 404/405 대신 401이 나온다** (예: `/api/totally-made-up`, 또는 GET만 있는 `/api/members/me`에 POST). 인증된 상태에서도 그렇다. 원인 미조사. 프론트엔드는 항상 실제 존재하는 엔드포인트만 호출하므로 실사용에는 영향 없음 — 나중에 여유 있을 때 원인을 봐도 되는 낮은 우선순위 항목.
- DB에 시험용 데이터가 남아 있음: `member` id 1(test@example.com), `work_system`에 `Payment System`(id 1), `test123`(id 2) 등. 태그·프로젝트도 시험 데이터가 조금 섞여 있을 수 있어 다음 세션에서 필요하면 정리.

### 완료 (2026-09-23, 기획서 2단계 마무리)
- `Task` 엔티티(`member`, `project` 둘 다 `@ManyToOne` LAZY), `TaskStatus`/`TaskPriority` enum(`@Enumerated(EnumType.STRING)`으로 DB엔 문자열 저장), 상태·우선순위 기본값(TODO/MEDIUM), `update()`(상태 제외 일반 수정), `changeStatus()`(DONE이면 completedAt 기록, 아니면 null로 초기화 — 기획서에 없어 직접 정한 부분), `archive()`
- `TaskTag`, `TaskWorkSystem` 중간 엔티티(다대다를 푸는 연결 테이블, update 메서드 없음 — 연결은 지우고 다시 만드는 방식)
- `TaskRepository`(findByMemberIdAndArchivedAtIsNull, findByIdAndMemberId — 상태·우선순위·마감일 등 조건 조합 검색은 아직 안 넣음, 4단계로 미룸), `TaskTagRepository`/`TaskWorkSystemRepository`(findByTaskId, deleteByTaskId)
- DTO: `TaskRequest`(title, description, priority, dueDate, projectId, tagIds, systemIds), `TaskStatusRequest`(status만), `TaskResponse`(태그·시스템은 `TagResponse`/`WorkSystemResponse` 재사용, projectId만 담고 projectName은 안 담음 — LAZY 추가 조회 피하려고)
- `TaskService`: 등록/수정 시 프로젝트·태그·업무시스템이 전부 내 것인지 검증(`findMyProject`, `resolveTags`, `resolveWorkSystems`), 수정 시 기존 태그·시스템 연결을 `deleteByTaskId`로 지우고 새로 연결, 조회는 업무마다 태그·시스템을 따로 조회함(N+1 있음, 4단계에서 개선 예정)
- `TaskController`: `GET/POST /api/tasks`, `GET/PATCH/DELETE /api/tasks/{id}`, `PATCH /api/tasks/{id}/status`
- `ErrorCode`에 `TASK_NOT_FOUND`, `TASK_ACCESS_DENIED` 추가(Task는 Project와 같은 패턴 — findById 후 소유자 직접 비교해서 403/404 구분)
- 등록/조회/수정/상태변경(완료↔취소)/보관/검증실패/없는프로젝트·태그로 등록시도/없는업무 조회 전부 실제 요청으로 검증 완료. 프로젝트·태그·업무시스템 기존 기능도 전체 회귀 테스트로 재확인함 — 전부 정상.
- `task`, `task_tag`, `task_work_system` 테이블은 사용자가 SQL로 직접 생성
- `LEARNING_NOTES.md` 파일을 프로젝트 루트에 만들어서, 사용자가 헷갈려하는 개념 설명(enum, interface, @ManyToOne, @AuthenticationPrincipal vs @PathVariable, memberId 흐름 등)을 계속 쌓고 있다. 앞으로도 개념 질문이 나오면 설명 후 이 파일에 추가할지 물어볼 것.
- **사용자가 "주석좀 항상 달아줘"라고 요청함(2026-09-23) — 이후 모든 소스에 한글 주석을 기본으로 넣는다.**
- `TaskResult` 엔티티(보관 개념 없음, Tag와 같은 방식으로 진짜 삭제), `TaskResultRepository`(findByTaskId, findByIdAndTaskId), `TaskResultRequest`/`TaskResultResponse`
- `TaskResultService`/`TaskResultController`: 주소가 `/api/tasks/{taskId}/results`로 업무 밑에 걸려 있어서 소유권 확인이 2단계(업무가 내 것인지 → 성과 항목이 진짜 그 업무 것인지). `ErrorCode`에 `TASK_RESULT_NOT_FOUND` 추가
- 등록(개선 전 값 있음/없음 둘 다)/수정/삭제/없는 업무 밑에서 접근/없는 성과 항목 전부 실제 요청으로 검증 완료

**이걸로 기획서 2단계 백엔드(프로젝트·업무·태그·성과·시스템)는 전부 끝났다.** (성과 항목 화면은 아직 — 아래 "성과 항목 화면 + 2단계 최종 마무리" 참고)

### 버그 수정 (2026-09-23, Task 수정 시 중복 키 오류)
- **증상**: 프론트 화면(`TasksPage.tsx`)에서 업무를 수정하면(기존과 같은 태그·시스템을 그대로 유지한 채) `PATCH /api/tasks/{id}`가 401로 실패. curl로도 재현되고 앱을 재시작해도 계속 발생.
- **진짜 원인**: `TaskService.updateTask`가 태그·업무 시스템 연결을 "전부 삭제 후 새로 생성"하는 방식인데, Hibernate는 같은 트랜잭션 안에서 **INSERT를 DELETE보다 먼저** 실행한다(자바 코드에 적은 순서와 무관). 그래서 기존과 동일한 태그를 다시 연결하면 삭제가 반영되기 전에 같은 `(task_id, tag_id)`로 INSERT가 먼저 들어가 `task_tag_task_id_tag_id_key` 유니크 제약 위반이 났다. 콘솔 에러는 500(또는 그 여파로 401)으로 보였음.
- **수정**: `deleteByTaskId(id)` 호출 직후 `taskTagRepository.flush()` / `taskWorkSystemRepository.flush()`를 추가해서 삭제를 즉시 DB에 반영한 뒤 INSERT가 일어나게 함. [TaskService.java](src/main/java/com/worklog/task/TaskService.java)의 `updateTask` 메서드.
- **디버깅 중 별개로 드러난 것**: 위 수정 후에도 같은 curl 재현 시나리오가 401로 남아있었는데, 원인을 추적하려고 `JwtTokenProvider`/`JwtAuthenticationFilter`에 임시 디버그 로그를 넣어 확인한 결과 **Claude가 디버그 curl을 만들면서 한글(`"디버그 테스트"`)을 파일 없이 `-d`로 직접 보낸 것**이 원인이었다. 즉 278번 줄에 이미 적혀있던 "한글은 항상 파일로" 규칙을 Claude 스스로 어겨서 생긴, 새로운 버그가 아닌 반복 실수였다. UTF-8 파일 + `--data-binary`로 동일 시나리오를 10회 재시도해 10/10 성공(200)으로 확정. 임시 디버그 로그는 원인 확인 후 제거함.
- 커밋: `fix: Task 수정 시 태그·시스템 재연결 중복 키 오류 수정` (5b92771), push 완료.

### 완료 (2026-09-23, 성과 항목 화면 + 2단계 최종 마무리)
- 프론트엔드 `TasksPage.tsx`에 성과 항목(지표명·개선 전·개선 후) 목록·추가·수정·삭제 UI를 업무 수정 폼 안에 붙였다(수정 모드일 때만 노출 — 성과 항목은 이미 존재하는 업무에만 붙일 수 있어서).
- 화면에서 목록을 보여주려면 성과 항목 조회 API가 있어야 하는데 기획서 API 목록엔 없어서, `GET /api/tasks/{taskId}/results`를 새로 추가했다(`TaskResultRepository.findByTaskId`는 이미 있어서 Service·Controller에 얇게 얹기만 함). 위 API 목록 표에도 반영함.
- 등록/수정/삭제를 실제 브라우저에서 전부 확인. 삭제 확인창(`window.confirm`)은 Claude의 자동화 브라우저에서는 네이티브 다이얼로그라 직접 클릭으로 못 눌러서 `window.confirm`을 임시로 덮어써서 우회 확인함 — 실제 사용자 브라우저에서는 정상적으로 뜬다.
- `TaskResultController`/`TaskResultService`는 아직 커밋 전(다음에 커밋 예정).

**이걸로 기획서 2단계(프로젝트·업무·태그·성과·시스템)가 백엔드·화면 전부 완전히 끝났다.**

### 완료 (2026-09-23, 기획서 3단계 시작 — DailyLog)
- `DailyLog` 엔티티(`member` `@ManyToOne` LAZY, `logDate`, `summary` — Tag처럼 보관 개념 없음), `updateSummary()`만 있는 단일 필드 수정 패턴. 엔티티는 사용자가 직접 타이핑.
- `DailyLogRepository`: `findByMemberIdAndLogDate`(단일 날짜 조회 — GET 상세, PUT 저장 둘 다 여기서 씀), `findByMemberIdAndLogDateBetween`(기간 목록, `Pageable` 사용)
- DTO: `DailyLogRequest`(summary만, NULL 허용이라 검증 없음), `DailyLogResponse`(TaskLog 목록은 나중에 추가함 — 아래 TaskLog 항목 참고)
- `DailyLogService`/`DailyLogController`: `GET /api/daily-logs`(기간+페이징, `Pageable`을 이번에 처음 도입 — 기획서 4단계로 미루지 않고 바로 넣음, `@PageableDefault(size=20, sort="logDate", direction=DESC)`), `GET /api/daily-logs/{date}`(없으면 404), `PUT /api/daily-logs/{date}`(upsert — 있으면 수정, 없으면 새로 만듦, `findMyDailyLog(...).map(...).orElseGet(...)` 패턴). `ErrorCode`에 `DAILY_LOG_NOT_FOUND` 추가(접근 권한 에러는 안 둠 — memberId로 스코프된 조회라 남의 것에 우연히 접근할 방법이 없어서).
- `daily_log` 테이블은 사용자가 SQL로 직접 생성(`(member_id, log_date)` UNIQUE).
- **버그 발견·수정: `DailyLog.getMemberId()` 편의 메서드가 Spring Data 쿼리 생성을 망가뜨림.** `findByMemberIdAndLogDate`처럼 "MemberId"가 들어간 메서드명은 원래 Spring Data가 `member`(연관관계 필드) + `.id`로 쪼개서 이해해야 하는데, 엔티티에 `getMemberId()`(제가 만든 편의 메서드, `member.getId()`를 그냥 리턴)가 있으니 Spring Data가 이걸 진짜 필드처럼 착각해서 `d.memberId`라는 존재하지 않는 JPQL 경로를 그대로 만들어버렸다. 결과: 요청 시점에 `BadJpqlGrammarException` 발생 → 아래 서술할 이유로 클라이언트에는 엉뚱하게 401로 보임 → "왜 만든 지 얼마 안 된 엔드포인트가 다 401이지?"로 한참 헤맴. 다른 엔티티(Project/Task/Tag/WorkSystem)는 이런 `getXxxId()` 단축 메서드가 없어서 문제가 없었음. **교훈: 연관관계 필드가 있는 엔티티에 `getXxxId()` 같은 편의 메서드를 만들지 말 것** — Spring Data 쿼리 메서드 이름과 충돌한다. `getMemberId()` 삭제로 해결.
- **버그 발견·수정: 처리 안 된 예외가 401로 보이는 문제.** `GlobalExceptionHandler`가 원래 `BusinessException`과 `MethodArgumentNotValidException`만 잡고 있어서, 그 외의 예외(위 JPQL 오류, 필수 `@RequestParam` 누락 등)가 이 앱의 Security 설정 특성상 클라이언트에는 401(`인증이 필요합니다`)로 보였다 — 9/23 하루 종일 쫓아다닌 "원인 불명 401"들의 상당수가 사실 이거였음. `MissingServletRequestParameterException`을 400으로 따로 잡고(`GET /api/daily-logs`에서 `from`/`to` 빠뜨리면 이제 정확히 400), 나머지 전부를 잡는 `@ExceptionHandler(Exception.class)`로 500을 반환하도록 추가함(`ErrorCode.INTERNAL_SERVER_ERROR` 추가). 이후 "존재하지 않는 경로" 요청도 401 대신 500으로 바뀜 — 여전히 정확한 404는 아니지만 최소한 "인증 필요"라는 거짓 메시지는 안 뜸. **404까지 정확하게 만드는 건 보류함**(`spring.mvc.throw-exception-if-no-handler-found` 설정 + `NoHandlerFoundException` 처리 필요, 사소해서 나중으로 미룸).
- 생성/조회/없는 날짜(404)/기간 목록(페이징)/수정(upsert, 같은 id 유지)/파라미터 누락(400)/토큰 없음(401) 전부 실제 요청으로 검증 완료.
- 커밋: `feat: 일일 기록(DailyLog) 조회·저장 API 구현` (b1245f0), push 완료.

### 완료 (2026-09-23, 기획서 3단계 마무리 — TaskLog)
- `TaskLog` 엔티티(`dailyLog`·`task` 둘 다 `@ManyToOne` LAZY, `content`, `spentMinutes`). `getMemberId()` 같은 편의 메서드는 위에서 겪은 버그 때문에 안 만듦 — 실제로 필요하지도 않았음(연관관계는 `dailyLogId`/`taskId`처럼 FK 이름 그대로 조회하면 돼서 "MemberId" 같은 조합 자체가 안 나옴).
- `TaskLogRepository`: `findByDailyLogIdOrderByCreatedAtAsc`(하루 일지용), `findByTaskIdOrderByCreatedAtAsc`(업무별 조회용, 기획서에서 "시간순" 요구)
- DTO: `TaskLogRequest`(등록용, `taskId`·`content`·`spentMinutes`), `TaskLogUpdateRequest`(수정용, 업무는 등록 후 못 바꾸니 `taskId` 없음), `TaskLogResponse`(하루 일지 화면·업무별 화면 둘 다 재사용하려고 `taskId`/`taskTitle`/`logDate`를 전부 담음)
- **엔드포인트가 주소 3군데에 흩어져 있음**: `POST /api/daily-logs/{date}/task-logs`(등록), `PATCH·DELETE /api/task-logs/{id}`(수정·삭제), `GET /api/tasks/{taskId}/task-logs`(업무별 조회). 공통 prefix가 없어서 `TaskLogController`는 클래스 레벨 `@RequestMapping` 없이 메서드마다 전체 경로를 적음.
- **소유권 확인이 한 단계 더 깊음**: `PATCH`/`DELETE`는 경로에 날짜·업무 번호가 없어서, `findMyTaskLog()`가 `진행 메모 → 연결된 업무 → 업무의 주인` 순서로 확인함. `ErrorCode`에 `TASK_LOG_NOT_FOUND`, `TASK_LOG_ACCESS_DENIED` 추가.
- **등록 시 그 날짜의 일일 기록이 없으면 회고 없이(summary=null) 자동으로 먼저 만들어준다** — 회고보다 업무 메모를 먼저 남기는 흐름을 막지 않으려고 (사용자와 상의해서 정한 동작).
- `DailyLogResponse`에 `List<TaskLogResponse> taskLogs` 필드를 추가함. 기간 목록 조회(`GET /api/daily-logs`)는 비용 때문에 빈 목록(`DailyLogResponse.from`)만 주고, 하루 상세 조회(`GET /api/daily-logs/{date}`)와 PUT 저장 응답은 실제 진행 메모까지 채워서 준다(`DailyLogResponse.of`). `DailyLogService`가 `TaskLogRepository`를 새로 주입받음.
- `task_log` 테이블은 사용자가 SQL로 직접 생성 (유니크 제약 없음 — 같은 날 같은 업무에 메모 여러 개 가능, 기획서 규칙).
- 등록(날짜 자동 생성 포함)/업무별 조회/하루 일지 조회(진행 메모 포함)/수정/검증 실패(400)/없는 진행 메모(404)/삭제/삭제 후 재조회 전부 실제 요청으로 검증 완료.

**이걸로 기획서 3단계(DailyLog, TaskLog)가 백엔드·화면 전부 완전히 끝났다.** 프론트엔드 하루 일지 화면(`DailyLogPage`)은 `work-log-frontend`의 CLAUDE.md에 자세히 기록.

### 완료 (2026-09-27, 기획서 4단계 — 업무 목록 필터·페이징·N+1 개선)
- **조건 조합 조회 구현 방식: Specification으로 결정**(QueryDSL은 빌드 설정이 복잡하고 JPQL 직접 조립은 "일부 필터만 있을 때" 처리가 지저분해져서 제외). spring-data-jpa에 이미 포함돼 있어 새 의존성 없음.
- `TaskSpecification`(새 파일): 상태·우선순위·프로젝트·마감일 범위·키워드(제목/설명, 대소문자 무시)는 Task 필드에 바로 조건을 걸고, 항상 `member_id`와 `archivedAt IS NULL`을 적용. **태그·업무시스템은 Task가 직접 모르는 관계(단방향 설계)라 서브쿼리**(`task.id IN (SELECT tt.task.id FROM TaskTag tt WHERE tt.tag.id = ?)`)로 걸러냄. 파라미터가 null이면 그 조건은 건너뜀.
- `TaskRepository`에 `JpaSpecificationExecutor<Task>` 추가, 안 쓰게 된 `findByMemberIdAndArchivedAtIsNull`은 삭제.
- `GET /api/tasks`가 `List`가 아니라 `Page<TaskResponse>`를 반환하고, 쿼리 파라미터 `status`, `priority`, `projectId`, `systemId`, `tagId`, `dueDateFrom`, `dueDateTo`, `keyword` + `page`/`size`/`sort`를 받음(기본 `size=20`, `createdAt` 내림차순).
- **N+1 개선**: 목록 조회 시 업무마다 태그·시스템을 따로 조회하던 걸, 이번 페이지 업무 id들로 `findByTaskIdIn`을 한 번씩만 호출(`TaskTagRepository`, `TaskWorkSystemRepository`에 추가)하고 `Collectors.groupingBy`로 메모리에서 매칭. 업무 단건 조회(`getTask`)·상태 변경은 업무 1개라 기존 방식 유지. 빈 id 목록이면 쿼리를 아예 안 날림.
- 필터 각각·조합(AND)·페이징(`size=1`)·키워드(영문·한글)·없는 태그(빈 목록) 전부 실제 요청으로 검증 완료. 한글 키워드는 URL 퍼센트 인코딩(`%EB%94%94...`)으로 보내면 Git Bash 인코딩 문제를 피할 수 있음.
- 프론트엔드 필터 UI·페이지 이동은 `work-log-frontend`의 CLAUDE.md 참고.

### 완료 (2026-10-06, 기획서 5단계 — Swagger)
- `springdoc-openapi-starter-webmvc-ui` **3.1.1** 추가(`pom.xml`). Spring Boot 4.x는 springdoc **v3 시리즈**를 써야 하고, v2는 Spring Boot 3용이라 시작 때 에러가 남. 오픈소스(Apache 2.0)라 무료.
- `SecurityConfig`의 `permitAll()`에 `/swagger-ui/**`, `/swagger-ui.html`, `/v3/api-docs/**` 추가(화면·설명서만 열림, 실제 API는 여전히 토큰 필요 — 토큰 없이 `/api/tasks`는 401).
- `config/OpenApiConfig`(새 파일): 제목·설명 + `bearerAuth`(HTTP bearer JWT) 보안 스킴을 등록해서 Swagger 화면에 Authorize 버튼이 생기고 토큰을 한 번 넣으면 모든 호출에 자동으로 붙음. 사용자가 `SecurityConfig` 수정은 직접, `OpenApiConfig`도 직접 작성.
- 접속: `http://localhost:8080/swagger-ui.html`(→ `/swagger-ui/index.html`로 302). `/v3/api-docs`에 우리 API 21개 경로 확인. 로그인 → Authorize → `GET /api/tasks` 200까지 사용자가 직접 확인.
- **[함정] STS에서 `ClassNotFoundException: io.swagger.v3.oas.models.OpenAPI`**: `pom.xml`에 의존성을 추가했는데 `mvnw` 컴파일·`dependency:tree`는 정상이고 STS의 Maven Dependencies에도 jar가 보이는데 실행만 실패함. **Maven → Update Project만으로는 안 풀렸고, Project → Clean 후 재시작으로 해결.** 새 의존성을 추가하면 Update Project → Clean → 재시작 순서로 할 것.

### 완료 (2026-10-06, 기획서 5단계 — 테스트 코드 시작)
- **테스트용 DB는 H2가 아니라 같은 PostgreSQL에 `work_log_test` DB를 따로 만들어 사용**(사용자가 `CREATE DATABASE work_log_test;` 직접 실행). 이 프로젝트의 위험 지점이 DB 제약과 JPA 동작이라 실제와 같은 DB로 검증해야 의미가 있어서. Docker가 필요한 Testcontainers는 "이후" 단계로 미룸.
- `src/test/resources/schema.sql`: 10개 테이블을 ERD대로(유니크·FK·CHECK·인덱스 포함) `DROP → CREATE`. Hibernate가 자동 생성하게 두지 않은 이유는, 엔티티에 없는 DB 전용 제약(예: `task_tag (task_id, tag_id)` 유니크)이 있어야 실제 버그를 잡을 수 있어서. **테이블을 바꾸면 이 파일도 같이 고칠 것.**
- `src/test/resources/application.yml`: 테스트 때는 `src/main`의 설정 대신 이 파일이 **통째로** 쓰임(같은 이름이면 병합이 아니라 대체). `work_log_test`에 연결, `spring.sql.init.mode=always`로 매번 schema.sql 실행, `ddl-auto: validate`, 테스트용 JWT 키. DB 비밀번호는 `${DB_PASSWORD}`.
- **테스트는 STS에서 실행**(우클릭 → Run As → JUnit Test). `DB_PASSWORD`가 STS 실행 설정에만 있고 Claude 터미널엔 없어서 Claude는 `mvnw test`를 못 돌림 → 컴파일(`./mvnw -q test-compile`)까지만 확인하고, 실행 결과는 사용자에게 받음. **JUnit 실행 설정은 클래스마다 따로 생기므로 Run Configurations → JUnit → Environment 탭에 `DB_PASSWORD`를 넣어야 함.**
- `TaskServiceTest`(3개, 전부 통과): ① 같은 태그·시스템을 그대로 두고 업무 수정해도 중복 키 오류가 안 남(Hibernate flush 순서 버그 회귀 방지), ② 남의 업무 수정·조회는 `TASK_ACCESS_DENIED`, ③ 업무 목록 필터(우선순위·상태·태그·시스템·키워드·조합·없는 태그)와 남의 업무·보관 업무 제외, 태그·시스템이 정확히 붙는지.
- **테스트 설계 주의: 테스트 클래스에 `@Transactional`을 붙이지 않음.** 붙이면 테스트 전체가 하나의 트랜잭션이라 끝에 롤백되면서 INSERT/DELETE가 실제로 DB에 실행되지 않아 중복 키 같은 제약 문제를 못 잡음. 대신 `@BeforeEach`에서 `TRUNCATE TABLE member RESTART IDENTITY CASCADE`로 비움.
- **테스트가 찾아낸 서버 코드의 약점: 조회 메서드의 `LazyInitializationException`.** `TaskService.getTasks`/`getTask`가 트랜잭션 없이 LAZY 연관관계(`TaskTag.tag`)의 이름을 읽고 있었는데, 실제 서버에서는 OSIV(요청이 끝날 때까지 DB 세션을 열어둠) 덕분에 문제가 안 보였음. 웹 요청이 없는 테스트에서 처음 드러남. **두 메서드에 `@Transactional(readOnly = true)`를 붙여 해결.** 같은 구조(트랜잭션 없는 조회가 LAZY 연관관계를 읽음)가 `DailyLogService.getDailyLog`, `TaskLogService.getTaskLogsByTask`, `TaskResultService.getResults` 등에도 있을 수 있어 해당 서비스 테스트를 쓸 때 같이 확인할 것.
- Mockito 실행 시 "self-attaching to enable the inline-mock-maker" 경고는 JDK 21에서 나오는 안내일 뿐 실패 원인이 아님(지금은 무시).

### 완료 (2026-10-06, 기획서 5단계 — 나머지 서비스 테스트)
- 공통 베이스 `support/ServiceTestSupport`(`@SpringBootTest` abstract): 매 테스트 전에 `TRUNCATE ... CASCADE`로 비우고 `owner`, `other` 회원과 각자의 프로젝트(`project`, `otherProject`)를 준비, `createTask(title)` 헬퍼 제공. 새 서비스 테스트는 이걸 상속(`extends`)해서 쓸 것. (`TaskServiceTest`는 베이스 도입 전에 만들어서 자체 준비 코드를 가짐.)
- `DailyLogServiceTest`(4): 같은 날 두 번 저장은 upsert(행 1개 유지)·없는 날짜 404·다른 회원 일지와 분리·하루 일지에 진행 메모(업무 제목·날짜) 포함·기간 목록 범위+페이징(남의 일지 제외, 목록은 `taskLogs` 빈 배열).
- `TaskLogServiceTest`(7): 일일 기록 자동 생성(summary=null)·같은 날 메모 여러 개·남의 업무에 등록 차단(`TASK_ACCESS_DENIED`)·남의 메모 수정/삭제 차단(`TASK_LOG_ACCESS_DENIED`)·수정·삭제·없는 메모 404·업무별 시간순 조회.
- `TaskResultServiceTest`(3): CRUD(개선 전 null 허용)·남의 업무 차단·다른 업무의 성과 항목을 내 다른 업무 주소로 접근 시 `TASK_RESULT_NOT_FOUND`.
- **예측대로 `LazyInitializationException` 2건이 실제로 실패로 드러남** → `DailyLogService.getDailyLog`, `TaskLogService.getTaskLogsByTask`에 `@Transactional(readOnly = true)` 추가로 해결(TaskService 때와 같은 원인: 진행 메모 응답이 LAZY 연관관계 `task.title`, `dailyLog.logDate`를 읽음). **규칙: 응답을 만들 때 LAZY 연관관계를 읽는 조회 메서드에는 `@Transactional(readOnly = true)`를 붙인다.**
- `TagServiceTest`(1): 같은 이름 태그 중복 등록 시 `TAG_DUPLICATED`. **사용자가 뼈대(빈칸 채우기)를 받아서 직접 작성**(첫 테스트 작성 연습). 뼈대에 `fail(...)`을 넣어 "안 쓴 테스트가 초록색으로 보이는 오해"를 막는 방식을 씀.
- 테스트를 쓸지 판단하는 기준(사용자와 합의): "틀렸을 때 눈치채기 어렵고 피해가 큰가?" — 소유권·권한, 규칙이 있는 로직, DB 제약과 얽힌 곳, 버그가 났던 곳은 쓰고, 단순 CRUD는 생략. 지금 테스트가 없는 곳: 로그인·가입, 프로젝트·태그(중복 외)·업무 시스템 CRUD.
- 전체 테스트(서비스 15개)는 STS에서 사용자가 실행해 전부 통과 확인.

### 완료 (2026-10-06, 기획서 5단계 — README)
- 백엔드 `README.md` 신규 작성: 소개, 주요 기능, 기술 스택, 실행 방법(DB 2개 생성·`src/test/resources/schema.sql`로 테이블 생성·환경변수·Swagger), 테스트 방법, 데이터 구조, API, "기술적으로 고민한 점과 겪은 문제" 6가지(Specification+서브쿼리, N+1, flush 순서, `getMemberId()`, 401로 보이던 예외, 지연 로딩), 앞으로 할 일. 프론트엔드 `README.md`는 Vite 기본 문구를 지우고 새로 작성.
- **README 서식 규칙(사용자 요청): 표를 쓰지 않고 목록으로 쓰며, 문단은 문장마다 줄을 나눈다.** 사용자의 화면에서 표가 안 보이고, 표 한 칸에 설명이 길게 이어지는 걸 불편해함. README를 고칠 때도 이 방식을 유지할 것. (CLAUDE.md는 Claude가 읽는 파일이라 표를 써도 됨.)
- 스크린샷은 뺌(Claude 브라우저 캡처가 불안정). 나중에 직접 찍어서 `docs/`에 넣고 README에 추가할 수 있음.
- 미확인: README의 N+1 쿼리 개수(`1 + 2N` → 고정 3개)는 계산한 값이고 쿼리 로그로 직접 센 건 아님. 면접에서 숫자를 말할 거면 `show-sql` 로그로 확인할 것.
- 저장소 이름을 `work-log-backend`로 바꾸면 두 README의 링크(`github.com/jh-st97/work-log`)도 같이 고칠 것.

### 완료 (2026-10-06, 기획서 5단계 — 컨트롤러 통합 테스트)
- `support/ControllerTestSupport`(`@SpringBootTest` + `@AutoConfigureMockMvc`): `MockMvc`로 서버를 띄우지 않고 보안 필터(JWT)·컨트롤러·서비스·DB를 전부 거치는 요청을 보냄. 도우미 `signupAndLogin(email)`(토큰 반환), `createProject`, `createTask`, `bearer(token)`. Spring Boot 4라서 어노테이션 패키지가 `org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc`임(Boot 3과 다름). JSON 응답은 `com.jayway.jsonpath.JsonPath`로 읽음(Jackson `ObjectMapper` 안 씀). 요청 본문은 문자열로 직접 조립(ASCII만 써서 인코딩 문제 회피). 한글 메시지는 단언하지 않고 `code`만 확인.
- `AuthControllerTest`(7): 가입 201(응답에 비밀번호 없음)·이메일 중복 409 `EMAIL_DUPLICATED`·짧은 비밀번호 400 `INVALID_INPUT`·로그인 토큰 발급·틀린 비밀번호와 없는 이메일이 똑같이 401 `LOGIN_FAILED`·유효 토큰으로 `/api/members/me` 200·토큰 없음/가짜 토큰 401 `UNAUTHORIZED`.
- `TaskControllerTest`(8): 보호된 API 토큰 없이 401·Swagger `/v3/api-docs`는 열리지만 실제 API는 여전히 401·업무 생성 후 내 목록에 나옴·남의 업무는 목록에 안 나오고 상세·수정 403 `TASK_ACCESS_DENIED`·없는 업무 404·제목 없음/프로젝트 없음 400·`GET /api/daily-logs`에서 `from/to` 누락 시 400(401 아님)·남의 프로젝트에 업무 등록 403 `PROJECT_ACCESS_DENIED`.
- 전체 35개(서비스 + 컨트롤러 + 앱 기동 테스트) STS에서 통과.
- **테스트가 실제로 구멍을 잡는지 직접 실험함**: `TaskService.findMyTask`의 소유권 검사를 일부러 주석 처리 → `Failures: 2`로 실패 → 되돌리자 35/35 통과. 화면으로는 안 보이는 보안 구멍을 테스트가 잡아준다는 걸 사용자가 직접 확인.
- **[STS 함정] 코드를 바꿨는데 테스트 결과가 안 맞으면(구멍을 뚫었는데 초록색) Project → Clean 후 다시 돌릴 것.** 소스·`target/classes` 파일은 이미 새것인데도 STS가 옛 코드를 써서 초록색이 나온 적이 있음(`ClassNotFoundException` 때와 같은 계열). 테스트 결과를 믿기 전에, 의심스러우면 Clean 먼저.
- 주의: 테스트를 쓸 때 이 STS는 `target/test-classes` 갱신이 늦을 수 있음. 새 테스트 파일은 Refresh 후 실행.

**이걸로 기획서 5단계(테스트, Swagger, README)가 모두 끝났다.**

### 완료 (2026-10-06, 태그·업무 시스템 삭제 시 연결 처리)
- **결정(사용자): 업무에 붙은 태그/업무 시스템을 삭제하면 "연결만 끊고 삭제"**(업무 자체는 그대로). 대안이던 "막고 409로 안내"는 지우려면 업무에서 하나씩 먼저 떼야 해서 번거로워 제외. 태그는 단순한 꼬리표라 이게 가장 자연스러움.
- **원래 문제**: `TagService.deleteTag`/`WorkSystemService.deleteWorkSystem`이 연결 검사 없이 바로 삭제 → `task_tag.tag_id`·`task_work_system.work_system_id` FK 때문에 업무에 붙은 걸 지우면 DB가 거부해 500 에러.
- 구현: `TaskTagRepository.deleteByTagId`, `TaskWorkSystemRepository.deleteByWorkSystemId` 추가. 삭제 전에 연결을 먼저 지우고 `flush()`한 뒤 태그/시스템을 삭제(업무 수정 때처럼 삭제 순서를 확실히 하려고). `TagService`/`WorkSystemService`가 task 패키지 레포지토리를 주입받음.
- 테스트: `WorkSystemServiceTest`(새 파일, Claude 작성)와 `TagServiceTest`에 "붙어 있는 걸 삭제해도 연결만 끊기고 업무는 남는다" 추가(태그 쪽은 사용자가 뼈대를 채워 직접 작성). 전체 통과.
- 프론트: `TagsPage`·`WorkSystemsPage`의 삭제 확인창 문구에 "붙은 업무에서도 빠집니다(업무 자체는 그대로)"를 추가.
- 성과 항목·진행 메모 삭제 처리는 기획서의 "아직 정하지 않은 것"이었지만, 둘 다 진짜 삭제이고 다른 테이블이 이들을 참조하지 않아 문제 없음.

### 완료 (2026-10-06, 이력서용 마크다운 내보내기 — 기획서 "이후" 기능)
- **결정(사용자)**: 담는 내용은 업무+기술+성과+진행 메모 전부, 업무는 목록에서 체크박스로 직접 선택.
- API: `GET /api/exports/resume?taskIds=1&taskIds=2` → `{ "markdown": "..." }`. 프론트 `apiFetch`가 JSON만 받아서 마크다운을 JSON으로 감쌈. `resume` 패키지(`ResumeExportController`, `ResumeExportService`, `dto/ResumeExportResponse`).
- 문서 구성: `# 업무 경력 정리` 아래 업무마다 `## 제목` + 프로젝트·기간(등록일 ~ 완료일, 없으면 "진행 중")·상태·사용 기술(태그)·업무 시스템(비어 있으면 줄 생략) + 설명 + `### 성과`(개선 전 있으면 "전 → 후", 없으면 후만) + `### 진행 과정`(날짜·소요 시간·메모, 여러 줄 메모는 들여쓰기로 이어붙임). 비어 있는 소제목은 만들지 않음.
- 선택한 업무는 전부 내 것이어야 함(남의 것이 섞이면 403 `TASK_ACCESS_DENIED`), 같은 업무를 두 번 골라도 한 번만, 고른 순서 유지, 비어 있으면 400 `INVALID_INPUT`. LAZY 연관관계를 읽으므로 `@Transactional(readOnly = true)`.
- 테스트: `ResumeExportServiceTest`(5), `ResumeExportControllerTest`(4). 전체 통과.
- 프론트: 업무 카드 체크박스(페이지를 넘겨도 선택 유지) → "이력서용 내보내기" → 미리보기 + 복사 + `.md` 파일 저장. 자세한 건 프론트 CLAUDE.md.
- **[도구 참고] Vite 개발 서버가 파일 수정을 놓쳐 이전 버전을 서빙한 적 있음**(`curl http://localhost:5173/src/...`로 확인했더니 최신 수정이 빠져 있었음, `?t=` 쿼리를 붙이면 최신). 화면이 새 코드를 반영 안 하면 개발 서버를 껐다 켤 것.

### 완료 (2026-10-06, 404 처리와 목록 페이징 — 미뤄둔 항목 정리)
- **에러 응답 정리**(`GlobalExceptionHandler`): 없는 주소 `NoResourceFoundException` → 404 `RESOURCE_NOT_FOUND`(스프링은 못 찾은 주소를 정적 파일 요청으로 봐서 이 예외를 던짐), 주소는 맞고 방식이 틀림 `HttpRequestMethodNotSupportedException` → 405 `METHOD_NOT_ALLOWED`, 깨진 JSON `HttpMessageNotReadableException`·번호 자리에 글자/잘못된 날짜 `MethodArgumentTypeMismatchException` → 400 `INVALID_INPUT`. 나머지는 계속 500. **로그인 안 한 채 없는 주소를 부르면 여전히 401**(주소 존재 여부를 알려주지 않는 게 의도). 이전 CLAUDE.md의 "없는 경로가 401/500으로 나옴"은 해결됨.
- `ErrorResponseControllerTest`(5개)로 위 응답 코드를 고정.
- **프로젝트·태그·업무 시스템 목록 페이징**(사용자가 "세 목록 모두"를 선택, 기획서 원칙): `GET /api/projects|tags|systems`가 `page`/`size`/`sort`를 받아 `Page`로 응답(기본 `size=20`, `createdAt` 오름차순 = 먼저 만든 순). 레포지토리 `findByMemberId...(memberId, Pageable)`로 바뀌어 기존 `List` 버전은 삭제. `ListPagingControllerTest`(4개).
- **프론트는 화면 코드를 안 바꿨음**: `getProjects/getTags/getWorkSystems`가 `?size=1000`으로 받아 `content`만 돌려줌(업무 화면 드롭다운·체크박스에 전체가 필요해서). 한 종류가 1000개를 넘으면 잘리는 한계가 있으나 개인용이라 허용. 사용자에게 "페이징 효과가 작다"고 설명했지만 원칙대로 하기로 함.
- 전체 테스트 통과 + 서버 재시작 후 네 화면(프로젝트·태그·업무 시스템·업무)이 정상으로 뜨는 것 확인.

### 완료 (2026-10-06, 보관 항목 복구 — 기획서 "이후" 기능)
- **결정(사용자)**: 보관 중인 프로젝트에 속한 업무는 복구를 **막고 안내**(409 `PROJECT_ARCHIVED`, "프로젝트를 먼저 복구해 주세요"), 화면은 각 화면에 **"보관함 보기" 토글**(전용 화면 아님).
- API: `GET /api/tasks?archived=true`, `GET /api/projects?archived=true`(보관함, 기본 false), `POST /api/tasks/{id}/restore`, `POST /api/projects/{id}/restore`. `Task.restore()`, `Project.restore()`는 `archivedAt = null`. `ProjectRepository.findByMemberIdAndArchivedAtIsNotNull` 추가. `TaskSpecification.search`에 `archived` 인자 추가(`TaskService.getTasks`도 인자 추가).
- **기획서 규칙 구현 누락을 발견·수정**: "프로젝트를 보관하면 하위 업무도 목록에서 함께 숨긴다"가 지금까지 안 지켜지고 있었음(업무 목록이 업무 자신의 `archivedAt`만 봄). 보통 목록은 이제 `task.archivedAt IS NULL AND project.archivedAt IS NULL`. 업무 보관함(`archived=true`)은 **직접 보관한 업무만** 보여줌(프로젝트 때문에 숨겨진 업무는 안 나옴). 보관 중인 프로젝트에 업무를 새로 만들거나 옮겨 넣는 것도 `PROJECT_ARCHIVED`로 막음(`TaskService.findMyProject`).
- **프로젝트 복구는 하위 업무의 보관 상태를 건드리지 않음**: 프로젝트 보관 때문에 숨겨졌던 업무는 프로젝트만 되살려도 다시 보이고, 직접 보관해 둔 업무는 보관함에 그대로 남는다.
- 테스트: `ArchiveRestoreServiceTest`(7), `ArchiveRestoreControllerTest`(4) + `TaskServiceTest` 호출부 수정. 전체 통과. 프론트까지 실제 브라우저로 보관→보관함→복구, 프로젝트 보관 중 업무 복구 차단 메시지, 프로젝트 복구까지 확인.
- [도구 참고] `./mvnw -q test-compile`이 메서드 시그니처가 바뀐 뒤에도 에러 없이 끝난 적이 있음(증분 컴파일이 테스트를 다시 안 컴파일). 호출부를 직접 확인하거나 테스트 파일을 `touch`해서 다시 컴파일시킬 것. Vite 개발 서버가 또 수정을 놓쳐서(`curl`로 `?t=` 없이 받은 결과가 낡음) 껐다 켬.

### 완료 (2026-10-14, 보관함에서 영구 삭제)
- **결정(사용자)**: 프로젝트는 **업무가 하나라도 남아 있으면(보관한 업무 포함) 막기**(409 `PROJECT_HAS_TASKS`), 확인은 **확인창 + 강한 경고 문구**(이름 타이핑 방식 아님).
- API: `DELETE /api/tasks/{id}/permanent`, `DELETE /api/projects/{id}/permanent`(기존 `DELETE /api/tasks/{id}`는 보관이라 경로로 구분). **보관한 항목만** 영구 삭제 가능(아니면 409 `NOT_ARCHIVED`), 남의 것은 403.
- 업무 영구 삭제(`TaskService.deleteTaskPermanently`): 성과 항목·진행 메모·태그 연결·시스템 연결을 먼저 지우고 `flush()` 후 업무 삭제(FK 순서). **일일 기록(회고 자리)과 태그·시스템 자체는 남김.** `TaskResultRepository.deleteByTaskId`, `TaskLogRepository.deleteByTaskId`, `TaskRepository.existsByProjectId` 추가. `TaskService` 생성자에 `TaskResultRepository`, `TaskLogRepository`가 주입됨.
- 프로젝트 영구 삭제(`ProjectService.deleteProjectPermanently`): `TaskRepository`를 주입받아 업무 존재 여부 확인.
- 테스트: `PermanentDeleteServiceTest`(6), `PermanentDeleteControllerTest`(3). 전체 통과(약 75개). 프론트까지 브라우저로 확인: 업무 남은 프로젝트 삭제 차단 안내, 성과·메모가 달린 업무 삭제, 업무 삭제 후 프로젝트 삭제, 일일 기록 보존(메모만 사라짐). 사용자도 직접 시험 데이터를 정리하며 확인.
- 프론트는 보관함 카드에 "영구 삭제" 버튼과 경고 확인창(업무: 성과·진행 메모도 함께 사라짐 안내, 프로젝트: 업무가 남으면 삭제 안 됨 안내). 자세한 건 프론트 CLAUDE.md.

### 다음 할 일
- 알림(저녁 기록·마감일): 사용자가 "나중에 생각할게"라고 보류 중. 정할 것: 디스코드/슬랙, 웹훅 주소 설정 위치(환경변수 vs 회원별), 시각(저녁 21:00 / 마감일 09:00 제안).
- GitHub 연동, Docker·CI, `LEARNING_NOTES.md` 커밋, 저장소 이름 변경, README 스크린샷.

### 다음 단계
- 기획서 1~5단계 완료. 남은 것은 아래 "미뤄둔 것"과 기획서의 "이후" 기능(보관 항목 복구, 알림, 이력서용 내보내기, GitHub 연동, Docker·CI).
- 미뤄둔 것: 태그/업무 시스템 삭제 시 연결된 업무 처리, 404 정확히 만들기, 프로젝트·태그·시스템 목록 페이징, `LEARNING_NOTES.md` 커밋, 저장소 이름 변경
- 미뤄둔 것들: 태그/업무 시스템 삭제 시 연결된 업무 처리, 404 정확히 만들기(`NoHandlerFoundException`), `LEARNING_NOTES.md` 커밋, 프로젝트·태그·시스템 목록 페이징(지금은 전체 반환), 저장소 이름 `work-log-backend`로 변경
- [도구 참고] 이 세션의 Claude 브라우저는 작업 디렉터리가 백엔드라 `preview_start`의 `launch.json`이 프론트엔드를 못 띄움 → Bash `run_in_background`로 `npm run dev`를 띄우고 `navigate`로 접속하는 방식을 씀. 사용자가 따로 `npm run dev`를 켜 두면 5174로 밀려 CORS 에러가 나니 포트 겹침 먼저 확인.

### 정한 것 (기획서에는 없던, 대화 중 결정)
- 토큰 저장 위치: **localStorage**
- 토큰 만료 시간: **2시간** (처음엔 24시간으로 정했다가 너무 길다는 판단에 2시간으로 변경)
- 회원가입 비밀번호 검증: 8자 이상 72자 이하 (BCrypt 72바이트 한계 고려)
- 에러 응답의 `code` 값은 `ErrorCode` enum 이름을 그대로 사용
- JWT 서명 방식: HS256, `io.jsonwebtoken`(jjwt) 라이브러리 0.12.6
- CORS 허용 origin: `http://localhost:5173`(React를 다른 포트로 만들면 그때 수정)
- 엔티티 수정 메서드 이름은 의미에 맞게: `Project.update(...)`(여러 필드), `Tag.rename(...)`(단일 필드), `Project.archive()`(보관 처리 전용)

### 아직 안 정한 것
- 이메일 대소문자 구분 여부
- **태그·업무 시스템을 삭제할 때, 이미 업무(Task)에 연결되어 있으면 어떻게 할지 — 이제 Task/TaskTag가 생겨서 실제로 발생 가능한 상황이 됐다.** 지금 코드는 아무 검사 없이 그냥 삭제되는데(TagService.deleteTag, WorkSystemService.deleteWorkSystem을 보면 연결 확인이 없음), DB에 FK 제약이 있다면 삭제 자체가 에러 날 수도 있다. 다음에 이 부분을 다룰 때 사용자에게 먼저 물어볼 것(막을지/같이 지울지/그냥 두고 고아 데이터로 남길지).
- 성과 항목 삭제 시 처리 방식
- 조건 조합 조회 구현 방식(QueryDSL vs Specification vs JPQL)
