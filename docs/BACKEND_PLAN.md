# 后端模块与实现规划

> **用途：** 后端开发的模块级规划。包含模块依赖关系、实现模式、开发状态。
> **相关文档：** 完整规格见 `SPECIFICATION.md`，进度见 `PROGRESS.md`，API 文档见 `API.md`

---

## 一、模块全景

```
modules/
├── auth/          ✅ 登录认证（JWT token 签发）
├── user/          ✅ 用户/教师/学生 CRUD + Excel 导入
├── classes/       ✅ 班级 CRUD + 教师-班级绑定
├── course/        ✅ 课程 + 学期 + 课时 + 资源文件夹（子包结构）
│   ├── controller/  CourseController, SemesterController, LessonController, CourseResourceController
│   ├── service/     CourseService, SemesterService, LessonService, CourseResourceService
│   ├── entity/      Course, Semester, Lesson, CourseResource, CourseClass
│   └── ...
├── audit/         ✅ 审计日志（跨切面，不影响主操作）
├── task/          🔲 Phase 4 — 课堂任务（worksheet / artifact）
├── realtime/      🔲 Phase 4 — WebSocket + STOMP 实时推送
├── evaluation/    🔲 Phase 5 — 四维度评分
├── stats/         🔲 Phase 5/7 — 统计分析 + 雷达图 + 总评导出
├── exam/          🔲 Phase 6a — 试卷 + 考试任务
├── project/       🔲 Phase 6b — 项目化学习 + 组队
└── drive/         🔲 Phase 7 — 学生网盘

infrastructure/
├── minio/         ✅ Phase 3b — MinioProperties, MinioConfig, MinioService
│                   预签名 PUT/GET URL、删除、对象信息、Bucket 自检
└── preview/       ✅ Phase 3b — PreviewProperties, PreviewService
                   kkFileView 预览 URL 生成
```

---

## 二、模块依赖与权限模型

### 数据依赖链

```
auth (JWT token) ──► 所有模块的身份来源
                      │
                      ├── userId, username, role, classId
                      │
user ◄── classes      │  教师-班级绑定 (teacher_classes)
  │       │           │
  │       └── course ─┴── 课程-班级绑定 (course_classes)
  │              │
  │              ├── semester ── lesson ── task ── submission ── evaluation
  │              │                              │
  │              ├── course_resource             │
  │              │                              │
  │              ├── exam ── exam_submission ────┤
  │              │                              │
  │              └── project ── project_team ────┘
  │
  ├── drive       (学生网盘)
  ├── stats       (跨模块读取：evaluation + exam + project)
  ├── realtime    (监听 submission 写入 → WebSocket 推送)
  └── audit       (监听所有敏感操作)
```

### 权限回溯链

```
操作 Lesson   → 查 Semester → 查 Course → 验证 teacherId == 当前用户
操作 Task    → 查 Lesson → 查 Semester → 查 Course → 同上
查 Submission → 学生：查自己的 (studentId == 当前用户)
              → 教师：查自己班级的 (通过 teacher_classes + course_classes 链路)
```

### 数据隔离规则

| 角色 | 可见范围 | 实现方式 |
|------|----------|----------|
| admin | 全部数据 | 无过滤 |
| teacher | 自己创建的课程 + 负责班级的学生 | `courses.teacher_id` + `teacher_classes` |
| student | 关联班级的课程 + 自己的数据 | `course_classes` + `users.class_id`

---

## 三、标准实现模式

### 3.1 新模块文件清单

```
modules/xxx/
├── controller/XxxController.java    # @RestController, 调用 Service, 不写业务
├── service/XxxService.java          # 接口
├── service/impl/XxxServiceImpl.java # 业务逻辑 + 权限校验 + 事务控制
├── mapper/XxxMapper.java            # MyBatis-Plus BaseMapper
├── entity/Xxx.java                  # @TableName, @TableLogic
├── dto/XxxCreateDTO.java            # @Valid 入参校验
├── dto/XxxUpdateDTO.java            # 可空字段更新
├── dto/XxxPageDTO.java              # page + size + keyword + 筛选项
├── vo/XxxVO.java                    # 列表返回（不含大字段）
├── vo/XxxDetailVO.java              # 详情返回（含子资源/JSONB 字段）
└── enums/XxxEnum.java               # 如有枚举
```

### 3.2 Controller 模式

