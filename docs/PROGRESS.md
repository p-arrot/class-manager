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

## Phase F1 — 前端：管理员 + 教师/学生课程页 ✅

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
  - [x] 学生端“批改详情”闭环：结果页、首页入口、课时任务入口、提交成功入口、学习评价页入口和结果页专用状态测试已完成
  - [x] 批改反馈数据落点未补齐：已新增 `submission_feedback`，保存 `gradedAt/teacherComment/questionFeedback`
  - [x] 学生可访问 `/api/submissions/{id}` 的提交详情路径需要收紧为“只能看本人提交”，避免同班学生枚举他人提交
  - [x] 教师任务数据看板的提交构成口径已拆分：`submitted` 仅表示待批改，`graded/special/not_submitted` 单独展示，提交率使用已提交待批改 + 已批改 + 特殊处理
  - [x] 教师批改页已区分“自动题预评分”和“整份任务已评分”，并补底部固定保存栏、人工题未评分校验、逐题评语输入
  - [x] 学生学习评价页应默认选中可见课程/当前学期，并从评分明细进入批改详情
  - [x] 提交时间/批改时间已统一为后端 `LocalDateTime` 按 `Asia/Shanghai` 课堂本地时间序列化，前端无时区字符串按本地课堂时间展示
  - [x] 学生考试提交权限已通过 `ExamSecurityConfigTest` 锁定：`POST /api/exams/{id}/submit` 对学生开放，教师/学生越权查看提交列表被拒绝
  - [x] 项目提交/评分服务课程归属权限已通过 `ProjectServiceTest` 锁定：学生跨班提交被拒，非任课教师不能查看/评分
  - [x] 旧 `/api/projects/{id}/scores` 接口已停用并通过测试锁定，主要评分入口为 `/api/project-submissions/{id}/score`
  - [x] 教师 dashboard 提交查询已锁定为 count + limit：列表使用 `selectPage`，数量使用 `selectCount`，教师路径不拉全量 submissions

### Phase 8 批改详情闭环执行顺序

1. [x] 文档补齐：统一真实路由/API，明确 `my-result` 响应结构、状态值、数据落点、验收标准。
2. [x] 后端计划：实现前先写小 plan，优先复用 `TaskServiceImpl` 的任务/提交解析、`QuestionScoreHelper`、`DimensionScoreService`。
3. [x] 后端实现：新增反馈表/实体/VO/接口，收紧提交详情权限，补充时间格式。
4. [x] 后端测试：覆盖本人可见、他人不可见、未提交、待批改、已批改、特殊处理。
5. [x] UI 设计：制作 `TaskResult.vue` 前先使用 `ui-ux-pro-max` 设计页面结构、状态和移动端适配。
6. [x] 前端实现：新增 `TaskResult.vue`、`getMyTaskResult`、`TaskResultVO`、学生结果路由。
7. [x] 前端入口：学生首页最近评分、课时任务列表、学习单提交成功页可进入批改详情。
8. [x] 前端入口：学习评价页评分明细进入批改详情；评价接口/提交接口已返回 `taskId/sourceId/submissionId/taskTitle`。
9. [x] 教师数据看板：修正提交构成口径，`submitted` 仅表示待批改，`graded/special/not_submitted` 单独展示。
10. [x] 教师批改页：自动题预评分文案、人工题必填校验、底部固定保存栏、逐题评语输入。
11. [x] 前端测试：补学生结果页专用状态测试，覆盖 `not_submitted/submitted/graded/special/403`。
12. [ ] 总体质量 review：所有计划任务收尾后，统一检查重复 schema 解析、硬编码状态散落、跨角色数据泄露、时间格式不一致。

### Phase 8 开发记录

- 2026-06-13：完成后端批改详情第一段。
  - 新增 `V14__submission_feedback.sql`、`SubmissionFeedback`、`SubmissionFeedbackMapper`。
  - 扩展 `EvaluateDTO`，教师评分可保存 `teacherComment/questionFeedback`。
  - 新增 `TaskResultVO` 和 `GET /api/tasks/{taskId}/my-result`。
  - 新增 `TaskResultAssembler`，批改详情组装逻辑不堆进 `TaskServiceImpl`。
  - 收紧 `GET /api/submissions/{id}`：学生只能读取本人提交。
  - 验证：`docker run --rm -v "${PWD}\\backend:/workspace" -w /workspace maven:3.9.9-eclipse-temurin-21 mvn "-Dtest=TaskServiceImplTest,TaskResultAssemblerTest,EvaluationServiceImplTest" test`，19 个测试通过。
  - 验证：`V14__submission_feedback.sql` 已在 `edu-postgres` 中通过 `BEGIN` + `ROLLBACK` 语法和约束检查。

