# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

信息科技课堂管理系统 — a classroom management system for primary/secondary school IT (Information Technology) classes. Teachers manage courses, tasks, worksheets, student submissions, exams, project-based learning, evaluations (process & result), student ability radar charts, student cloud drives, and semester grade exports.

## Tech Stack

**Backend:** Java 21, Spring Boot 3.4.1, Spring Security 6 (JWT), MyBatis-Plus 3.5.9, PostgreSQL 16, Redis, MinIO, kkFileView, EasyExcel, Flyway, Spring WebSocket + STOMP, SpringDoc OpenAPI, Lombok, MapStruct, Hutool, Maven

**Frontend:** Vue 3, TypeScript, Vite, Pinia, Vue Router, Naive UI, ECharts, Axios, dayjs

**Deployment:** Docker, Docker Compose, Nginx

## Implementation Status

Phases 1-7 (all backend phases) and F0-F6 (all frontend phases) are complete. Phase 8 (scoring model refactor + performance) is in progress.

Phase 1 (backend foundation):
- Spring Boot app with MyBatis-Plus, Spring Security, JWT auth, Flyway, Redis
- `common/` infrastructure: result wrapper (`R<T>`), exception handling, security config, JWT utils, SpringDoc
- `auth/` module: login endpoint (global lookup: admin/teacher by username, student by student_no)
- `user/` module: `User` entity + mapper
- Flyway `V1__init.sql`: `schools`, `school_classes`, `users`, `teacher_classes`, `audit_logs` tables
- Docker Compose: PostgreSQL 16, Redis 7, MinIO, kkFileView
- AdminInitializer: creates default admin (`admin`/`admin123`) on first startup

Phase 2 (school/class/user management):
- `classes/` module: SchoolClass + TeacherClass entities, ClassService + ClassController (CRUD + list-all)
- `user/` module extended: TeacherService (CRUD + batch bind/unbind), StudentService (Excel import, paginated list, password reset)
- `audit/` module: AuditLog entity + AuditLogService (cross-cutting, never fails main operation)
- MyBatis-Plus pagination configured (`MybatisPlusConfig` + `mybatis-plus-jsqlparser`)
- Route-level role restrictions in SecurityConfig + `@PreAuthorize` on controllers
- Teacher data isolation: teachers can only access students in their assigned classes

Phase 3a-3b (course/semester/lesson + MinIO file infrastructure):
- `course/` module: Course, Semester, Lesson CRUD + course-class binding + resource tree
- `infrastructure/minio/`: MinioService (presigned PUT/GET, delete, bucket init)
- `infrastructure/preview/`: PreviewService (kkFileView integration)
- FileController: presigned upload, download, preview endpoints
- Flyway V4-V5: course/semester/lesson tables + file metadata columns

Phase 4 (classroom tasks + WebSocket realtime):
- `task/` module: Task CRUD (worksheet + artifact types), Submission (upsert submit, deadline check)
- `realtime/` module: RealtimeService + WebSocket/STOMP config (`/ws`, `/topic/task/{id}`)
- Task schema v3 with dimension scores per question (AWARENESS/COMPUTING/DIGITAL_LEARNING/RESPONSIBILITY)

Phase 5 (evaluation + radar):
- `evaluation/` module: dimension scores, grade levels A-F, auto-grade F on missed deadline
- Radar chart data: current semester + progress comparison
- DimensionScoreService + QuestionScoreHelper for per-question scoring

Phase 6a-6c (exams, projects, result evaluation):
- `exam/` module: exam papers, exam tasks, submissions, absent handling, auto-grading
- `project/` module: projects, teams, submissions, rubric scoring
- `stats/` module: StatsService aggregates process/exam/project dimension scores
- AssessmentScheme: per-semester weighting (process/exam/project must sum to 100%)

Phase 7 (student drive + semester grade export):
- `drive/` module: DriveItem tree (FOLDER/FILE), MinIO-backed storage, teacher access
- Excel export via EasyExcel: per-class sheets with four-dimension + grade breakdown

Phase 8 (in progress):
- `dimension_scores` numeric scoring model replacing legacy grade-only evaluations
- `dashboard/` module: teacher/student aggregated home page endpoints
- Frontend: MarkdownEditor, MarkdownView, Monaco lazy-load, Vite chunk splitting
- Remaining: security hardening, N+1 query cleanup on dashboard

### Frontend

All frontend phases (F0-F6) are complete. The Vue 3 + TypeScript + Naive UI app is fully scaffolded with:
- Login page with animated password characters, three role layouts, router guards
- Admin: class/teacher/student management (CRUD, Excel import, password reset)
- Teacher: course/semester/lesson management, task creation/grading, exam/project management, grade export, student overview, task analytics
- Student: course browsing, task submission, exam taking, project submission, evaluation/radar view, personal drive
- Shared components: CourseCard, PageHeader, FileUpload, FilePreview, MarkdownEditor, MarkdownView, RadarChart, WorksheetRenderer, AssessmentSchemePanel, etc.
- Design system: "Quiet Precision" refined minimalist design (see `docs/SPECIFICATION.md` §28)

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

Implemented business modules: `auth`, `user`, `classes`, `course`, `task`, `evaluation`, `exam`, `project`, `drive`, `stats`, `realtime`, `dashboard`, `audit`

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

- Login: POST `/api/auth/login` with `{account, password}` (no schoolId needed — session-scope only)
  - Attempts `username` + `role=admin` first, then `username` + `role=teacher`, then `student_no` + `role=student`
  - All three lookups are global (username and student_no each have unique indexes across all schools)
  - Built-in constant-time defense against username enumeration