```java
@Tag(name = "xxx管理")
@RestController
@RequestMapping("/api/xxx")
@RequiredArgsConstructor
public class XxxController {
    private final XxxService xxxService;

    @PostMapping
    @PreAuthorize("hasRole('TEACHER')")          // 方法级权限
    public R<XxxVO> create(@Valid @RequestBody XxxCreateDTO dto) {
        return R.ok(xxxService.create(dto));
    }
}
```

### 3.3 Service 权限回溯链

```
Lesson 写操作 → 查 Semester → 查 Course → 验证 teacherId == 当前用户
Task 写操作  → 查 Lesson → 查 Semester → 查 Course → 同上
提交 读操作  → 学生查自己的 / 教师查自己班级的
```

关键工具：`SecurityUtils.getCurrentUserId()` / `getCurrentUserRole()` / `getCurrentUserClassId()`

### 3.4 审计日志

敏感操作调用：
```java
auditLogService.record("DELETE_COURSE", "course", courseId, detail);
```
审计写入失败 **不抛异常**，仅 `log.warn`。

### 3.5 事务

多表写入方法添加：
```java
@Transactional(rollbackFor = Exception.class)
```

---

## 四、模块开发状态

| 模块 | 状态 | 关键类 |
|------|------|--------|
| auth | ✅ | `AuthController`, `AuthServiceImpl`, `JwtUtils`, `AdminInitializer` |
| user | ✅ | `TeacherService`, `StudentService`（含 Excel 导入、密码重置、批量操作） |
| classes | ✅ | `ClassController`, `SchoolClass`, `TeacherClass` |
| course | ✅ | `CourseController` + `SemesterController` + `LessonController` + `CourseResourceController` |
| audit | ✅ | `AuditLogService`（跨切面，审计失败不影响主操作） |
| task | ✅ | `TaskController`, `TaskServiceImpl`, `TaskAnalyticsVO`, `TaskResultAssembler` |
| realtime | ✅ | `WebSocketConfig`, `RealtimeService`（任务提交/批改事件推送，前端可继续增强订阅范围） |
| evaluation | ✅ | `EvaluationServiceImpl`, `DimensionScoreService`, `SubmissionFeedback` |
| stats | ✅ | `StatsController`, `StatsService`（学期总评预览/Excel 导出） |
| exam | ✅ | `ExamController`, `ExamService`（考试创建、提交、人工批改、缺考处理） |
| project | ✅ | `ProjectController`, `ProjectService`（项目 CRUD、组队、提交、逐维度评分） |
| drive | ✅ | `DriveController`, `DriveService`（学生网盘与教师查看） |

### 基础设施

| 组件 | 状态 | 说明 |
|------|------|------|
| MinIO | ✅ | Phase 3b 完成（MinioService 预签名 PUT/GET URL、delete、stat、Bucket 自检） |
| Redis | ✅ | 连接就绪，业务缓存待按需接入 |
| kkFileView | ✅ | Phase 3b 完成（PreviewService 预览 URL 生成） |
| EasyExcel | ✅ | 学生导入、学期总评导出已实现 |
| Flyway | ✅ | V1-V14 迁移已覆盖当前核心表结构 |
| SpringDoc | ✅ | `/api-docs` + `/swagger-ui.html` 可用 |

---

---
## 六、Phase 3b — MinIO 文件基础设施（详细计划）

### 6.1 目标

在 Phase 3a 课程资源文件夹结构之上叠加文件能力。文件不上传经过后端，而是通过 MinIO 预签名 URL 由前端直传。文件预览通过 kkFileView。

**数据流：**
```
[前端] --POST /api/files/upload/presigned--> [后端] --创建 CourseResource (type=FILE)--> PostgreSQL
                                              [后端] --生成 presigned PUT URL----------> MinIO
[前端] --PUT 直传---------------------------> MinIO
[前端] --GET /api/files/{id}/download-------> [后端] --生成 presigned GET URL---------> [前端直接 GET MinIO]
[前端] --GET /api/files/{id}/preview--------> [后端] --构建 kkFileView URL------------> [前端 iframe 加载]
```

### 6.2 新建文件清单（12 个）

