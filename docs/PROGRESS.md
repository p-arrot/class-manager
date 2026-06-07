# 开发进度

> **用途：** 项目进度的唯一跟踪文件。记录各阶段完成状态、待办清单。
> **相关文档：** 规格见 `SPECIFICATION.md`，前端规划见 `FRONTEND_PLAN.md`，后端规划见 `BACKEND_PLAN.md`，API 见 `API.md`

## 进度总览

| 阶段 | 内容 | 状态 |
|------|------|------|
| Phase 1 | 后端基础工程 | ✅ 已完成 |
| Phase 2 | 班级、教师、学生管理 | ✅ 已完成 |
| Phase F0 | 前端脚手架 + 登录 + Layout | ✅ 已完成 |
| Phase 3a | 后端：课程、学期、课时 | ✅ 已完成 |
| Phase F1 | 前端：管理员端 + 教师/学生课程页 | ✅ 已完成 |
| Phase 3b | 后端：MinIO 文件基础设施 | ✅ 已完成 |
| Phase F2 | 前端：文件组件 + 课程资源 | ✅ 已完成 |
| Phase 4 | 后端：课堂任务 + 实时汇总 | ✅ 已完成 |
| Phase F3 | 前端：课堂任务 + 实时统计 | ✅ 基本完成 |
| Phase 5 | 后端：评分和雷达图 | ✅ 已完成 |
| Phase F4 | 前端：评分 + 雷达图 | ✅ 已完成 |
| Phase 6a | 后端：考试系统 | ✅ 已完成 |
| Phase 6b | 后端：项目化学习 | ✅ 已完成 |
| Phase 6c | 后端：结果评价 | ✅ 已完成 |
| Phase F5 | 前端：考试 + 项目 + 结果评价 | ✅ 已完成 |
| Phase 7 | 后端：网盘和总评导出 | ✅ 已完成 |
| Phase F6 | 前端：网盘 + 总评导出 | ✅ 已完成 |
| Phase 8 | 评分模型重构 + 工作台性能优化 | 🔄 进行中 |

---

## Phase 1 — 后端基础工程 ✅

- Spring Boot 3.4.1 + Maven + application.yml
- Docker Compose（PostgreSQL 16, Redis 7, MinIO, kkFileView）
- Flyway V1：schools, school_classes, users, teacher_classes, audit_logs
- 统一响应 `R<T>` / `PageResult<T>` / `ErrorCode`（27 个错误码）
- `BizException` / `GlobalExceptionHandler`
- Spring Security 6 + JWT（全局查找：admin/teacher 按 username，student 按 student_no）
- BCrypt 密码加密，AdminInitializer（admin/admin123）
- SpringDoc OpenAPI + MyBatis-Plus 分页 + 审计日志基础设施

## Phase 2 — 班级、教师、学生管理 ✅

- 班级 CRUD + 教师 CRUD + 班级绑定/解绑
- 学生 Excel 导入（EasyExcel，含失败原因反馈）+ 分页列表 + 按班级筛选
- 学生密码重置（默认/指定新密码，BCrypt，审计日志）
- 教师数据隔离：只能访问负责班级的学生
- Flyway V2 + V3（全局唯一索引、性能索引）
- 16 个 REST 端点

## Phase F0 — 前端脚手架 ✅

- Vite + Vue 3 + TypeScript + Naive UI（Quiet Precision 主题）
- Axios 封装（token 注入、双格式响应处理、401 跳转）
- 登录页（左右双栏）+ 路由守卫（按 role 分发）
- AdminLayout / TeacherLayout / StudentLayout 三套骨架
- Pinia auth + theme store
- 5 个 API 模块 + TypeScript 类型定义

## Phase 3a — 后端：课程、学期、课时 ✅

- Course CRUD + CourseClass 关系表（课程-班级多对多）
- Semester CRUD + Lesson CRUD（含排序号 + 拖拽重排）
- 课程资源文件夹（树形目录结构）
- 权限：教师管理自己的课程；学生仅可见关联班级的课程
- Flyway V4（5 张新表 + 9 个索引）+ 18 个 REST 端点

---

## Phase F1 — 前端：管理员 + 教师/学生课程页 🔄

### 已完成 ✅