- Response: `{token, userId, username, name, role, classId}`
- Token claims: `sub`=userId, `username`, `role`, `classId`
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
│       │   ├── config/           # SecurityConfig, JwtAuthenticationFilter, SpringDocConfig, AdminInitializer, MyMetaObjectHandler, MybatisPlusConfig
│       │   ├── exception/        # BizException, GlobalExceptionHandler
│       │   ├── result/           # R<T>, PageResult<T>, ErrorCode
│       │   └── security/         # JwtUtils, LoginUser
│       └── modules/
│           ├── auth/             # controller, dto, vo, service (login flow)
│           ├── user/             # entity/mapper + TeacherService, StudentService, controllers
│           ├── classes/          # SchoolClass/TeacherClass entities, ClassService/Controller
│           ├── course/           # Course, Semester, Lesson, CourseResource, assessment schemes
│           ├── task/             # Task, Submission (worksheet + artifact)
│           ├── evaluation/       # Evaluation, DimensionScore, radar data
│           ├── exam/             # Exam, ExamPaper, ExamSubmission
│           ├── project/          # Project, ProjectTeam, ProjectSubmission
│           ├── drive/            # DriveItem (student cloud drive)
│           ├── stats/            # StatsService (aggregation, export)
│           ├── realtime/         # WebSocket/STOMP realtime push
│           ├── dashboard/        # Teacher/student aggregated home endpoints
│           └── audit/            # AuditLog entity, AuditLogService (never fails main op)
├── docs/
│   ├── SPECIFICATION.md         # Full project specification
│   ├── PROGRESS.md              # Development progress tracking
│   ├── API.md                   # API documentation
│   ├── FRONTEND_PLAN.md         # Frontend planning
│   └── BACKEND_PLAN.md          # Backend planning
├── frontend/                     # Vue 3 frontend app
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
mvn test                       # Run all tests
mvn test -Dtest=ClassName      # Run a single test class
mvn test -Dtest=ClassName#m    # Run a single test method
docker-compose up -d           # Start dev services (PostgreSQL, Redis, MinIO, kkFileView)
docker-compose down -v         # Tear down services (removes volumes/data)
bash api-test.sh login admin   # API test helper (auto token cache, Chinese-safe)
```

### Frontend
```bash
cd frontend
npm install                     # Install dependencies
npm run dev                     # Start dev server on port 5173
npm run build                   # Production build
npm run test:run                # Run Vitest tests
bash test-f1.py                 # Playwright functional test
```

## Known Issues

- **JWT secret** in `application-dev.yml` is a placeholder — generate a strong random string for production.
- **Teacher dashboard** loads excessive historical submissions — should be changed to count + limit queries.
- **Student exam submission** permissions may be intercepted by broad `/api/exams/**` SecurityConfig rules.
- **Project scoring** service needs course ownership permission checks.

## Evaluation System

Four IT core competency dimensions: `AWARENESS` (信息意识), `COMPUTING` (计算思维), `DIGITAL_LEARNING` (数字化学习与创新), `RESPONSIBILITY` (信息社会责任).

Six-level grading: A(100) B(80) C(60) D(40) E(20) F(0). Teachers grade A-E; auto F on missed deadline.

- **Process evaluation** = aggregate of worksheet + artifact dimension scores (earned_score / max_score per dimension)
- **Result evaluation** = aggregate of exam + project dimension scores
- **Semester grade** = process × processPercent + exam × examPercent + project × projectPercent (from AssessmentScheme; defaults 50/50/0; not generated if any required source is missing)
- Radar chart can display dimension scores from any source or combined per assessment scheme
- All dimension scoring uses `dimension_scores` table (source_type = process/exam/project) with per-question earned_score/max_score

## Development Order (backend + frontend interleaved)

```
Phase 1   ✅ Backend foundation (auth, JWT, Flyway, Docker)
Phase 2   ✅ Class/teacher/student management + Excel import + password reset
Phase F0  ✅ Frontend scaffold + login + 3 role layouts + API layer + router guards
Phase 3a  ✅ Backend: Course/semester/lesson CRUD + course-class binding
Phase F1  ✅ Frontend: Admin pages (class/teacher mgmt) + Teacher course/semester/lesson pages
Phase 3b  ✅ Backend: MinIO file infrastructure (upload/download/preview/presigned URLs)
Phase F2  ✅ Frontend: FileUpload/FilePreview/FileList components + course resources UI
Phase 4   ✅ Backend: Classroom tasks (worksheet + artifact) + WebSocket real-time aggregation
Phase F3  ✅ Frontend: Task creator + worksheet form builder + student task submission + real-time stats
Phase 5   ✅ Backend: Four-dimension evaluation + radar charts + auto-grade F
Phase F4  ✅ Frontend: Teacher grading UI + student radar charts
Phase 6a  ✅ Backend: Exam system (papers, timed exams, scoring)
Phase 6b  ✅ Backend: Project-based learning (team formation, submissions, scoring)
Phase 6c  ✅ Backend: Result evaluation
Phase F5  ✅ Frontend: Exam UI + project UI + result evaluation views
Phase 7   ✅ Backend: Student cloud drive + semester grade Excel export
Phase F6  ✅ Frontend: Student drive UI + grade export page
```

Frontend phases start from F0 (scaffold) and run ~1 phase behind backend, keeping each increment deliverable and testable.
