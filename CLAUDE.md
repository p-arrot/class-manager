# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

信息科技课堂管理系统 — a classroom management system for primary/secondary school IT (Information Technology) classes. Teachers manage courses, tasks, worksheets, student submissions, exams, project-based learning, evaluations (process & result), student ability radar charts, student cloud drives, and semester grade exports.

## Tech Stack

**Backend:** Java 21, Spring Boot 3.4.1, Spring Security 6 (JWT), MyBatis-Plus 3.5.9, PostgreSQL 16, Redis, MinIO, kkFileView, EasyExcel, Flyway, Spring WebSocket + STOMP, SpringDoc OpenAPI, Lombok, MapStruct, Hutool, Maven

**Frontend:** Vue 3, TypeScript, Vite, Pinia, Vue Router, Naive UI, ECharts, Axios, dayjs (not yet scaffolded)

**Deployment:** Docker, Docker Compose, Nginx

## Implementation Status

Phase 1 (backend foundation) is in progress. Currently has:
- Spring Boot app with MyBatis-Plus, Spring Security, JWT auth, Flyway, Redis
- `common/` infrastructure: result wrapper (`R<T>`), exception handling, security config, JWT utils, SpringDoc
- `auth/` module: login endpoint (staff via username, students via student_no + schoolId)
- `user/` module: `User` entity + mapper only
- Flyway `V1__init.sql`: `schools`, `school_classes`, `users`, `teacher_classes`, `audit_logs` tables
- Docker Compose: PostgreSQL 16, Redis 7, MinIO, kkFileView
- AdminInitializer: creates default admin (`admin`/`admin123`) on first startup

Not yet started: frontend, all business modules beyond auth/user, tests, MinIO/preview integration, WebSocket.

## Architecture

Modular monolith (Spring Boot single app, organized by business domain). Base package: `com.example.edu`. Each module follows:

```
modules/xxx/
├── controller/    # Receives params, calls service, returns result (no business logic)
├── dto/           # Input params with @Valid validation + @Schema swagger annotations
├── vo/            # Output params (never return Entity directly)
├── entity/        # Database mapping with @TableName
├── mapper/        # MyBatis-Plus BaseMapper interfaces
├── service/
│   └── impl/      # Business logic, permission checks, transaction control
├── converter/     # MapStruct DTO/Entity/VO converters
└── enums/
```

Planned business modules: `auth`, `user`, `school`, `course`, `semester`, `lesson`, `task`, `evaluation`, `exam`, `project`, `drive`, `stats`, `realtime`, `audit`

Shared infrastructure: `common/config`, `common/security`, `common/exception`, `common/result`, `common/utils`, `infrastructure/minio`, `infrastructure/redis`, `infrastructure/preview`

Roles: **admin** (system init, school/class/teacher management), **teacher** (student management, course delivery, evaluation), **student** (login via student_no, submit work, view grades/radar, personal cloud drive). Teachers can only manage their assigned classes.

## Key Design Rules

- All APIs return `R<T>` unified response: `{code: 0, msg: "ok", data: {}}`
- Pagination returns `PageResult<T>`: `{records, total, page, size}`
- Use `BizException` for business errors, handled by `GlobalExceptionHandler`
- Input validation via `@Valid` on DTOs
- Passwords must use BCrypt, never stored in plaintext. Teachers cannot view student passwords (can only reset).
- All file storage via MinIO; DB stores metadata only. Large uploads use presigned PUT URLs; downloads use presigned GET URLs.
- File preview via kkFileView (doc/docx/ppt/pptx/pdf/html/images)
- Hard deletes prohibited for important business data — use `deleted` SMALLINT with `@TableLogic`
- All DB schema changes via Flyway SQL migrations
- Sensitive operations (password reset, data deletion, grade changes, special-case marking, exports, teacher actions on student files) must write audit logs
- Permission checks required in Service layer (Controller-level annotations alone are insufficient)
- `@Transactional(rollbackFor = Exception.class)` on multi-table write methods
- Entity never returned directly to frontend; always converted to VO
- Course structure: Course → Semester → Lesson → Task (worksheet/artifact)

## Database

PostgreSQL 16 with JSONB for flexible schemas (form schemas, submissions, exam papers).

### Naming Conventions
- Tables: plural snake_case (`schools`, `school_classes`, `users`, `teacher_classes`, `audit_logs`)
- Primary keys: `GENERATED ALWAYS AS IDENTITY`, column name `id`
- Timestamps: `created_at`, `updated_at` with `NOT NULL DEFAULT now()`
- Logical delete: `deleted SMALLINT NOT NULL DEFAULT 0`
- Foreign key columns: `{table}_id` (e.g., `school_id`, `class_id`, `teacher_id`)
- Index names: `idx_{table}_{column}`