- [x] 管理员：班级管理（NDataTable + 年级筛选 + CRUD 弹窗）
- [x] 管理员：教师管理（表格 + 创建/编辑/班级绑定 + 删除按钮）
- [x] 管理员：学生管理（表格 + 班级筛选 + Excel 导入 + 密码重置 + 批量操作）
- [x] 教师：课程列表（卡片网格 + 创建/编辑/删除 + 班级多选）
- [x] 教师：课程详情（学期 Tab + 课时 Tab + 排序 + 面包屑）
- [x] 学生：课程列表 + 课程详情（只读卡片 + 只读学期/课时）
- [x] 改造 TeacherLayout：侧边栏 + 顶栏（NMenu：工作台/课程管理）
- [x] 改造 StudentLayout：侧边栏导航（NMenu：我的课程）
- [x] 三套布局统一 `min-height:100vh`（div flex 方案替代 NLayout 高度依赖）
- [x] 抽取共享组件：`CourseCard.vue`、`PageHeader.vue`
- [x] 抽取工具函数：`utils/date.ts`（dayjs，替换 5 处手写 formatDate）
- [x] 后端新增 `DELETE /api/teachers/{id}` 端点
- [x] API 模块：courses.ts / semesters.ts / lessons.ts + 12 个 TS 类型
- [x] Bug 修复：getCourse 导入、学生 HomeView 静态页、SecurityConfig 权限、卡片高度统一

- [x] 班级命名从年级制改为入学年份制（grade: "三年级" → "2024"，显示格式: "2024级1班"）
- [x] TeacherLayout 班级选择器接入 Pinia 全局过滤（useClassFilterStore + 后端 classId 筛选）

### 待完成 🔲

（无待完成项）

---

## Phase 3b — 后端：MinIO 文件基础设施 ✅

> 详细设计见 `BACKEND_PLAN.md` §六

### MinIO 基础设施

- [x] `infrastructure/minio/MinioProperties.java` — `@ConfigurationProperties(prefix="minio")`
- [x] `infrastructure/minio/MinioConfig.java` — `MinioClient` Bean
- [x] `infrastructure/minio/MinioService.java` — 预签名 PUT/GET URL、deleteObject、getObjectInfo、ensureBucketExists

### kkFileView 预览集成

- [x] `infrastructure/preview/PreviewProperties.java` — `@ConfigurationProperties(prefix="kkfileview")`
- [x] `infrastructure/preview/PreviewService.java` — `generatePreviewUrl(presignedGetUrl)`

### 数据库迁移 (Flyway V5)

- [x] `V5__phase3b.sql` — `course_resources` 加 `file_size`/`content_type`/`object_name`；新建 `lesson_resources` 表

### 课程资源文件支持

- [x] `CourseResource.java` entity — 加 `fileSize`/`contentType`/`objectName` 字段
- [x] `CourseResourceVO.java` — 同上
- [x] `CourseResourceServiceImpl.java` — `delete()` 加 MinIO 清理；`toVO()` 加文件字段映射；注入 `MinioService`
- [x] `FileUploadDTO.java` + `FileUploadVO.java`
- [x] `FileService.java` + `FileServiceImpl.java` — 校验→创建 CourseResource→生成预签名 URL→审计日志
- [x] `FileController.java` — 3 个端点：`POST /api/files/upload/presigned`、`GET /api/files/{id}/download`、`GET /api/files/{id}/preview`

### 课时资源预建

- [x] `LessonResource.java` entity — `lesson_resources` 表实体（CRUD 后续扩展）

### 错误码与配置

- [x] `ErrorCode.java` — 新增 `FILE_SIZE_EXCEEDED(40005)` / `FILE_TYPE_NOT_ALLOWED(40006)` / `FILE_NOT_FOUND(40414)`
- [x] `application-dev.yml` — 新增 `kkfileview.base-url: http://localhost:8012`

---


> 详细任务清单见 `FRONTEND_PLAN.md` 第六节和 `SPECIFICATION.md` 第二十九节。历史规划中的 F2-F6/Phase 4-7 已基本落地；后续重点转向评分模型一致性、权限收敛、性能和体验质量。

---

## Phase 4 — 后端：课堂任务 + WebSocket ✅

- Flyway V6：tasks + submissions 表（TEXT 类型存储 JSON）
- Task 实体 + Submission 实体 + Mapper
- TaskController：6 个端点（CRUD + submit + list submissions）
- 权限回溯：Task → Lesson → Semester → Course
- WebSocket/STOMP 配置（`/ws` SockJS 端点，`/topic/task/{id}` 推送）
- RealtimeService：学生提交后自动推送到教师端
- Student submit: upsert 逻辑 + 截止时间检查 + 已评分不可修改
- ErrorCode: TASK_NOT_FOUND / SUBMISSION_NOT_FOUND / TASK_DEADLINE_PASSED / SUBMISSION_ALREADY_GRADED / TASK_SUBMIT_STUDENT_ONLY