| # | 文件 | 说明 |
|---|------|------|
| 1 | `infrastructure/minio/MinioProperties.java` | `@ConfigurationProperties(prefix="minio")` 绑定 yml 配置 |
| 2 | `infrastructure/minio/MinioConfig.java` | 创建 `MinioClient` Bean |
| 3 | `infrastructure/minio/MinioService.java` | 核心方法：`generatePresignedPutUrl` / `generatePresignedGetUrl` / `deleteObject` / `getObjectInfo` / `ensureBucketExists` |
| 4 | `infrastructure/preview/PreviewProperties.java` | `@ConfigurationProperties(prefix="kkfileview")` |
| 5 | `infrastructure/preview/PreviewService.java` | `generatePreviewUrl(presignedGetUrl)` → kkFileView 预览链接 |
| 6 | `db/migration/V5__phase3b.sql` | `course_resources` 加 `file_size`/`content_type`/`object_name`；新建 `lesson_resources` 表 |
| 7 | `modules/course/dto/FileUploadDTO.java` | 预签名上传请求：`fileName`/`contentType`/`fileSize`/`courseId`/`parentId` |
| 8 | `modules/course/vo/FileUploadVO.java` | 预签名上传响应：`presignedUrl`/`resourceId`/`objectName` |
| 9 | `modules/course/service/FileService.java` | 接口：`createPresignedUpload` / `getDownloadUrl` / `getPreviewUrl` |
| 10 | `modules/course/service/impl/FileServiceImpl.java` | 核心：校验→创建 CourseResource→生成预签名 URL→审计日志 |
| 11 | `modules/course/controller/FileController.java` | `/api/files` 端点 |
| 12 | `modules/course/entity/LessonResource.java` | `lesson_resources` 表实体（预建，CRUD 待后续扩展） |

### 6.3 修改文件清单（5 个）

| # | 文件 | 改动 |
|---|------|------|
| 1 | `modules/course/entity/CourseResource.java` | 加 `fileSize` / `contentType` / `objectName` 字段 |
| 2 | `modules/course/vo/CourseResourceVO.java` | 同上，`@Builder` 类 |
| 3 | `modules/course/service/impl/CourseResourceServiceImpl.java` | `delete()` 中加 MinIO 文件清理；`toVO()` 加文件字段映射；注入 `MinioService` |
| 4 | `common/result/ErrorCode.java` | 新增 `FILE_SIZE_EXCEEDED(40005)` / `FILE_TYPE_NOT_ALLOWED(40006)` / `FILE_NOT_FOUND(40414)` |
| 5 | `application-dev.yml` | 新增 `kkfileview.base-url: http://localhost:8012` |

### 6.4 新增 API 端点

```
POST /api/files/upload/presigned    → 获取预签名 PUT URL（TEACHER，校验课程所有权）
GET  /api/files/{id}/download       → 返回预签名 GET URL（TEACHER/STUDENT）
GET  /api/files/{id}/preview        → 返回 kkFileView 预览 URL（TEACHER/STUDENT）
```

### 6.5 新增错误码

| 错误码 | 说明 |
|--------|------|
| 40005 FILE_SIZE_EXCEEDED | 文件大小超过 200MB 限制 |
| 40006 FILE_TYPE_NOT_ALLOWED | 不支持的文件类型 |
| 40414 FILE_NOT_FOUND | 文件资源不存在或非 FILE 类型 |

已有可复用：`MINIO_ERROR(50001)` / `FILE_UPLOAD_ERROR(50002)`

### 6.6 关键设计决策

| 决策 | 说明 |
|------|------|
| objectName 格式 `{courseId}/{yyyy-MM}/{uuid8}_{fileName}` | 按课程+月份分组，UUID 片段消除命名冲突 |
| 预签名 PUT 过期 10 分钟 | 给前端充足的上传窗口 |
| 预签名 GET 过期 1 小时 | 足够下载和预览 |
| 文件扩展名白名单（19 种） | doc/docx/ppt/pptx/pdf/xls/xlsx/txt/html/htm/jpg/jpeg/png/gif/bmp/mp3/mp4/zip/rar |
| 单文件最大 200MB | 与 `spring.servlet.multipart.max-file-size` 一致 |
| DB 先于 MinIO 写 | 先创建 CourseResource 记录再生成预签名 URL；上传失败则 DB 产生孤儿记录（可接受的折中） |
| MinIO 删除尽力而为 | 删除资源时尝试删 MinIO 文件，失败仅 `log.warn`，不阻断 DB 删除 |
| 上传/删除记审计日志 | 下载和预览不记（只读操作） |
| 权限回溯 | `FileServiceImpl` 沿用 `CourseServiceImpl` 的 `checkTeacherOwnsCourse` / `checkCourseAccess` 模式 |