- 2026-06-13：完成学生批改详情前端主链路。
  - 新增 `TaskResult.vue`，按 `not_submitted/submitted/graded/special` 展示不同页面状态。
  - 新增 `getMyTaskResult(taskId)` 和 `TaskResultVO` 类型定义。
  - 新增 `/student/tasks/:taskId/result` 路由。
  - 首页最近评分、课时任务列表、学习单提交成功页已接入批改详情跳转。
  - 后续已补：教师看板状态口径、教师批改页交互和学生学习评价页入口均已完成，见 2026-06-14 记录。
  - 验证：`npm.cmd run check` 通过类型检查、Vitest 和生产构建。

- 2026-06-13：完成学习评价页批改详情入口。
  - 后端 `EvaluationVO` 补充 `sourceId/submissionId/taskId/taskTitle/taskStatus/evaluatedAt`。
  - 后端 `getStudentEvaluations` 修复学期过滤：评价记录只来自当前学生在所选学期任务下的提交，避免串学期数据。
  - 后端 `SubmissionVO` 补充 `taskTitle/taskType`，`getStudentSubmissions` 复用同一批任务数据返回提交明细，避免前端逐个查任务。
  - 前端 `EvaluationView.vue` 默认选择第一个可见课程和当前/首个学期，展示“任务批改明细”，已批改/待批改任务可进入 `/student/tasks/:taskId/result`。
  - 共享 `RadarChart.vue` 增加小屏半径适配和 resize 后重算配置，避免移动端雷达标签贴边裁切。
  - 验证：后端 Maven 镜像定向测试 `EvaluationServiceImplTest,TaskServiceImplTest,TaskResultAssemblerTest` 共 20 个测试通过。
  - 验证：`npm.cmd run check` 通过质量门、类型检查、20 个前端单测和生产构建。
  - 验证：Playwright 视觉烟测覆盖 `/student/evaluation` 桌面和 375px 移动端，批改详情/提交状态按钮存在且无横向滚动。

- 2026-06-14：补齐学生批改详情页专用状态测试并修复页面文案乱码。
  - 修复 `TaskResult.vue` 中学生可见中文文案乱码，补充错误态 `errorMessage`，权限/加载失败时不渲染提交内容。
  - 修复 `getErrorMessage` 默认提示文案乱码。
  - 新增 `task-result-view.test.ts`，覆盖 `graded/submitted/not_submitted/special/权限错误` 五个关键状态。
  - 验证：`npm.cmd run test -- task-result-view`，1 个测试文件 5 个测试通过。
  - 验证：`npm.cmd run check`，6 个测试文件 25 个测试通过，并完成质量门、类型检查和生产构建。
  - 备注：本轮尝试用 Playwright 做页面截图烟测，但当前受限沙箱启动 Chrome 被拒绝；自动化组件状态测试和构建验证已完成。

- 2026-06-14：修正教师任务数据看板提交状态口径和页面文案。
  - 后端 `TaskAnalyticsVO.submittedCount` 明确为“已提交待批改”人数，`gradedCount/specialCount/notSubmittedCount` 分别表示已批改、特殊处理、未提交。
  - `submissionRate` 继续按 `(submitted + graded + special) / totalStudents` 计算，表示实际已产生提交或处理记录的完成率。
  - 前端 `TaskAnalytics.vue` 状态图例改为“待批改 / 已批改 / 未提交 / 特殊处理”，顶部指标改为“提交率 / 待批改 / 已批改 / 自动题准确率”，避免把已批改误显示为“已提交”。
  - 修复 `TaskAnalytics.vue` 可见中文文案乱码。
  - 验证：Docker Maven 定向测试 `TaskServiceImplTest` 5 个测试通过。
  - 验证：`npm.cmd run check`，6 个测试文件 25 个测试通过，并完成质量门、类型检查和生产构建。

- 2026-06-14：补齐教师批改页关键交互。
  - 制作页面前已按 `ui-ux-pro-max` 的表单反馈、固定操作区、移动端布局、按钮加载态和可访问性规则做交互 plan；检索脚本在本机安装路径缺失，采用已读取的规则清单执行。
  - 复用 `WorksheetSubmissionPanel` 题目渲染和维度分输入能力，没有另写一套题目解析组件。
  - 自动题区域文案改为“自动题预评分”，并提示保存批改后写入评价数据，避免老师误解为整份任务已批改。
  - 人工题需要逐维度显式填写分数，0 分也必须确认；未完成时阻止保存，显示题目级错误并定位到首个问题题目。
  - 批改页新增顶部评分进度、总评/特殊处理原因输入、逐题评语输入、参考答案展示开关和底部固定“保存批改 / 特殊处理 / 返回数据看板 / 上一份 / 下一份”操作栏。
  - 提交评分时同步发送 `teacherComment/questionFeedback`，特殊处理时要求填写原因。
  - 验证：`npm.cmd run check`，6 个测试文件 25 个测试通过，并完成质量门、类型检查和生产构建。
  - 视觉烟测：前端 dev server 已在 `http://127.0.0.1:5173` 返回 200；Playwright/Chrome 启动仍受 Windows 沙箱限制，报 `CreateProcessAsUserW failed: 5`，未能产出页面截图。