## Phase F3 — 前端：课堂任务 ✅

- `api/tasks.ts`：7 个 API（list/create/update/delete/submit/listSubmissions）
- `types/api.ts`：TaskVO / TaskDetailVO / TaskCreateDTO / TaskUpdateDTO / SubmissionVO / SubmissionDTO
- `LessonTaskPanel.vue`：任务列表（折叠行内嵌）、创建/编辑/删除弹窗
- Teacher CourseDetail：课时表 expand row 显示 LessonTaskPanel
- Student CourseDetail：课时表 expand row 显示 LessonTaskPanel（只读）
- `TaskCreate.vue`：问卷式题目创建界面，支持填空、单选、多选、是非、简答、题干 Markdown/Monaco、题目配图、截止时间选择器
- `WorksheetRenderer.vue`：学生端问卷式答题体验，Markdown 渲染题干

## Phase 5 — 后端：四维度评价 + 雷达图 ✅

- Flyway V8：evaluations 表（dimension / grade / is_special）
- Evaluation 实体 + Mapper
- EvaluationController：4 个端点（evaluate / student evaluations / radar / auto-grade）
- 评分逻辑：按维度 A-E 评分，支持特殊情况标记
- 雷达图数据：当前学期四维度均分 + 上学期对比
- 自动 F 评：截止后未提交的学生自动评 F
- ErrorCode: EVALUATION_NOT_FOUND

## Phase 6a — 后端：考试系统 ✅

- Flyway V9：exam_papers / exams / exam_classes / exam_submissions 表
- Exam 实体 + ExamPaper 实体 + ExamSubmission 实体 + Mapper
- ExamController：8 个端点（papers CRUD / exams CRUD / submit / list / grade）
- 缺考处理：absent 状态 + 0 分
- 考试关联 `version: 3` 题目 schema；自动批改题提交后写入四维度数值得分

## Phase 6b — 后端：项目化学习 ✅

- Flyway V9：projects / project_teams / team_members / project_submissions / project_scores 表
- Project / ProjectTeam / ProjectTeamMember / ProjectSubmission 实体 + Mapper
- ProjectController：项目 CRUD、组队、提交、提交列表、项目提交逐维度评分
- 项目支持文件/文件夹提交配置、文件后缀限制、按核心素养维度设置评分项

## Phase 6c — 后端：结果评价 ✅

- Flyway V11：assessment_schemes 表
- Flyway V12：dimension_scores 表
- StatsService：按平时任务、考试、项目三类来源分别汇总核心素养四维度得分
- 学期考核方案：平时任务/考试/项目占比合计必须为 100%，默认 50/50/0
- 多考试、多项目通过同一来源桶内“学生得分 / 该维度总分”折算

## Phase 7 — 后端：学生网盘 + 总评导出 ✅

- Flyway V10：user_drive 表（树形目录，FOLDER/FILE）
- DriveItem 实体 + Mapper
- DriveController：4 个端点（tree / createFolder / delete / download）
- MinIO 集成：上传/下载/预览 via presigned URL

## Phase 8 — 评分模型重构 + 性能优化 🔄

- 后端新增 `dimension_scores` 数值得分模型，自动批改和手动逐题评分都按四个核心素养维度落分
- 前端新增 `MarkdownEditor.vue` / `MarkdownView.vue`，Markdown 编辑使用 Monaco 懒加载，降低非编辑页面首屏负担
- `vite.config.ts` 增加 Monaco/ECharts/Naive UI 手动分包，生产构建不再出现大 chunk 警告
- 教师工作台、学生首页改用 `/api/dashboard/teacher` 和 `/api/dashboard/student` 聚合接口，避免前端课程→学期→课时→任务→提交的串行 N+1 请求
- 当前已发现待处理技术债：
  - 学生考试提交权限在 `SecurityConfig` 中可能被 broad `/api/exams/**` 规则提前拦截
  - 项目提交/评分服务需要补齐课程归属权限校验
  - 旧 `/api/projects/{id}/scores` 接口当前只做兼容保留，不应继续作为主要评分入口
  - 教师 dashboard 后端仍会加载较多历史提交，后续应改为 count + limit 查询