### 6.7 执行顺序

```
1. infrastructure/minio/MinioProperties.java + MinioConfig.java + MinioService.java
2. infrastructure/preview/PreviewProperties.java + PreviewService.java
3. V5__phase3b.sql（Flyway 迁移）
4. CourseResource.java entity + CourseResourceVO.java（实体字段 + VO 更新）
5. ErrorCode.java（3 个新错误码）
6. FileUploadDTO.java + FileUploadVO.java
7. FileService.java + FileServiceImpl.java
8. FileController.java
9. CourseResourceServiceImpl.java（delete/toVO 适配 + MinioService 注入）
10. LessonResource.java entity（预建实体，CRUD 后续扩展）
11. application-dev.yml（kkfileview.base-url）
```

### 6.8 验证方式

```bash
# 编译验证
cd backend && mvn clean compile

# API 手动测试
# 1. 教师登录获取 token
# 2. POST /api/files/upload/presigned → 获取 presignedUrl
# 3. curl -X PUT presignedUrl --upload-file test.pdf → 直传 MinIO
# 4. GET /api/files/{resourceId}/download → 获取下载 URL → curl 下载验证
# 5. GET /api/files/{resourceId}/preview → 获取 kkFileView 预览 URL

# 前端 E2E（Phase F2 时执行）
# Playwright: 教师 → 课程资源 → 上传文件 → 列表中显示 → 下载 → 预览
```

---
## 七、Phase 8 补充：学生批改详情闭环

### 7.1 目标

补齐学生端“看见批改详情”的后端能力。学生不仅能看到 `已评分` 状态和雷达图汇总，还能看到本人某次任务的逐题答案、逐题得分、维度拆分、教师评语和批改时间。

### 7.2 新增/调整接口

| 方法 | 路径 | 权限 | 说明 |
|------|------|------|------|
| GET | `/api/tasks/{taskId}/my-result` | STUDENT | 学生查看本人该任务批改详情 |
| GET | `/api/submissions/{id}` | ADMIN/TEACHER/STUDENT | 收紧权限：学生只能看本人提交，教师只能看负责班级/课程下提交 |
| POST | `/api/submissions/{id}/evaluate` | ADMIN/TEACHER | 扩展请求体，保存教师总评和逐题评语 |

`my-result` 状态约定：

- `not_submitted`：包装层状态，表示当前学生没有提交记录，不写入 `submissions.status`。
- `submitted`：已提交、待教师批改。
- `graded`：已评分。
- `special`：特殊处理。

### 7.3 数据模型计划

现有 `dimension_scores` 只适合保存数值得分，不适合保存教师总评和逐题文字反馈；因此新增反馈表，避免把评语塞进得分表导致职责混乱。

新增迁移 `V14__submission_feedback.sql`：

| 字段 | 类型 | 说明 |
|------|------|------|
| `submission_id` | BIGINT PK | 对应 `submissions.id` |
| `teacher_id` | BIGINT | 最后批改教师 |
| `teacher_comment` | TEXT | 整份任务总评语 |
| `question_feedback` | TEXT | JSON 数组，元素含 `questionId/comment/referenceAnswerVisible` |
| `graded_at` | TIMESTAMP | 批改完成时间 |
| `created_at` | TIMESTAMP | 创建时间 |
| `updated_at` | TIMESTAMP | 更新时间 |

复用现有表：

- `submissions.content`：学生答案 JSON。
- `submissions.status`：提交状态，使用小写值 `submitted/graded/special`。
- `tasks.form_schema`：题目定义、参考答案、维度满分。
- `dimension_scores`：逐题/维度数值得分，`source_type=process`、`source_id=submission_id`。
- `evaluations`：兼容旧的 A-E/F 等级评价。

### 7.4 VO/DTO 计划

新增 `TaskResultVO`：

- `task`：任务、课程、课时基本信息。
- `status`：`not_submitted/submitted/graded/special`。
- `submission`：提交 ID、状态、答案内容、提交时间、批改时间、教师总评。
- `questions`：题目 ID、序号、题型、题干、是否自动批改、是否展示参考答案。
- `answers`：学生答案 Map。
- `questionResults`：每题正确性、自动/人工、得分、满分、维度得分、题目评语。
- `dimensionSummary`：四维度汇总得分、满分、得分率、等级。