### MyBatis-Plus Entity Mapping
- `@TableName` for table mapping
- `@TableId(type = IdType.AUTO)` for auto-increment PKs
- `@TableLogic` for logical delete field
- `@TableField(fill = FieldFill.INSERT)` / `FieldFill.INSERT_UPDATE` for auto-timestamps (handled by `MyMetaObjectHandler`)
- camelCase <-> snake_case auto-conversion enabled
- `type-aliases-package`: `com.example.edu.modules`

### Flyway
- Migration files: `src/main/resources/db/migration/V{version}__{description}.sql`
- `baseline-on-migrate: true`

## JWT Authentication Flow

- Login: POST `/api/auth/login` with `{account, password, schoolId?}`
  - Admin: login via `username` (no schoolId needed)
  - Teacher: login via `username` + `schoolId`
  - Student: login via `student_no` + `schoolId`
- Response: `{token, userId, username, name, role, schoolId, classId}`
- Token claims: `sub`=userId, `username`, `role`, `schoolId`, `classId`
- Auth header: `Authorization: Bearer {token}`
- Expiration: configurable via `jwt.expiration` (default 86400000ms = 24h)
- Public endpoints: `/api/auth/login`, `/api-docs/**`, `/swagger-ui/**`
- Auto-admin: `AdminInitializer` creates `admin`/`admin123` on startup if no admin exists

## Project Structure

```
class-manager/
├── backend/
│   ├── docker-compose.yml       # PostgreSQL, Redis, MinIO, kkFileView
│   ├── pom.xml
│   └── src/main/java/com/example/edu/
│       ├── EduApplication.java
│       ├── common/
│       │   ├── config/           # SecurityConfig, JwtAuthenticationFilter, SpringDocConfig, AdminInitializer, MyMetaObjectHandler
│       │   ├── exception/        # BizException, GlobalExceptionHandler
│       │   ├── result/           # R<T>, PageResult<T>, ErrorCode
│       │   └── security/         # JwtUtils, LoginUser
│       └── modules/
│           ├── auth/             # controller, dto, vo, service (login flow)
│           └── user/             # entity (User), mapper (UserMapper)
├── docs/
│   └── project-prompt.md        # Full project specification
└── CLAUDE.md
```

## Coding Conventions (from existing code)

- Controllers: `@RestController`, `@RequestMapping("/api/xxx")`, `@RequiredArgsConstructor` + `private final` injection, `@Valid @RequestBody` on POST
- Services: `@Service`, `@Slf4j`, `@RequiredArgsConstructor`
- DTOs: `@Data`, `@Schema(description="...")`, `jakarta.validation` constraints (`@NotBlank`, etc.)
- VOs: `@Data`, `@Builder` (or `@NoArgsConstructor`/`@AllArgsConstructor` for non-builder)
- Entities: `@Data`, `@TableName`, `@TableId`, `@TableLogic`, `@TableField(fill=...)`
- Mappers: `@Mapper`, extends `BaseMapper<Entity>`
- ErrorCode: enum with `(int code, String msg)`, used by `BizException`
- Business exceptions use `ErrorCode` constants, never hardcoded messages

## Common Commands

### Backend
```bash
cd backend
mvn spring-boot:run            # Start backend on port 8080
mvn clean compile              # Compile only
mvn clean package -DskipTests  # Package JAR
docker-compose up -d           # Start dev services (PostgreSQL, Redis, MinIO, kkFileView)
```

### Frontend
(To be scaffolded.)

## Known Issues

- **JWT secret** in `application-dev.yml` is a placeholder — generate a strong random string for production.
- **No tests exist yet** — `spring-boot-starter-test` and `spring-security-test` are in pom.xml but no test files.

## Evaluation System

Four IT core competency dimensions: `AWARENESS` (信息意识), `COMPUTING` (计算思维), `DIGITAL_LEARNING` (数字化学习与创新), `RESPONSIBILITY` (信息社会责任).

Six-level grading: A(100) B(80) C(60) D(40) E(20) F(0). Teachers grade A-E; auto F on missed deadline.

- **Process evaluation** = weighted avg of worksheet (weight 1.0) and artifact (weight 1.5) scores
- **Result evaluation** = weighted avg of exam and project scores
- **Semester grade** = process × 50% + result × 50% (not generated if either is missing data)
- Radar chart from worksheet and artifact evaluations (exams and projects excluded from radar)

## Development Order (8 phases)

1. Backend foundation (Spring Boot init, security, JWT, Flyway, Docker Compose) — **in progress**
2. School/class/user management + student Excel import + password reset
3. Course/semester/lesson + course resources + file upload/preview
4. Classroom tasks (worksheet form builder + artifact submissions)
5. Four-dimension evaluation + radar charts + auto-grade F on deadline miss
6. WebSocket real-time aggregation on worksheet submission
7. Exams (papers, timed exams, scoring) + project-based learning (team formation, submissions, scoring) + result evaluation
8. Student cloud drive + teacher access + semester grade Excel export