- 2026-06-14：统一提交时间/批改时间格式。
  - 新增 `JacksonConfig`，集中配置 `LocalDateTime` 序列化为 `yyyy-MM-dd'T'HH:mm:ss`，明确按 `Asia/Shanghai` 课堂本地时间输出。
  - 反序列化兼容当前 ISO 本地时间格式和旧的 `yyyy-MM-dd HH:mm:ss` 格式，避免历史客户端请求直接失效。
  - 前端 `formatDate` 对无时区后端字符串按课堂本地时间原样格式化，不再依赖浏览器时区做隐式转换；`toLocalDateTime` 继续复用原有本地时间提交逻辑。
  - 验证：Docker Maven 定向测试 `JacksonConfigTest` 2 个测试通过。
  - 验证：`npm.cmd run check`，6 个测试文件 26 个测试通过，并完成质量门、类型检查和生产构建。

- 2026-06-14：锁定学生考试接口权限规则。
  - 复查 `SecurityConfig` 后确认学生考试开始/提交规则位于 broad `/api/exams/**` 教师规则之前，当前代码没有被提前拦截。
  - 新增 `ExamSecurityConfigTest`，覆盖学生可提交考试、教师不能调用学生提交端点、学生不能查看考试提交列表三种边界。
  - 验证：Docker Maven 定向测试 `ExamSecurityConfigTest` 3 个测试通过。

- 2026-06-14：文档 review 与页面逻辑对齐。
  - 已核对 `API.md`、`SPECIFICATION.md`、`FRONTEND_PLAN.md`、`BACKEND_PLAN.md` 中学生批改详情、教师批改页、教师任务数据看板、时间格式和考试权限的描述。
  - 批改详情链路已写全：学生首页、课时任务列表、提交成功页、学习评价页都进入 `/student/tasks/:taskId/result`；结果页覆盖未提交、待批改、已批改、特殊处理、权限失败和网络失败。
  - 教师链路已写全：发布任务后进入数据看板，数据看板按待批改/已批改/未提交/特殊处理拆分，批改页支持自动题预评分、人工题必填校验、逐题评语、参考答案可见性和返回数据看板。
  - 当时仍保留为后续开发的待办包括项目队伍评分页面和班级分析；其中班级分析基础页已在后续 2026-06-14 记录中完成，项目队伍同分/独立队伍页仍需先确认业务规则。

- 2026-06-14：锁定项目提交/评分课程归属权限。
  - 复查 `ProjectService` 后确认项目模块已复用 `CoursePermissionHelper`，权限链路为 `Project -> Semester -> Course`，没有新建第二套权限逻辑。
  - 新增 `ProjectServiceTest`，覆盖同班学生可提交、跨班学生提交被拒、非任课教师不能查看提交、任课教师可逐维度评分、非任课教师评分被拒、重复提交更新原记录、旧项目评分接口停用。
  - 旧 `/api/projects/{id}/scores` 服务层继续返回 `BAD_REQUEST`；新评分入口保持 `/api/project-submissions/{id}/score` 并写入 `dimension_scores(source_type='project', source_id=submissionId)`。
  - 验证：Docker Maven 定向测试 `ProjectServiceTest` 7 个测试通过。
- 2026-06-14：锁定教师 Dashboard 提交查询性能边界。
  - 复查 `DashboardService.teacherDashboard()` 后确认教师首页只需要两个 Top-N 列表和两个数量指标，当前实现已经使用 `selectPage` 加载待批改/最近提交列表，使用 `selectCount` 统计数量。
  - 新增 `DashboardServiceTest`，验证返回的待批改数、最近提交数、待批改列表、最近提交列表和近期任务正确。
  - 测试同时锁定 `submissionMapper.selectPage(...)` 调用 2 次、`selectCount(...)` 调用 2 次，并确认教师 dashboard 路径不会调用 `submissionMapper.selectList(...)` 拉取全量历史提交。
  - 新增 `scripts/backend-test-docker.ps1`，后端 Docker Maven 测试默认挂载 `class-manager-maven-repo` volume 到 `/root/.m2`，避免每次容器启动都重新下载依赖。
  - 验证：Docker Maven 定向测试 `DashboardServiceTest` 1 个测试通过；缓存 volume 首次预热约 3 分 27 秒，立即复跑约 17 秒且不再重新下载依赖。