扩展 `EvaluateDTO`：

- `teacherComment`：整份任务总评语。
- `questionFeedback`：逐题反馈数组，字段为 `questionId/comment/referenceAnswerVisible`。
- 现有 `questionScores` 保持为数值得分输入，不混入文字反馈。

### 7.5 服务实现计划

1. 在 `TaskService` 增加 `getMyResult(taskId, studentId)`。
2. 在 `TaskServiceImpl` 复用现有任务访问检查、提交查询、题目 schema 解析逻辑；结果组装逻辑已抽到 `TaskResultAssembler`，避免继续堆大方法。
3. 新增 `SubmissionFeedback` entity/mapper/service，用于保存和查询教师反馈。
4. 在 `EvaluationServiceImpl.evaluate` 中保存 `submission_feedback`，更新 `graded_at` 和 `teacher_id`。
5. 在 `TaskController` 增加 `/api/tasks/{taskId}/my-result`。
6. 修复 `getSubmission(id)` 权限：学生校验 `submission.studentId == currentUser.userId`；教师校验任务所属课程/班级权限。
7. 时间输出统一：优先返回带时区 ISO-8601，或在全局 Jackson 配置中明确 `Asia/Shanghai`。

### 7.6 测试计划

- 未提交学生访问 `my-result`：返回 `status=not_submitted`，不报错。
- 已提交未批改：返回学生答案，`questionResults` 为空或仅含自动题预评分，页面可显示“待教师批改”。
- 已批改：返回总评、逐题评语、逐题维度得分和四维度汇总。
- 学生访问其他同学提交详情：返回 403/业务无权限错误，不泄露内容。
- 教师访问非负责班级/课程提交：返回 403/业务无权限错误。
- 评分后 `dimension_scores` 和 `submission_feedback` 可重复覆盖保存，不产生重复脏数据。

### 7.7 后续后端待办

- 教师任务数据看板口径已明确：`submittedCount` 表示纯 `submitted` 状态人数，即“已提交待批改”；`gradedCount/specialCount/notSubmittedCount` 分别单独表示已批改、特殊处理、未提交，前端图例不得再把 `graded` 归入“已提交”。
- 学习评价页任务评分明细入口已补齐：评价接口返回 `taskId/sourceId/sourceType/submissionId/taskTitle/taskStatus`，提交历史接口返回 `taskTitle/taskType`。
- 时间格式已统一：后端 `LocalDateTime` 通过 `JacksonConfig` 输出 `yyyy-MM-dd'T'HH:mm:ss`，语义为 `Asia/Shanghai` 课堂本地时间；反序列化兼容旧空格格式。

### 7.8 项目提交/评分权限回归计划

项目模块已经复用课程权限链路：`Project -> Semester -> Course -> CoursePermissionHelper`。本轮不新增第二套权限模型，重点是用测试锁住入口，避免后续迭代把项目评分重新绕回旧接口或漏掉课程归属校验。

需要锁定的入口：

1. `listBySemester(semesterId)`：教师只能看自己的课程学期，学生只能看本班绑定课程学期。
2. `create/update/delete(project)`：仅课程创建教师或管理员可写。
3. `createTeam/joinTeam/submit(projectId)`：学生必须属于项目所在课程绑定班级；提交仍受 deadline 限制。
4. `listSubmissions(projectId)`：教师必须拥有项目所属课程。
5. `scoreSubmission(submissionId)`：教师必须拥有提交所属项目课程，评分写入 `dimension_scores(source_type='project', source_id=submissionId)`。
6. 旧 `/api/projects/{projectId}/scores`：兼容路由保留，但服务层继续返回 `BAD_REQUEST`，不再写 `project_scores`。

测试策略：

- 新增 `ProjectServiceTest`，mock `SemesterMapper/CourseMapper/CourseClassMapper` 和项目相关 mapper，分别覆盖学生跨班提交被拒、同班提交可保存、非任课教师不能查看提交/评分、任课教师评分写入 `DimensionScoreService`、旧评分接口被拒。
- 后端验证必须在 Docker Maven 中运行，命令形如：`docker run --rm -v "${PWD}\backend:/workspace" -w /workspace maven:3.9.9-eclipse-temurin-21 mvn "-Dtest=ProjectServiceTest" test`。

执行结果：

