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
| task | ⬜ | 待 Phase 4 |
| realtime | ⬜ | 待 Phase 4（WebSocket + STOMP） |
| evaluation | ⬜ | 待 Phase 5 |
| stats | ⬜ | 待 Phase 5/7 |
| exam | ⬜ | 待 Phase 6a |
| project | ⬜ | 待 Phase 6b |
| drive | ⬜ | 待 Phase 7 |

### 基础设施

| 组件 | 状态 | 说明 |
|------|------|------|
| MinIO | ✅ | Phase 3b 完成（MinioService 预签名 PUT/GET URL、delete、stat、Bucket 自检） |
| Redis | ✅ | 连接就绪，业务缓存待按需接入 |
| kkFileView | ✅ | Phase 3b 完成（PreviewService 预览 URL 生成） |
| EasyExcel | ✅ | 学生导入已实现，总评导出待 Phase 7 |
| Flyway | ✅ | V1-V4 迁移已执行 |
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
## 七、技术决策记录

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