- 2026-06-14：补齐学期总评预览/导出权限和教师页面产品化。
  - 后端 `StatsService.calculateSemesterGrades` 已在计算前校验 `semester -> course` 归属，并复用 `CoursePermissionHelper.checkTeacherOwnsCourse`；`exportExcel` 继续复用同一入口，避免预览和导出权限不一致。
  - 扩展 `StatsServiceTest`，覆盖非任课教师不能预览其他课程学期总评、管理员可访问任意课程学期，并保留平时任务、多考试、项目分折算测试。
  - 前端 `GradeExport.vue` 从卡片列表升级为表格型工作台，支持课程/学期筛选、学生数/可生成总评/缺失数据/平均总评摘要、缺失数据聚合提示、完整字段预览和导出加载态。
  - `SemesterStatsPreviewRow` 类型已补齐后端 `GradeRow` 字段：班级、四维度分、结果分和缺失备注。
  - 验证：Docker Maven 缓存脚本定向测试 `StatsServiceTest` 6 个测试通过；`npm run test -- grade-export-view` 2 个测试通过；`npm run check` 7 个测试文件 28 个测试通过并完成生产构建。
- 2026-06-14：补齐考试提交批改基础闭环。
  - 后端 `ExamService.listSubmissions/gradeSubmission` 已沿 `Exam -> Semester -> Course` 校验课程归属，非任课教师不能查看或批改其他课程考试提交。
  - 考试人工批改支持通过 `dimensionScores` 写入 `dimension_scores(source_type='exam', source_id=submissionId)`，雷达图和学期总评可复用；缺考写 `score=0/status=absent` 并清空旧维度得分。
  - 前端 `ExamManage.vue` 增加“提交/批改”入口和批改弹窗：左侧提交列表，右侧题目、学生答案、逐维度评分和缺考标记。
  - 页面复用 `parseTaskSchema/questionStem/questionTotalScore/CORE_DIMENSIONS`，没有新增第二套题目 schema 解析。
  - 验证：Docker Maven 缓存脚本定向测试 `ExamServiceTest` 5 个测试通过；`npm run check` 7 个测试文件 28 个测试通过并完成生产构建。
- 2026-06-14：复盘项目评分实现边界，校准后续计划。
  - 确认用户记忆正确：项目评分并非完全未实现，`ProjectPanel.vue` 已有项目提交列表、作品文件预览/下载、rubric 评分和 `scoreProjectSubmission` 保存能力。
  - 后端项目提交/评分权限和新评分模型已在 `ProjectServiceTest` 中锁定，旧 `/api/projects/{id}/scores` 继续停用。
  - 当前真实缺口是独立教师项目管理页 `ProjectManage.vue` 还没有复用这套提交/批改能力，早期规划的 `/teacher/projects/:id/teams` 队伍视角页面也尚未拆出。
  - 已更新 `FRONTEND_PLAN.md`：下一步不从零重写项目评分，而是先抽取项目提交评分复用层，再同时服务 `ProjectPanel.vue` 和 `ProjectManage.vue`，避免堆出第二套相似逻辑。
- 2026-06-14：独立教师项目管理页接入已有项目提交批改能力。
  - 新增 `useProjectSubmissionScoring.ts`，统一封装项目提交列表、作品预览/下载、rubric 维度分输入和保存评分逻辑。
  - `ProjectPanel.vue` 已改为复用该 composable，继续保留课程详情内的项目列表、创建和删除能力。
  - `ProjectManage.vue` 表格操作列新增“查看提交和批改”，复用 `ProjectSubmissionModal` 和同一套文件预览弹窗；没有新建第二套评分 UI。
  - `ProjectManage.vue` 创建/编辑项目改用 `ProjectCreateModal`，支持提交方式、允许文件后缀和项目 rubric，避免独立页创建出的项目缺少评分配置。
  - 仍未实现队伍同分同步和独立 `/teacher/projects/:id/teams` 页面；这两项需要先确认评分按个人提交还是按队伍统一评分。
  - 验证：`npm run check` 通过质量门、类型检查、7 个前端测试文件 28 个测试和生产构建。
- 2026-06-14：补齐教师班级数据分析基础页面。
  - 新增 `ClassStats.vue` 和 `/teacher/stats` 路由，教师侧边栏新增“数据分析”入口。
  - 页面复用 `useCourseSemesterPicker`、`getSemesterStatsPreview` 和 `RadarChart.vue`，按 `className` 在前端聚合班级指标，不新增第二套后端总评算法。
  - 支持课程/学期/班级筛选，展示学生数、完整总评、缺失数据、平均总评、优秀率、合格率、班级维度雷达、班级对比表和学生明细表。
  - 仍保留为后续增强：跨学期趋势对比、图表/表格导出、从班级聚合行下钻到学生画像。
  - 验证：`npm run check` 通过质量门、类型检查、7 个前端测试文件 28 个测试和生产构建。