- 已新增 `ProjectServiceTest`，覆盖同班提交、跨班拒绝、非任课教师查看/评分拒绝、任课教师逐维度评分、重复提交更新、旧评分接口停用。
- 验证：Docker Maven 定向测试 `ProjectServiceTest` 7 个测试通过。

### 7.9 教师 Dashboard 提交查询性能计划

教师工作台只需要“待批改列表前 N 条”“最近提交前 N 条”和两个数量指标，不应为了首页卡片加载全量历史提交。

实现约束：

1. 课程/学期/课时/任务仍可按教师可见课程聚合，保持现有权限来源。
2. 待批改列表使用 `SubmissionMapper.selectPage(Page(1, TEACHER_LIST_LIMIT), status='submitted')`。
3. 最近提交列表使用 `SubmissionMapper.selectPage(Page(1, TEACHER_LIST_LIMIT), status=null)`。
4. 待批改数量使用 `SubmissionMapper.selectCount(status='submitted')`。
5. 最近提交数量使用 `SubmissionMapper.selectCount(status=null)`。
6. 教师 dashboard 路径不得调用 `submissionMapper.selectList(...)` 加载全部历史提交；学生 dashboard 仍只查询当前学生自己的提交，可后续按需要继续优化。

测试策略：

- 新增 `DashboardServiceTest`，mock 课程、学期、课时、任务、提交 mapper，验证 `teacherDashboard()` 返回列表/数量正确。
- 测试必须验证 `selectPage` 调用 2 次、`selectCount` 调用 2 次，且 `submissionMapper.selectList(...)` 没有在教师 dashboard 中调用。
- 后端验证必须在 Docker Maven 中运行，命令形如：`docker run --rm -v "${PWD}\backend:/workspace" -w /workspace maven:3.9.9-eclipse-temurin-21 mvn "-Dtest=DashboardServiceTest" test`。

执行结果：
- 已新增 `DashboardServiceTest`，锁定教师 dashboard 使用分页列表和 count 查询，不允许通过 `submissionMapper.selectList(...)` 加载全量提交。
- 验证：Docker Maven 定向测试 `DashboardServiceTest` 1 个测试通过；使用缓存脚本首次预热约 3 分 27 秒，立即复跑约 17 秒。

### 7.10 后端 Docker Maven 测试缓存约定

后端测试必须继续在 Docker 里的 Maven 环境运行，避免本机 JDK/Maven 版本漂移。为了减少每次测试重新下载依赖的耗时，统一使用 Docker volume 缓存 Maven 本地仓库：

```powershell
powershell -ExecutionPolicy Bypass -File scripts\backend-test-docker.ps1 -Test DashboardServiceTest
```

脚本约定：
1. 自动创建并复用 `class-manager-maven-repo` Docker volume。
2. 将 `class-manager-maven-repo` 挂载到容器 `/root/.m2`，Maven 依赖只在首次缺失时下载。
3. 将 `backend` 挂载到容器 `/workspace`，继续使用 `maven:3.9.9-eclipse-temurin-21` 镜像。
4. 定向测试用 `-Test "ProjectServiceTest,DashboardServiceTest"`；完整后端测试可省略 `-Test`。
5. 如需附加 Maven 参数，可使用 `-MavenArgs "test"` 或更具体的 Maven goal/参数。

验证结果：
- 首次创建并预热 `class-manager-maven-repo` 时仍会下载依赖，这是一次性成本。
- 同一测试立即复跑时 Maven 不再重新下载依赖，`DashboardServiceTest` 从约 3 分 27 秒降到约 17 秒。

### 7.11 学期总评预览/导出权限与产品化计划

学期总评预览和 Excel 导出包含学生姓名、学号、班级和多来源评价分，属于高敏感聚合数据。后端不能只依赖 Controller 的 `ADMIN/TEACHER` 角色限制，必须在 Service 层沿用课程权限链路校验学期归属。

实现约束：
1. `calculateSemesterGrades(semesterId)` 入口先查询 `Semester`，不存在则返回 `SEMESTER_NOT_FOUND`。
2. 再查询 `Course`，不存在则返回 `COURSE_NOT_FOUND`。
3. 使用 `CoursePermissionHelper.checkTeacherOwnsCourse(course)` 校验：管理员可访问，教师只能访问自己创建/任课的课程学期。
4. `exportExcel(semesterId)` 继续复用 `calculateSemesterGrades(semesterId)`，不新建第二套权限逻辑。
5. 后续前端总评预览页展示后端 `GradeRow` 的完整字段：班级、学号、姓名、四维度分、过程分、考试分、项目分、结果分、总评、等级和缺失数据备注。

测试策略：
- 扩展 `StatsServiceTest`，验证非任课教师访问其他课程学期总评时被拒绝。
- 验证管理员访问同一学期不受教师归属限制。
- 保持现有分数折算测试不退化；后端验证必须使用缓存脚本在 Docker Maven 中运行：`powershell -ExecutionPolicy Bypass -File scripts\backend-test-docker.ps1 -Test StatsServiceTest`。

执行结果：
- `StatsService.calculateSemesterGrades` 已在计算前校验学期和课程归属；`exportExcel` 继续复用同一计算入口，因此预览和导出共享权限边界。
- `StatsServiceTest` 已新增非任课教师拒绝、管理员允许两个边界，并保留平时任务/多考试/项目分折算测试。
- 验证：Docker Maven 缓存脚本定向测试 `StatsServiceTest` 6 个测试通过。

### 7.12 考试提交批改权限与评分落点计划

考试提交和批改属于结果评价来源，必须和项目、平时任务一样复用课程权限链路，避免教师通过提交 ID 查看或批改其他课程学生的考试结果。

实现约束：
1. 教师侧 `listSubmissions(examId)` 必须先查询 `Exam -> Semester -> Course`，并调用 `CoursePermissionHelper.checkTeacherOwnsCourse(course)`。
2. 教师侧 `gradeSubmission(submissionId, ...)` 必须先通过 `ExamSubmission -> Exam -> Semester -> Course` 校验课程归属。
3. 人工批改不能只写 `exam_submissions.score`；如果请求带逐维度分，应写入 `dimension_scores(source_type='exam', source_id=submissionId)`，供雷达图和学期总评复用。
4. 缺考/特殊处理时 `exam_submissions.status='absent'`，总分写 0，并清空该提交已有的考试维度得分；后续若需要“免评不计入总评”，应新增独立特殊状态而不是复用缺考。
5. 继续保留自动题提交后的 `replaceAutoScores("exam", ...)`；教师人工批改使用 `replaceScores("exam", ...)` 覆盖最终结果，避免同一提交残留旧自动得分。

测试策略：
- 新增 `ExamServiceTest`，mock `ExamMapper/ExamSubmissionMapper/SemesterMapper/CourseMapper/DimensionScoreService`。
- 覆盖非任课教师不能查看考试提交列表、任课教师可以查看提交、非任课教师不能批改、任课教师批改会更新提交状态并写入 `DimensionScoreService.replaceScores("exam", submissionId, studentId, scores)`。
- 覆盖缺考批改写 0 分和 `absent` 状态。
- 后端验证使用 Docker Maven 缓存脚本运行：`powershell -ExecutionPolicy Bypass -File scripts\backend-test-docker.ps1 -Test ExamServiceTest`。

执行结果：
- `ExamService.listSubmissions` 已在查询提交前校验 `Exam -> Semester -> Course` 归属。
- `ExamService.gradeSubmission` 已在批改前校验提交所属考试归属；人工批改可写入 `dimension_scores(source_type='exam')`，缺考写 `score=0/status=absent` 并清空旧维度得分。
- `ExamController` 的批改接口已兼容 `dimensionScores` 数组，旧的 `score/absent` 请求仍可用。
- 验证：Docker Maven 缓存脚本定向测试 `ExamServiceTest` 5 个测试通过。

---

## 八、技术决策记录

| 决策 | 原因 |
|------|------|
| 单体应用 + 模块化分包 | 当前规模不需微服务，模块边界清晰便于未来拆分 |
| `school_id` 预留不实现 CRUD | 系统为单学校内部使用 |
| 用户统一 `users` 表，`role` 字段区分 | 简化登录流程（全局查找），避免多表 JOIN |
| `student_no` 全局唯一 | 避免跨班级学号冲突 |
| 课程资源先文件夹后文件 | Phase 3a 数据结构先行，Phase 3b 对接 MinIO |
| 学期/课时嵌套在课程/学期路径下 | REST 层级表达从属关系 |
| 评价三层模型（维度→任务→提交） | 支持 1-4 个维度灵活评分 |
| 雷达图不包含考试和项目 | 第一版简化，后续可扩展 |
