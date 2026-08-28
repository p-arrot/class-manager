# 前端页面与交互设计规划

> **用途：** 前端开发的唯一详细规划参考。包含所有页面功能说明、路由结构、组件设计、交互逻辑、分阶段实现计划。
> **相关文档：** 项目规格见 `SPECIFICATION.md`，进度跟踪见 `PROGRESS.md`，API 文档见 `API.md`
> 署名：Tatakai | 日期：2026-05-02

---

## 一、现有页面评审

### 1.1 已完成的页面

| 页面 | 文件 | 状态 | 评审 |
|------|------|------|------|
| 登录页 | `views/LoginView.vue` | ✅ 合格 | 左右双栏布局，角色自动识别，enter快捷键登录。无需改动 |
| 管理员-班级管理 | `views/admin/ClassManage.vue` | ✅ 合格 | NDataTable + 年级筛选 + CRUD弹窗。分页remote模式正常 |
| 管理员-教师管理 | `views/admin/TeacherManage.vue` | ✅ 合格 | 表格+创建/编辑/班级绑定+删除按钮。 |
| 管理员-学生管理 | `views/admin/StudentManage.vue` | ✅ 合格 | 最完整的页面。表格+班级筛选+Excel导入+密码重置+批量操作 |
| 教师-课程列表 | `views/teacher/CourseList.vue` | ✅ 合格 | 卡片网格+创建/编辑/删除。已修复getCourse导入 |
| 教师-课程详情 | `views/teacher/CourseDetail.vue` | ✅ 合格 | 学期Tab+课时Tab+排序。已移除未使用的updateCourse导入 |
| 学生-课程列表 | `views/student/HomeView.vue` | ✅ 合格 | 只读卡片网格+分页。已统一卡片高度 |
| 学生-课程详情 | `views/student/CourseDetail.vue` | ✅ 合格 | 只读学期+课时。无操作按钮，符合学生权限 |

### 1.2 已发现并修复的问题

| 问题 | 影响 | 修复状态 |
|------|------|----------|
| CourseList.vue 缺少 `getCourse` 导入 | 编辑课程时运行时报错 | ✅ 已修复 |
| Teacher HomeView 显示"课程管理-即将上线" | 教师登录后以为功能未开放 | ✅ 已修复 |
| 登录后跳转到 `/teacher/home` 而非 `/teacher/courses` | 教师看不到课程列表 | ✅ 已修复 |
| 学生 HomeView 为静态占位页 | 学生看不到课程 | ✅ 已修复 |
| 学生缺少课程详情路由 | 学生无法查看课程内容 | ✅ 已修复 |
| SecurityConfig 阻止学生访问 `/api/classes/list-all` | 学生课程详情报403 | ✅ 已修复 |
| 课程卡片高度不统一 | 有描述和没描述的卡片大小不一 | ✅ 已修复 |

### 1.3 尚未修复的设计问题

| 问题 | 严重程度 | 建议 |
|------|----------|------|
| ~~班级命名用自然年级~~ | ✅ 已修复 | 改为入学年份制（"2024级1班"），永久不变 |
| TeacherLayout 班级选择器未与任何功能联动 | 中 | Phase F2 实现班级筛选后使用 |
| 静默 catch `{ /* ignore */ }` 过多 | 低 | 关键操作保留静默，数据加载加 toast 提示 |
| ~~无共享组件，dateFormat 重复~~ | ✅ 已修复 | `components/` + `utils/date.ts` |
| ~~dayjs 未使用~~ | ✅ 已修复 | date.ts 使用 dayjs |
| ~~教师管理页无删除按钮~~ | ✅ 已修复 | 后端 `DELETE /api/teachers/{id}` + 前端按钮 |
| ~~TeacherLayout/StudentLayout 纯顶栏无导航~~ | ✅ 已修复 | 侧边栏 NMenu 导航 |
| ~~AdminLayout 缺少 `has-sider`~~ | ✅ 已修复 | Naive UI 警告消除 |

---

## 二、完整页面清单

### 2.1 页面总览

```
/login                         登录页

/admin                          管理员布局（侧边栏导航）
  /admin/classes                班级管理
  /admin/teachers               教师管理
  /admin/students               学生管理

/teacher                        教师布局（顶部导航 + 侧边栏）
  /teacher/home                 教师工作台（仪表板）
  /teacher/courses              课程列表
  /teacher/courses/:id          课程详情（嵌套路由）
  /teacher/courses/:id/resources 课程资源管理（文件树 + 上传）
  /teacher/semesters/:id/lessons/:lid/tasks  课时任务管理（学习单/作品）
  /teacher/tasks/:taskId/analytics          任务数据看板 + 实时统计
  /teacher/grading/:taskId                  评分页（四维度 A-E）
  /teacher/exams                            考试管理（试卷列表 + 考试任务）
  /teacher/exams/:id/submissions            考试提交查看
  /teacher/projects                         项目管理
  /teacher/projects/:id/teams               项目队伍 + 评分
  /teacher/stats                            班级数据分析（跨班级对比）
  /teacher/students/:id/drive               查看学生网盘
  /teacher/grade-export                     学期总评导出

/student                        学生布局（顶部导航）
  /student/home                 我的课程
  /student/courses/:id          课程详情
  /student/courses/:id/resources 课程资源（只读）
  /student/lessons/:lid         课时详情（资源 + 任务入口）
  /student/tasks/:id             学习单填写 / 作品提交
  /student/exams                 我的考试
  /student/exams/:id             考试答题
  /student/projects              我的项目
  /student/projects/:id          项目详情 + 个人提交 + 批改详情
  /student/evaluation            我的评价 + 雷达图
  /student/tasks/:taskId/result  批改详情（核心闭环已完成）
  /student/drive                 我的网盘
```

### 2.2 页面详细功能说明

---

#### 登录页 `/login`

**功能：**
- 账号+密码登录，角色自动识别
- Enter 键快捷登录
- 登录成功按 role 跳转：admin→`/admin/classes`，teacher→`/teacher/courses`，student→`/student/home`
- 错误提示（用户名或密码错误）
- 深色/浅色主题适配

**状态：** ✅ 已完成

---

#### 管理员布局 `AdminLayout`

**导航侧边栏：**
- 班级管理 → `/admin/classes`
- 教师管理 → `/admin/teachers`
- 学生管理 → `/admin/students`

**顶栏：** 管理员工作台 + 主题切换 + 退出登录

**状态：** ✅ 已完成

---

#### 管理员-班级管理 `/admin/classes`

**功能：**
- 分页表格：年级、班级名、创建时间
- 年级筛选（客户端过滤）
- 新建班级弹窗（年级 + 班级名）
- 编辑班级弹窗
- 删除班级（确认对话框，检查学生/教师关联）

**状态：** ✅ 已完成

**改进建议：** 年级筛选应联动后端分页（改为 remote 模式）

---

#### 管理员-教师管理 `/admin/teachers`

**功能：**
- 分页表格：用户名、姓名、状态标签、负责班级数、创建时间
- 关键词搜索（用户名/姓名）
- 创建教师弹窗（用户名 + 姓名 + 密码）
- 编辑教师弹窗（姓名 + 电话 + 邮箱 + 启用开关）
- 绑定班级弹窗（班级多选列表）
- 删除教师按钮（含后端 `DELETE /api/teachers/{id}`）

**状态：** ✅ 已完成

---

#### 管理员-学生管理 `/admin/students`

**功能：**
- 分页表格：学号、姓名、班级、状态、创建时间（带行选择框）
- 班级筛选下拉
- 关键词搜索（学号/姓名）
- Excel 导入（upload + 结果反馈弹窗）
- 单个密码重置（默认密码/指定新密码）
- 编辑学生弹窗（姓名 + 班级 + 启用）
- 批量重置密码
- 批量删除

**状态：** ✅ 已完成（功能最完整的页面）

---

#### 教师工作台 `/teacher/home`

**功能：**
- 今日概览：待评分、即将截止、近期提交
- 待评分提交可直接跳转教师批改页
- 即将截止任务和最近提交支持展开查看
- 空状态引导教师创建课程和课堂任务

**状态：** ✅ 已完成；早期“即将上线”入口卡片已被真实 dashboard 数据概览替代

---

#### 教师-课程列表 `/teacher/courses`

**功能：**
- 响应式3列卡片网格
- 每张卡片：首字母封面、名称、教师名、描述（2行截断）、班级数标签
- 操作：进入课程、编辑、删除
- 创建课程弹窗（名称 + 介绍 + 班级多选）
- 编辑课程弹窗（含班级绑定修改）
- 分页
- 空状态引导

**状态：** ✅ 已完成

---

#### 教师-课程详情 `/teacher/courses/:id`

**功能：**
- 面包屑导航：课程列表 > 课程名
- 课程头部：名称、描述、班级标签列表
- 学期管理 Tab：
  - 学期卡片列表（名称、日期范围、课时数）
  - 创建/编辑学期弹窗（名称 + 日期范围选择器）
  - 删除学期（检查课时依赖）
- 课时管理 Tab：
  - 学期选择器下拉
  - 课时数据表格（序号、名称、操作）
  - 上移/下移排序按钮
  - 创建/编辑课时弹窗（名称）
  - 删除课时

**状态：** ✅ 已完成

**补充：** 课程资源已通过独立路由 `/teacher/courses/:courseId/resources` 承载，课程详情页保持课程/学期/课时主流程。

---

#### 教师-课程资源 `/teacher/courses/:id/resources` 🔨 Phase F2

**功能：**
- 左侧文件夹树（可展开/折叠）
- 右侧文件列表（图标/列表视图切换）
- 新建文件夹
- 上传文件（拖拽/点击上传，进度条）
- 下载文件（预签名URL）
- 预览文件（kkFileView iframe）
- 重命名/移动/删除文件夹和文件
- 面包屑：课程列表 > 课程名 > 资源

**状态：** ✅ 基础版已完成，后续可继续增强移动/重命名/批量操作体验

---

#### 教师-课时任务管理 `/teacher/semesters/:sid/lessons/:lid/tasks` 🔨 Phase F3

**功能：**
- 任务列表（学习单 + 课堂作品混合展示）
- 创建学习单任务：标题 + 表单设计器（radio/checkbox/text/textarea/table）
- 创建课堂作品任务：标题 + 说明 + 截止时间
- 编辑/删除任务
- 从课时详情页可导航至此

**状态：** ✅ 基础版已完成，当前主要嵌入课程详情的课时任务面板和独立任务创建页

---

#### 教师-任务数据看板 `/teacher/tasks/:taskId/analytics` 🔨 Phase F3

**功能：**
- 提交列表表格：学生姓名、学号、提交状态、提交时间
- 按班级筛选
- 学习单提交：查看答案内容（按题目展示）
- 课堂作品提交：下载/预览附件
- 实时统计面板（ECharts）：
  - 学习单：各选项统计（单选/多选用柱状图/饼图，填空/表格用列表）
  - 课堂作品：提交/未提交人数统计
- WebSocket 实时更新（新提交自动刷新统计）

**补充记录：数据看板状态口径已修正**
- 历史演示截图中，提交构成图曾将 `graded` 归在“已提交”里；数据不算错，但老师容易理解成“还没批改的已提交”。当前口径已拆分。
- 状态应拆成四类展示：
  - `not_submitted`：未提交（前端/看板派生状态，不写入 `submissions.status`）。
  - `submitted`：已提交，待批改。
  - `graded`：已评分。
  - `special`：特殊处理（缺交、无法评分、免评等）。
- 顶部指标使用：
  - 提交率：`(submitted + graded + special) / totalStudents`。
  - 待批改：`submitted` 人数。
  - 已批改：`graded` 人数。
  - 未提交：`not_submitted` 人数。
- 图表文案和图例与表格状态一致，避免同一学生在图表中显示“已提交”、表格中显示“已评分”。
- 验收标准：批改前显示“待批改 1 / 已批改 0 / 未提交 4”；批改后显示“待批改 0 / 已批改 1 / 未提交 4”。

**状态：** ✅ 状态口径已完成，页面整体仍归属任务数据看板能力

---

#### 教师-批改收件箱（课堂任务优先）🔨 Phase F3/F4

设计约束：
- 教师不应从任务、考试、项目列表直接进入某个空白批改界面；先看到该课程/学期/班级下“应完成学生”的状态清单，再选择学生批改或查看已批改详情。
- 页面保持工作台风格：顶部筛选和统计，主体是按班级分组的密集表格；状态使用文字 + 标签，不只靠颜色表达。
- 每行至少展示班级、学号、姓名、状态、提交时间、提交内容摘要和操作；未提交学生也必须出现，便于教师追踪缺交。
- 已批改学生的主操作文案为“查看/复核”，待批改为“去批改”，未提交禁用批改操作。
- 不在前端临时拼业务权限；任务收件箱复用后端 `TaskAnalyticsVO.submissions`，后端负责返回应完成学生全集。

实现计划：
1. 后端扩展 `TaskAnalyticsVO.StudentTaskAnswerVO`：允许 `submissionId` 为空，新增 `classId/className`。
2. `TaskServiceImpl.getTaskAnalytics` 将 `submissions` 从“已有提交列表”改为“应完成学生状态列表”：按学生全集左连接提交记录，未提交行状态为 `not_submitted`。
3. 前端 `TaskAnalytics.vue` 将“提交明细”升级为“批改收件箱”：按班级展示学生状态，表格加入班级、状态、提交时间、内容摘要和操作。
4. `GradingView.vue` 支持 query 参数 `submissionId`，教师从收件箱点击某个学生后，批改页定位到对应提交；已批改仍可进入复核。
5. 入口调整：任务面板和数据看板按钮优先进入数据看板/收件箱，不直接跳批改页。
6. 考试/项目：先在文档记录同样的目标形态；由于当前接口只返回已提交记录，后续需要补“应完成学生状态聚合”接口后再完整接入。
7. 测试：后端跑 `TaskServiceImplTest`；前端跑 `npm run check`。

执行结果：
- 后端 `TaskAnalyticsVO.submissions` 已调整为“应完成学生状态列表”，未提交学生也返回一行，`status='not_submitted'` 且 `submissionId/submittedAt/content` 为空。
- `StudentTaskAnswerVO` 已新增 `classId/className`，任务看板可以按班级展示完整名单。
- `TaskAnalytics.vue` 的提交明细已升级为“批改收件箱”，展示班级、学号、姓名、状态、提交时间、内容摘要和操作。
- 从收件箱点击待批改/已批改学生进入 `/teacher/grading/:taskId?submissionId=...`，批改页会定位到对应提交；未提交学生的操作禁用。
- `LessonTaskPanel.vue` 教师端任务按钮改为“批改收件箱”，避免直接跳进批改页。
- 后端顺手修复自动题漏答时 `answersEqual` 对 `null` 答案触发 NPE 的健壮性问题，漏答会判定为错误。
- 考试/项目当时仍需后续补同类“应完成学生状态聚合”接口后再完整接入；当前考试收件箱和项目收件箱都已在后续增强中完成。
- 验证：Docker Maven 缓存脚本定向测试 `TaskServiceImplTest` 6 个测试通过；`npm run check` 通过质量门、类型检查、8 个前端测试文件 30 个测试和生产构建。

---

#### 教师-评分页 `/teacher/grading/:taskId` 🔨 Phase F4

**功能：**
- 学生提交列表（含提交内容预览）
- 四维度评分选择器（每个维度 A/B/C/D/E 下拉或按钮组）
- 可选 1-4 个维度评分
- 特殊情况标记（填写原因）
- 批量评分（同分快速操作）
- 评分后状态变更（submitted → graded）

**补充记录：自动题预评分与人工题操作区已补齐**
- 批改前页面若已有自动题结果，文案使用“自动题预评分”，不要直接写“自动判题 9/9 分”造成整份任务已完成批改的误解。
- 自动题区域展示：
  - 自动题得分、正确题数、每题正确/错误标签。
  - 说明“自动题分数会随本次提交评分一起写入评价数据”。
- 人工题区域展示：
  - 按题展示学生答案、得分输入、维度拆分输入、单题评语。
  - 未填写或超出满分时阻止提交，并定位到问题题目。
- 操作区：
  - 底部固定“保存批改 / 下一份 / 返回数据看板”操作栏，避免长页面滚动后漏点提交。
  - 顶部显示评分进度，例如“自动题 2/2 已完成，人工题 0/1 待评分”。
- 验收标准：老师在 1366x768 视口下不滚到页面底部，也能看到保存批改入口；人工题未评分时不能误提交为已评分。

**状态：** ✅ 关键交互已完成，批量评分仍可作为后续效率优化

---

### 2026-07-10 当前批改与提交基线（优先于下方历史执行记录）

- `/teacher/exams` 和 `/teacher/projects` 只负责创建、编辑和列表管理；批改分别进入 `/teacher/exams/:examId/submissions`、`/teacher/projects/:projectId/submissions`。
- 两个批改页首屏统一使用 `SubmissionRoster.vue`，按班级分组展示所有应完成学生，并提供班级、状态、姓名/学号筛选；选择真实提交后再显示评分详情。
- 任务、考试、项目统一支持 `submitted / graded / returned / absent / special`，考试另有 `in_progress`。已批改内容默认只读，教师可重新批改或填写原因退回。
- 学生考试支持开始/恢复、1 秒防抖草稿、保存状态、倒计时和未保存关闭确认；`/student/tasks` 是普通课堂任务的独立待完成入口。
- 项目仅支持个人提交，保留附件和“备注 / 组员说明”。队伍 UI、队伍 API、`ProjectSubmissionModal.vue` 和 `useProjectSubmissionScoring.ts` 已删除。
- 下方 2026-06-14 的弹窗批改、队伍表和旧组件描述只保留为开发演进记录，不代表当前实现，也不得据此恢复旧架构。

#### 教师-考试管理 `/teacher/exams` 🔨 Phase F5

**功能：**
- Tab 1：试卷管理
  - 试卷列表（标题、总分、创建时间）
  - 创建试卷：题目编辑器（单选/多选/判断/简答，JSONB结构）
  - 编辑/删除试卷
- Tab 2：考试任务
  - 考试列表（名称、学期、试卷、时间、班级）
  - 创建考试：选择试卷 + 学期 + 班级 + 起止时间 + 权重
  - 编辑/删除考试

**状态：** ✅ 已完成管理页与独立名单批改页拆分，考试过程控制已包含草稿、倒计时、锁定和退回

---

#### 教师-考试提交查看 `/teacher/exams/:id/submissions` 🔨 Phase F5

**功能：**
- 提交列表：学生姓名、学号、得分、状态
- 查看答题详情（题目+学生答案+得分）
- 简答题手动评分
- 缺考/特殊情况标记

**状态：** ✅ 已完成独立路由；首屏先显示完整学生名单，选择学生后进入答卷和评分详情

##### 考试提交批改页面计划

设计原则：这是教师处理考试结果的工作界面，不做大面积展示型卡片。布局采用“左侧提交列表 + 右侧答题详情/评分”的双栏结构，便于教师逐个学生批改。

交互结构：
1. 考试列表操作列增加“提交/批改”按钮，打开当前考试的批改工作台。
2. 弹窗左侧为提交列表，显示学生姓名、学号、状态、分数、提交时间；点击切换右侧详情。
3. 右侧展示试卷题目、学生答案、题目满分和四维度得分输入；自动题也允许教师最终覆盖评分。
4. 顶部显示当前学生、状态、总分；保存按钮有加载态。
5. 支持“标记缺考”：保存后状态为 `absent`，总分为 0，不写入维度得分。
6. 空状态区分“暂无提交”和“请选择一名学生”。

复用约束：
- 复用 `parseTaskSchema/questionStem/questionTotalScore/CORE_DIMENSIONS`，不在页面里重新发明题目 schema 解析。
- 复用现有 `PUT /api/exam-submissions/{id}/grade`，通过 `dimensionScores` 写入后端 `dimension_scores(source_type='exam')`。
- 本轮先完成基础人工批改闭环；批量评分、缺考自动生成未提交记录、独立路由可后续增强。

执行结果：
- `ExamManage.vue` 考试列表已增加“提交/批改”入口。
- 批改弹窗采用左侧提交列表、右侧答题详情和逐维度评分布局；支持保存人工评分和标记缺考。
- 前端复用 `parseTaskSchema/questionStem/questionTotalScore/CORE_DIMENSIONS` 解析试卷题目，不新增第二套 schema 解析。
- 验证：`npm run check` 7 个测试文件 28 个测试通过，并完成质量门、类型检查和生产构建。

##### 考试批改收件箱增强计划

设计约束（ui-ux-pro-max）：
- 教师考试批改是高频工作台，不做展示型大卡片；主视图保持“左侧学生名单 + 右侧答题/评分”的双栏结构。
- 左侧名单必须先呈现“应完成学生全集”，并按班级、学号、姓名、状态、提交时间、分数组织；未提交学生仍出现在列表中，操作禁用或只显示状态说明。
- 状态不能只靠颜色表达，必须有明确文字：`submitted`=待批改，`graded`=已批改，`absent`=缺考，`not_submitted`=未提交。
- 右侧详情只在有真实提交或缺考记录时启用；未提交学生显示只读空状态，避免教师误保存一份不存在的提交。
- 移动端弹窗改为单列堆叠，名单在上、评分区在下；表格列保持紧凑，避免横向溢出。

执行计划：
1. 后端 `GET /api/exams/{examId}/submissions` 从“仅提交记录”升级为“应完成学生状态聚合”，返回课程绑定班级下所有学生。
2. `ExamSubmissionVO` 增加 `submissionId/classId/className` 语义字段；为兼容现有前端，真实提交行继续保留 `id`，未提交行 `id/submissionId=null`。
3. 后端复用课程归属校验和课程绑定班级查询，不在考试模块另写第二套权限模型；班级名称格式沿用任务收件箱 `年级 + 级 + 班名`。
4. 前端 `ExamManage.vue` 左侧标题改为“批改收件箱”，表格补班级、提交时间；未提交行不允许选中批改，右侧显示“该学生尚未提交考试”。
5. 保存批改后刷新收件箱并保留当前学生；已批改学生可再次进入查看/复核。
6. 测试：后端补 `ExamServiceTest` 覆盖未提交学生行、班级名、缺考/已批改状态；前端补 `ExamManage` 收件箱组件测试，覆盖未提交行、点击待批改学生和保存后刷新。

执行结果：
- 后端 `GET /api/exams/{examId}/submissions` 已升级为考试批改收件箱：按课程绑定班级返回所有应完成学生，未提交学生返回 `status='not_submitted'` 且 `id/submissionId` 为空。
- `ExamSubmissionVO` 已新增 `submissionId/classId/className`；真实提交行保留原 `id`，前端评分仍调用现有 `PUT /api/exam-submissions/{id}/grade`。
- `ExamManage.vue` 的考试提交弹窗左侧已改为“批改收件箱”，展示班级、学生、学号、状态、提交时间和分数；未提交学生可被选中查看状态，但保存批改按钮禁用。
- 未重复开发新考试批改页，继续复用原有双栏工作台、试卷 schema 解析和逐维度评分逻辑。
- 验证：Docker Maven 缓存脚本定向测试 `ExamServiceTest` 6 个测试通过；新增 `exam-manage-inbox.test.ts`，`npm run test -- exam-manage-inbox` 1 个测试通过；`npm run check` 通过质量门、类型检查、9 个前端测试文件 31 个测试和生产构建。

---

#### 教师-项目管理 `/teacher/projects` 🔨 Phase F5

**功能：**
- 项目列表（名称、学期、截止时间、提交与批改状态）
- 创建项目：名称 + 说明 + 学期 + 截止时间 + 提交方式 + 文件限制 + rubric
- 编辑/删除项目

**状态：** ✅ 基础版已完成；后端项目提交/评分权限已锁定，课程详情 `ProjectPanel` 和独立 `/teacher/projects` 页面都已接入提交查看、文件预览/下载和按 rubric 评分

---

#### 教师-项目提交与评分 🔨 Phase F5

**功能：**
- 按班级分组的应完成人员名单（姓名、学号、状态、提交时间、分数、退回次数）
- 查看个人提交作品和“备注 / 组员说明”（下载/预览）
- 按 rubric 逐维度评分、重新批改和退回修改
- 未提交、缺交和特殊情况状态展示

**状态：** ✅ 已完成个人提交口径和独立名单批改页；旧队伍功能已删除

已存在能力：
- 后端 `ProjectService` 已完成 `Project -> Semester -> Course` 课程归属校验，非任课教师不能查看提交或评分。
- 新评分入口为 `POST /api/project-submissions/{id}/score`，写入 `dimension_scores(source_type='project', source_id=submissionId)`；旧 `/api/projects/{id}/scores` 已停用。
- `ProjectPanel.vue` 已能打开项目提交列表、预览/下载作品文件，并根据项目说明中的 rubric 写入逐维度得分。
- `ProjectSubmissionModal.vue`、`ProjectCreateModal.vue`、`projects.ts` 中的 `listProjectSubmissions/scoreProjectSubmission` 可复用。

当前口径：
- 产品口径已确认：项目按个人提交评分，不做队伍同分同步；学生如有组员，在提交备注中写清组员，教师手动找到对应学生并分别评分。
- 早期规划的独立路由 `/teacher/projects/:id/teams` 不再作为当前主流程；评分能力由课程详情 `ProjectPanel` 和独立 `ProjectManage.vue` 承载。
- 项目收件箱已补齐应完成学生全集，未提交学生也出现在教师批改弹窗里。

已执行开发计划：
1. 先抽取项目提交评分复用层，建议新增 `useProjectSubmissionScoring.ts`，封装提交列表加载、作品预览/下载、rubric 分数状态、保存评分和错误提示。
2. 将 `ProjectPanel.vue` 改为使用该复用层，保持课程详情内现有体验不退化。
3. 在 `ProjectManage.vue` 的操作列增加“提交/批改”入口，复用 `ProjectSubmissionModal.vue`，保留现有编辑/删除能力。
4. 暂不做真正的 `/teacher/projects/:id/teams`，学生端隐藏创建队伍入口，保留后端历史队伍接口但不作为当前主流程。
5. 项目提交接口补应完成学生全集状态聚合，已提交/已评分/未提交在同一收件箱展示。

口径说明：以上 1-5 已完成；队伍同分同步和独立队伍页按当前产品决策保留为暂不开发。

本轮执行计划：
1. 新增 `useProjectSubmissionScoring.ts`，只抽取与项目提交批改相关的状态和方法：当前项目、提交列表、rubric、文件预览、下载、维度分输入、保存评分。
2. `ProjectPanel.vue` 删除重复的提交评分状态和方法，继续负责课程详情里的项目列表、创建和删除。
3. `ProjectManage.vue` 在现有表格操作列补“提交/批改”按钮，复用同一个 `ProjectSubmissionModal` 和预览弹窗，不新增第二套 UI。
4. 暂不实现队伍同分同步和 `/teacher/projects/:id/teams` 独立页面；当前项目按个人提交和个人评分闭环。
5. 验证以 `npm run check` 为准，确保类型检查、组件测试和构建通过。

执行结果：
- 已新增 `useProjectSubmissionScoring.ts`，项目提交列表、作品预览/下载、rubric 评分保存逻辑由 `ProjectPanel.vue` 和 `ProjectManage.vue` 共用。
- `ProjectManage.vue` 已新增“查看提交和批改”入口，并复用 `ProjectSubmissionModal`，没有新增第二套评分 UI。
- `ProjectManage.vue` 创建/编辑项目已改用 `ProjectCreateModal`，支持提交方式、文件后缀和项目 rubric。
- 验证：`npm run check` 通过质量门、类型检查、7 个前端测试文件 28 个测试和生产构建。

#### 项目批改收件箱增强计划

设计约束（ui-ux-pro-max）：
- 项目批改继续使用现有 `ProjectSubmissionModal` 工作台，不新建 `/teacher/projects/:id/teams` 页面，不恢复队伍同分流程。
- 左侧列表/表格必须展示课程绑定班级下所有应完成学生，列包含班级、学号、姓名、状态、提交时间、备注摘要和操作反馈。
- 状态必须用文字 + 标签：`not_submitted`=未提交，`submitted`=待评分，`graded`=已评分；未提交学生不能触发保存评分和文件预览。
- 已提交学生继续展示文件/文件夹、学生备注 / 组员说明、rubric 维度评分输入；已评分学生可再次复核保存。
- 不新增数据库字段；项目是否已评分从 `dimension_scores(source_type='project', source_id=submissionId)` 判断。

实现计划：
1. 后端 `GET /api/projects/{projectId}/submissions` 从“仅提交记录”升级为项目批改收件箱：按 `Project -> Semester -> Course` 读取课程绑定班级所有学生。
2. `ProjectSubmissionVO` 增加 `submissionId/classId/className/status/score`：未提交行 `id/submissionId/content/submittedAt/score=null` 且 `status=not_submitted`。
3. 后端复用 `ProjectService` 的课程归属校验和 `CourseClassMapper`，新增 `SchoolClassMapper/UserMapper/DimensionScoreMapper` 聚合收件箱，不在控制器临时拼数据。
4. 前端 `useProjectSubmissionScoring` 和 `ProjectSubmissionModal` 支持未提交行：解析内容时允许空内容，保存评分前检查 `submissionId`，未提交行显示“尚未提交项目作品”。
5. 前端表格补班级、状态、提交时间和备注摘要；已提交/已评分继续复用文件预览、下载和 rubric 评分。
6. 测试：后端补 `ProjectServiceTest` 覆盖未提交行和已评分状态；前端补项目收件箱组件/组合式测试，覆盖未提交行禁用评分。

执行结果：
- 后端 `GET /api/projects/{projectId}/submissions` 已升级为项目批改收件箱：按课程绑定班级返回所有应完成学生，未提交学生返回 `status='not_submitted'` 且 `id/submissionId` 为空。
- `ProjectSubmissionVO` 已新增 `submissionId/classId/className/status/score`；项目是否已评分从 `dimension_scores(source_type='project', source_id=submissionId)` 汇总判断。
- 前端继续复用 `useProjectSubmissionScoring` 和 `ProjectSubmissionModal`，未新建项目评分页；未提交学生显示“尚未提交项目作品”并不渲染文件预览和评分控件。
- 已提交/已评分学生继续使用原有作品预览、下载、备注展示和 rubric 评分保存链路。
- 验证：Docker Maven 缓存脚本定向测试 `ProjectServiceTest` 8 个测试通过；新增 `project-submission-modal-inbox.test.ts`，`npm run test -- project-submission-modal-inbox` 1 个测试通过；`npm run check` 通过质量门、类型检查、10 个前端测试文件 32 个测试和生产构建。

#### 项目个人提交备注计划

设计约束：
- 项目主流程改为个人提交，不在学生端展示组队/创建队伍入口，避免学生误以为系统会自动按队伍同步评分。
- 备注是提交表单内的显式字段，标签写清“备注 / 组员说明”，帮助学生把协作成员、链接或补充说明写在同一个地方。
- 教师端只读展示备注，位置靠近学生姓名和提交时间，标题明确为“学生备注 / 组员说明”，不参与 rubric 分数计算。
- 继续复用现有 `ProjectSubmissionContent.note` JSON 结构，不新增数据库列，不新建第二套项目提交 DTO。

实现计划：
1. `ProjectView.vue` 删除学生端创建队伍按钮和组队弹窗，把页面副标题、项目卡片 meta 和提交弹窗文案改为个人提交口径。
2. `ProjectView.vue` 提交弹窗的备注输入增加可见标签和 helper 文案：“如有组员，请写清姓名/学号；教师会据此手动查找并评分”。
3. `ProjectSubmissionModal.vue` 将备注展示从普通正文升级为只读信息块，标题固定为“学生备注 / 组员说明”，便于教师批改时快速看到。
4. 测试：运行 `npm run check`，确认类型、现有项目提交/批改组件和生产构建通过。

执行结果：
- `ProjectView.vue` 已隐藏学生端创建队伍入口，页面副标题和项目卡片 meta 改为个人提交口径。
- 学生提交弹窗保留文件/文件夹上传，并新增显式“备注 / 组员说明”字段；提交内容继续写入 `ProjectSubmissionContent.note`。
- `ProjectSubmissionModal.vue` 已将备注展示为“学生备注 / 组员说明”只读信息块，教师批改时可直接看到学生填写的组员和补充说明。
- 后端接口和数据库未改动；当前复用已有 `content` JSON 保存备注，保持个人提交评分链路不变。
- 验证：`npm run check` 通过质量门、类型检查、7 个前端测试文件 28 个测试和生产构建。

---

#### 教师-班级数据分析 `/teacher/stats` 🔨 Phase 5b

**功能：**
- 筛选：课程 + 学期 + 班级
- 指标卡片：平均过程评价分、平均结果评价分、平均总评分
- 四维度平均分柱状图（多班级对比）
- 学习单完成率 / 作品提交率
- 考试平均分 / 项目平均分
- 优秀率 / 合格率
- 班级四维度雷达图对比（ECharts radar，多条叠加）

**状态：** ✅ 基础产品化已完成，当前复用学期总评预览接口做班级聚合；后续可继续增强趋势对比、导出和下钻

页面设计计划：
1. 页面定位是教师复盘班级表现的工作台，采用紧凑的筛选栏、指标摘要、班级对比表和雷达图，不做展示型 hero。
2. 数据来源优先复用 `GET /api/stats/semester/{id}/preview`，前端按 `className` 聚合；不新增第二套学期总评计算逻辑。
3. 筛选区包含课程、学期、班级；班级筛选只影响当前页面的图表和明细，不先改全局 `TeacherLayout` 筛选范围。
4. 摘要指标包括学生数、完整总评人数、缺失数据人数、平均总评、优秀率、合格率。
5. 班级对比表按班级聚合四维度均分、过程分、考试分、项目分、结果分、总评、优秀率、合格率和缺失人数。
6. 图表区复用 `RadarChart.vue` 展示选中班级或全体学生的四维度平均；明细表复用 `NDataTable` 展示学生成绩，移动端保持横向滚动。
7. 验证以 `npm run check` 为准；如仅复用现有 stats API，不需要新增后端测试。

执行结果：
- 新增 `/teacher/stats` 路由和 `ClassStats.vue` 页面，教师侧边栏新增“数据分析”入口。
- 页面复用 `useCourseSemesterPicker`、`getSemesterStatsPreview` 和 `RadarChart.vue`，不新增后端统计接口。
- 支持课程/学期/班级筛选，展示学生数、完整总评、缺失数据、平均总评、优秀率、合格率。
- 班级对比表按班级聚合四维度均分、过程分、考试分、项目分、结果分、总评、优秀率、合格率和缺失人数；学生明细表可查看每名学生的总评与缺失备注。
- 验证：`npm run check` 通过质量门、类型检查、7 个前端测试文件 28 个测试和生产构建。

---

#### 教师-查看学生网盘 `/teacher/students/:id/drive` 🔨 Phase F6

**功能：**
- 网盘文件树（只读浏览）
- 文件预览（kkFileView）
- 文件下载
- **不可：** 上传、删除（需审计日志的写操作暂不开放）

**状态：** ✅ 基础版已完成，当前通过教师学生页的网盘弹窗查看；独立路由可后续补齐

---

#### 教师-学期总评导出 `/teacher/grade-export` 🔨 Phase F6

**功能：**
- 选择课程 + 学期
- 总评预览表格（学号、姓名、班级、四维度分、过程分、结果分、总评、等级）
- 导出 Excel 按钮（按班级分 Sheet）
- 导出进度反馈

**状态：** ✅ 基础产品化已完成：独立 `/teacher/grade-export` 页面已支持课程/学期筛选、摘要指标、缺失数据提示、完整预览表格和导出加载态

##### 产品化计划

> `/teacher/export` 为早期规划名；当前已统一使用 `/teacher/grade-export`。

设计原则：该页面是教师课后整理成绩的工作台，采用密集但清晰的表格布局；避免营销式 hero、装饰性卡片堆叠和过大的留白。教师需要快速判断“是否可以导出正式总评”“哪些学生缺数据”“导出的表格包含哪些字段”。

页面结构：
1. 顶部 `PageHeader` 保持现有教师端一致性，主操作为“导出 Excel”，按钮需要加载态，未选择学期或正在加载时禁用。
2. 筛选区使用课程、学期两个选择器；课程未选时禁用学期选择器，切换学期立即刷新预览。
3. 摘要区显示学生数、可生成总评人数、缺失数据人数、班级平均总评；摘要用于导出前快速确认数据质量。
4. 缺失数据提示区只在存在 `remark` 时显示，按缺平时任务/考试/项目成绩聚合人数，提醒教师先补齐评分。
5. 明细区使用 `NDataTable`，列包括班级、学号、姓名、四维度分、过程分、考试分、项目分、结果分、总评、等级、备注；数值统一保留 1 位小数，缺失显示 `-`。
6. 空状态区区分“未选择课程/学期”和“已选择但暂无评价数据”，避免教师误以为导出失败。
7. 移动端保持表格横向滚动，筛选器纵向堆叠，不让按钮和选择器挤压变形。

复用约束：
- 复用 `useCourseSemesterPicker`、`getSemesterStatsPreview`、`exportSemesterStats`、`getErrorMessage` 和 `PageHeader`。
- 不新增第二套总评计算逻辑；前端只展示后端 `GradeRow` 字段。
- 与课程详情页内的 `ExportPanel` 暂时共存，独立 `/teacher/grade-export` 作为完整产品化页面。

执行结果：
- `GradeExport.vue` 已改为表格型工作台，展示班级、学号、姓名、四维度分、过程分、考试分、项目分、结果分、总评、等级和备注。
- 摘要指标包含学生数、可生成总评人数、缺失数据人数、完整总评学生的平均总评；缺失数据按 `remark` 聚合提示。
- `SemesterStatsPreviewRow` 类型已与后端 `GradeRow` 对齐。
- 验证：`npm run test -- grade-export-view` 2 个测试通过；`npm run check` 7 个测试文件 28 个测试通过，并完成类型检查和生产构建。

---

#### 学生-我的课程 `/student/home`

**功能：**
- 响应式3列卡片网格（只读）
- 每张卡片：首字母封面、名称、教师名、描述、班级数
- 点击进入课程详情
- 分页
- 空状态：提示联系教师分配课程

**状态：** ✅ 已完成

---

#### 学生-课程详情 `/student/courses/:id`

**功能：**
- 面包屑：返回课程列表
- 课程头部：名称、描述、授课教师、班级标签
- 学期 Tab：学期列表（名称、日期、课时数，只读）
- 课时 Tab：学期选择器 + 课时表格（序号、名称，只读）

**状态：** ✅ 已完成

**补充：** 课程资源浏览已通过独立路由 `/student/courses/:courseId/resources` 承载，课程详情页保持课程/学期/课时主流程。

---

#### 学生-课程资源 `/student/courses/:id/resources` 🔨 Phase F2

**功能：**
- 文件夹树浏览（只读）
- 文件下载
- 文件预览（kkFileView）
- **不可：** 上传、新建文件夹、删除、重命名

**状态：** ✅ 基础版已完成

---

#### 学生-课时详情 `/student/lessons/:lid` 🔨 Phase F3

**功能：**
- 课时信息：名称、所属学期、序号
- 课时资源列表（下载/预览）
- 本课时任务入口：
  - 待完成学习单 → 填写页
  - 待提交作品 → 提交页
  - 已提交 → 查看提交状态
- 截止时间倒计时/逾期标记

**状态：** ✅ 基础版已完成，当前由学生课程详情的课时任务面板承载任务入口；独立课时详情页可后续增强

---

#### 学生-学习单填写 `/student/tasks/:id` 🔨 Phase F3

**功能：**
- 根据 form_schema 动态渲染表单
  - radio → 单选按钮组
  - checkbox → 多选框组
  - text → 文本输入框
  - textarea → 多行文本
  - table → 动态表格（按列定义渲染）
- 临时保存草稿（localStorage）
- 提交按钮（截止前可提交/修改，截止后禁用）
- 已评分后显示评分结果（只读）

**补充待办：批改详情入口**
- 当任务状态为 `graded` 时，学习单页不再只显示“已评分”，需要提供“查看批改详情”入口。
- 入口位置：
  - 学生首页“最近评分”卡片。
  - 课时任务列表的已评分任务行。
  - 学习单提交成功/只读页顶部状态区。
- 点击后进入 `/student/tasks/:taskId/result`，或在当前 `/student/tasks/:taskId` 下切换为结果视图；实现时优先独立路由，便于从首页、课时详情、评价页统一跳转。

**状态：** ✅ 基本完成，批改详情入口已接入；草稿保存可作为后续增强

---

#### 学生-作品提交 `/student/tasks/:id` (artifact类型) 🔨 Phase F3

**功能：**
- 任务说明展示
- 文件上传（拖拽/点击，进度条）
- 已上传文件列表（可删除重传，截止前）
- 提交按钮
- 截止后禁用上传和提交

**状态：** ✅ 基础版已完成，与学习单填写页共用 `TaskSubmit.vue`

---

#### 学生-我的考试 `/student/exams` 🔨 Phase F5

**功能：**
- 考试列表（名称、时间、状态：即将开始/进行中/已结束）
- 进行中的考试：点击进入答题
- 已结束：查看成绩

**状态：** ✅ 基础版已完成，后续可增强考试倒计时、自动交卷和成绩详情

---

#### 学生-考试答题 `/student/exams/:id` 🔨 Phase F5

**功能：**
- 计时器（倒计时，时间到自动提交）
- 题目渲染（单选/多选/判断/简答）
- 题目导航（快速跳转）
- 提交确认对话框
- 已提交后显示只读状态

**状态：** ✅ 基础版已完成，当前在 `/student/exams` 内弹窗答题；独立答题路由可后续增强

---

#### 学生-我的项目 `/student/projects` 🔨 Phase F5

**功能：**
- 项目列表（名称、截止时间、提交状态、成绩）
- 个人提交：上传项目作品并填写“备注 / 组员说明”
- 截止前可修改待批改或已退回的提交；已批改、缺考、特殊处理状态锁定
- 已退回时显示原因；已批改时查看总分和四个维度得分

**状态：** ✅ 已完成

---

#### 学生-项目详情 `/student/projects/:id` 🔨 Phase F5

**功能：**
- 项目说明
- 作品提交：文件上传 + 备注 / 组员说明
- 查看提交状态、退回原因和修订次数
- 查看只读批改结果；教师退回后允许修改重交

**状态：** ✅ 已完成，当前主流程在 `/student/projects` 内以弹窗/详情面板呈现；项目仅按个人提交和个人评分处理

---

#### 学生-批改详情 `/student/tasks/:taskId/result` 🔨 Phase F4 补充

**目标：**
- 学生能看见“老师具体怎么批”的结果，而不只是首页的 `已评分` 标签或评价页的汇总雷达图。

**展示内容：**
- 任务概览：任务标题、所属课程/课时、提交时间、批改时间、总得分/满分、状态。
- 总评区：过程评价维度汇总，显示四个核心素养维度的得分、满分、得分率和等级。
- 逐题明细：
  - 题干、题型、学生答案、参考答案/标准答案（仅自动题或教师允许展示时显示）。
  - 自动题：正确/错误、自动得分、对应维度得分。
  - 人工题：教师给出的题目得分、维度拆分得分、教师评语。
  - 未评分题：明确显示“等待教师批改”，不展示空分数。
- 教师反馈：总评语、改进建议、特殊情况标记（缺交、无法评分等）。
- 导航：返回课时、返回我的评价、查看同课程其他已评分任务。

**交互与状态：**
- `not_submitted`：显示任务基本信息和“尚未提交”；若任务未逾期，主按钮进入填写/作品提交页，若已逾期则只保留返回课时。
- `submitted`：显示只读答案和“待教师批改”；隐藏人工分数，自动题如已预评分，必须标注“自动题预评分”，不能写成已批改。
- `graded`：完整展示总分、逐题得分、维度拆分、教师总评和逐题评语。
- `special`：显示特殊处理原因/总评，明确“不计入评价”；不展示维度汇总为正常得分。
- 接口失败或权限不足：提示“只能查看自己的提交批改详情”，不得渲染任何已返回的提交内容。
- 网络错误：保留页面框架和重试按钮，返回课时/首页入口必须可用。

**页面分区：**
- 顶部导航区：返回按钮、课程/课时上下文、任务标题、状态标签。
- 概览区：提交时间、批改时间、总得分/满分、状态说明；移动端改为单列堆叠。
- 教师反馈区：整份任务总评语、特殊处理说明；没有评语时显示温和空状态，不出现空白卡片。
- 维度汇总区：四个核心素养维度的得分、满分、得分率、等级；无评分时整体隐藏。
- 逐题明细区：题干、学生答案、参考答案、自动/人工标记、题目得分、题目评语、维度拆分。
- 底部操作区：返回课时、返回我的评价；未提交且未逾期时提供“去完成任务”。

**实现要点：**
- 新增路由：`/student/tasks/:taskId/result`，页面组件建议 `TaskResult.vue`。
- 首页最近评分、课时任务列表、提交成功页、学习评价页都统一跳转到该路由。
- `tasks.ts` 新增 `getMyTaskResult(taskId)` API 封装，并定义 `TaskResultVO`、`QuestionResultVO`、`DimensionScoreResultVO` 类型。
- 复用 `MarkdownView.vue` 渲染题干、学生答案和参考答案。
- 逐题得分建议复用教师批改页的题目解析逻辑，但学生端只读且不暴露其他学生信息。
- 结果页不能自己拼接其他学生提交 ID；只调用 `GET /api/tasks/{taskId}/my-result`。
- 学习评价页任务明细必须有可跳转的 `taskId`；若当前评价接口只返回维度汇总，需要补充 `taskResults` 或等价列表字段。
- 展示参考答案时遵守 `referenceAnswerVisible`，人工题默认不展示标准答案，除非教师在逐题反馈中允许。

**验收标准：**
- 学生首页最近评分点击后进入批改详情，而不是只停留在首页标签。
- 课时任务列表中已评分任务按钮文案为“详情/查看批改详情”，待批改任务仍显示提交状态。
- 学习单提交成功页提供“查看提交状态/批改结果”入口。
- 学习评价页每条已评分任务可进入同一个批改详情页。
- 已批改任务能看到总分、逐题得分、维度得分和教师评语。
- 待批改任务只能看到自己的答案和“待教师批改”，不能看到空分数或其他同学信息。
- 学生手动访问不存在、未授权或其他学生的结果地址时，显示权限提示并不泄露提交内容。
- 375px 移动端、1366x768 桌面端无横向滚动，按钮文本不溢出。

**状态：** ✅ 核心闭环已完成

---

#### 学生-我的评价 `/student/evaluation` 🔨 Phase F4

**功能：**
- 当前学期四维度分数卡片
- 学期雷达图（ECharts radar）
- 进步雷达图（双学期叠加对比）
- 各任务评分详情列表：每条已评分任务可进入批改详情
- 过程评价分 / 结果评价分 / 学期总评

**补充待办：评价页默认上下文**
- 演示流程中，学生首页能看到最近评分，但学习评价页需要手动选择上下文，学生容易以为没有数据。
- 进入页面时自动选择：
  - 优先选择最近评分所在课程/学期。
  - 其次选择当前学生唯一可见课程/当前学期。
  - 多课程多学期时保留选择器，但默认选中最近有评价数据的一组。
- 空状态区分：
  - 没有评分数据：提示“暂无已批改任务”。
  - 有任务但待批改：提示“已有提交，等待教师批改”。
  - 接口失败：显示重试按钮。
- 验收标准：完成一次批改后，学生进入“学习评价”无需额外选择即可看到维度汇总和可点击的任务评分明细。

**状态：** ✅ 基本完成，默认上下文和批改详情入口已接入；进步雷达/学期总评可继续增强

---

#### 学生-我的网盘 `/student/drive` 🔨 Phase F6

**功能：**
- 左侧文件夹树（个人文件管理）
- 右侧文件列表（图标/列表视图切换）
- 新建文件夹
- 上传文件（拖拽/点击，进度条）
- 下载文件
- 预览文件（kkFileView）
- 删除文件/文件夹
- 容量使用显示（已用/总容量 1GB 进度条）

**状态：** ✅ 基础版已完成，后续可增强容量统计、批量操作和更细的文件审计

---

## 三、布局与导航设计

### 3.1 AdminLayout

```
┌─────────────────────────────────────────────┐
│  顶栏：课堂管理 │ 管理员工作台   [主题] [退出] │
├──────────┬──────────────────────────────────┤
│ 侧边栏   │                                  │
│          │        <router-view />            │
│ ● 课堂管理 │                                  │
│   班级管理  │                                  │
│   教师管理  │                                  │
│   学生管理  │                                  │
│          │                                  │
│ [用户信息] │                                  │
│ Tatakai   │                                  │
└──────────┴──────────────────────────────────┘
```

**状态：** ✅ 已完成

### 3.2 TeacherLayout（重新设计）

当前教师布局只有顶栏，缺少导航。**重新设计为侧边栏+顶栏布局**，与管理员布局结构统一。

```
┌────────────────────────────────────────────┐
│  顶栏：课堂管理 / 教师工作台    [班级选择] [主题] [退出] │
├─────────┬──────────────────────────────────┤
│ 侧边栏   │                                  │
│         │        <router-view />            │
│ 📋 工作台  │                                  │
│ 📚 课程管理 │                                  │
│ 📝 考试管理 │                                  │
│ 🎯 项目管理 │                                  │
│ 📊 数据分析 │                                  │
│ 📤 成绩导出 │                                  │
│         │                                  │
│ [用户信息] │                                  │
│ Tatakai  │                                  │
└─────────┴──────────────────────────────────┘
```

**侧边栏菜单（随阶段迭代激活）：**

| 菜单项 | 路由 | 激活阶段 |
|--------|------|----------|
| 工作台 | `/teacher/home` | F0（已完成） |
| 课程管理 | `/teacher/courses` | F1（已完成） |
| 考试管理 | `/teacher/exams` | F5 |
| 项目管理 | `/teacher/projects` | F5 |
| 数据分析 | `/teacher/stats` | 5b |
| 成绩导出 | `/teacher/grade-export` | F6 |

**顶栏班级选择器：** 选择后作为全局筛选条件（通过 Pinia store），影响课程列表、任务列表、统计数据的班级过滤。

**状态：** ✅ 已完成（侧边栏 NMenu + 顶栏，div flex 布局，min-height:100vh）

### 3.3 StudentLayout（重新设计）

**状态：** ✅ 已完成（侧边栏 NMenu + 顶栏，div flex 布局，min-height:100vh）

```
┌────────────────────────────────────────────┐
│  顶栏：信息科技课堂        [主题] [退出]    │
├─────────┬──────────────────────────────────┤
│ 侧边栏   │                                  │
│         │        <router-view />            │
│ 📚 我的课程 │                                  │
│ 📝 我的考试 │                                  │
│ 🎯 我的项目 │                                  │
│ 📊 我的评价 │                                  │
│ 💾 我的网盘 │                                  │
│         │                                  │
└─────────┴──────────────────────────────────┘
```

**侧边栏菜单（随阶段迭代激活）：**

| 菜单项 | 路由 | 激活阶段 |
|--------|------|----------|
| 我的课程 | `/student/home` | F1（已完成） |
| 我的考试 | `/student/exams` | F5 |
| 我的项目 | `/student/projects` | F5 |
| 我的评价 | `/student/evaluation` | F4 |
| 我的网盘 | `/student/drive` | F6 |

**状态：** ✅ 已完成（侧边栏 NMenu + 顶栏，div flex 布局，min-height:100vh）

---

## 四、共享组件与工具函数

### 4.1 需要抽取的共享组件

已创建 `components/` 目录。已完成和待创建的共享组件：

| 组件 | 用途 | 状态 |
|------|------|------|
| `CourseCard.vue` | 课程卡片（封面、名称、描述、操作按钮插槽） | ✅ 已创建 |
| `PageHeader.vue` | 页面标题栏（标题+描述+操作按钮插槽） | ✅ 已创建 |
| `FileUpload.vue` | 文件上传（拖拽/点击、进度条、文件类型校验） | ✅ Phase F2 |
| `FilePreview.vue` | kkFileView iframe 预览 | ✅ Phase F2 |
| `FileTree.vue` | 文件夹树组件（展开/折叠、右键菜单） | ✅ Phase F2 |
| `RadarChart.vue` | ECharts 雷达图封装 | ✅ Phase F4 |

### 4.2 需要抽取的工具函数

| 文件 | 函数 | 用途 |
|------|------|------|
| `@/utils/date.ts` | `formatDate(date, format?)` | 统一日期格式化（用 dayjs） |
| `@/utils/date.ts` | `formatDateRange(start, end)` | 日期范围格式化 |
| `@/utils/validation.ts` | `validateFileSize(file, maxMb)` | 文件大小校验 |
| `@/utils/validation.ts` | `validateFileType(file, allowedTypes)` | 文件类型校验 |

### 4.3 需要创建的 Pinia Store

| Store | 用途 |
|-------|------|
| `useAuthStore` (已有) | 用户认证状态 |
| `useThemeStore` (已有) | 深色/浅色主题 |
| `useClassFilterStore` | 教师端全局班级筛选（TeacherLayout 班级选择器联动） |
| `useSemesterStore` | 当前选中学期（跨页面共享） |

### 4.4 需要创建的 Composables

| Composable | 用途 |
|------------|------|
| `useWebSocket` | WebSocket 连接/断开管理、自动重连、消息订阅 |
| `useFileUpload` | 文件上传逻辑封装（预签名URL获取、进度、错误处理） |

---

## 五、路由结构规划（完整版）

```typescript
// 最终完整路由结构

/login                              // LoginView

/admin                              // AdminLayout (sidebar)
  /admin/classes                    // ClassManage
  /admin/teachers                   // TeacherManage
  /admin/students                   // StudentManage
  /admin/                           // redirect → /admin/classes

/teacher                            // TeacherLayout (sidebar + header)
  /teacher/home                     // TeacherHome
  /teacher/courses                  // CourseList
  /teacher/courses/:id              // CourseDetail
  /teacher/courses/:id/resources    // CourseResources (Phase F2)
  /teacher/semesters/:sid/lessons/:lid/tasks  // LessonTasks (Phase F3)
  /teacher/tasks/:taskId/analytics  // TaskAnalytics (Phase F3)
  /teacher/grading/:taskId          // TaskGrading (Phase F4)
  /teacher/exams                    // ExamManage (Phase F5)
  /teacher/exams/:id/submissions    // ExamSubmissions (Phase F5)
  /teacher/projects                 // ProjectManage (Phase F5)
  /teacher/projects/:id/teams       // ProjectTeams (Phase F5)
  /teacher/stats                    // ClassStats (Phase 5b)
  /teacher/students/:id/drive       // StudentDriveView (Phase F6)
  /teacher/grade-export             // GradeExport (Phase F6)
  /teacher/                         // redirect → /teacher/home

/student                            // StudentLayout (sidebar)
  /student/home                     // StudentCourseList (HomeView)
  /student/courses/:id              // StudentCourseDetail
  /student/courses/:id/resources    // StudentCourseResources (Phase F2)
  /student/lessons/:lid             // StudentLessonDetail (Phase F3)
  /student/tasks/:id                // TaskSubmit (Phase F3)
  /student/exams                    // StudentExamList (Phase F5)
  /student/exams/:id                // StudentExamTake (Phase F5)
  /student/projects                 // StudentProjectList (Phase F5)
  /student/projects/:id             // StudentProjectDetail (Phase F5)
  /student/evaluation               // StudentEvaluation (Phase F4)
  /student/tasks/:taskId/result     // TaskResult / 学生批改详情 (Phase F4 补充)
  /student/drive                    // StudentDrive (Phase F6)
  /student/                         // redirect → /student/home

/*                                  // catch-all → /login
```

---

## 六、分阶段实现计划（更新版）

### Phase F0 — 脚手架 + 登录 + 三套 Layout ✅

- [x] Vite + Vue 3 + TypeScript 项目
- [x] Naive UI 主题系统
- [x] Axios 封装
- [x] 登录页
- [x] 路由守卫
- [x] AdminLayout / TeacherLayout / StudentLayout
- [x] Pinia auth + theme store
- [x] API 模块基础
- [x] TypeScript 类型定义

### Phase F1 — 管理员端 + 教师课程页（当前阶段）

- [x] 管理员：班级管理
- [x] 管理员：教师管理
- [x] 管理员：学生管理
- [x] 教师：课程列表 + 新建/编辑
- [x] 教师：课程详情（学期Tab + 课时Tab）
- [x] 学生：课程列表（只读）
- [x] 学生：课程详情（只读学期+课时）
- [x] TeacherLayout 侧边栏导航（NMenu：工作台 + 课程管理）
- [x] StudentLayout 侧边栏导航（NMenu：我的课程）
- [x] 三套布局 `min-height:100vh`（div flex 方案）
- [x] 共享组件：`CourseCard.vue`、`PageHeader.vue`
- [x] 工具函数：`utils/date.ts`（dayjs）
- [x] 教师管理：删除按钮（含后端 `DELETE /api/teachers/{id}`）
- [x] **修复：** TeacherLayout 班级选择器接入全局过滤（useClassFilterStore + 后端 classId 筛选）

### Phase F2 — 文件组件 + 课程资源（依赖 Phase 3b 后端）

- [x] 共享组件：`FileUpload.vue`、`FilePreview.vue`、`FileTree.vue`
- [x] 教师：课程资源管理页（文件夹树 + 上传/下载/预览）
- [x] 学生：课程资源浏览页（只读）
- [x] 课程封面图片上传

#### 课程封面图片上传计划

设计约束：
- 课程封面属于课程基础资料，不进入课程资源文件树，避免资源管理里混入系统图片。
- 创建课程时还没有 `courseId`，因此不能直接复用要求 `courseId` 的课程资源上传接口。
- 不把 MinIO 预签名 GET URL 写入 `coverUrl`，因为预签名 URL 会过期；`coverUrl` 必须是稳定可访问地址。
- 表单内展示小尺寸预览、文件名/上传进度和错误反馈；按钮使用明确文本与图标，移动端纵向堆叠。

实现计划：
1. 后端新增课程封面上传接口：`POST /api/files/course-cover/upload`，仅 `ADMIN/TEACHER` 可用，限制 `image/*` 且大小不超过 5MB。
2. 后端上传到 MinIO `course-covers/yyyy-MM/...`，返回稳定封面地址 `GET /api/files/course-cover/{token}`；该读取接口公开只读，便于 `<img>` 直接加载。
3. 前端 `courses.ts` 增加 `uploadCourseCover(file)`；`CourseList.vue` 创建/编辑弹窗增加封面上传、预览、清除和 URL 兜底展示。
4. `CourseCard.vue` 优先渲染 `course.coverUrl` 图片，加载失败时回退首字母封面，固定 48x48 尺寸避免布局跳动。
5. 测试：后端新增封面上传服务单测；前端运行 `npm run check`；后端用 Docker Maven 缓存脚本跑相关测试。

执行结果：
- 后端已新增 `POST /api/files/course-cover/upload` 和 `GET /api/files/course-cover/{token}`，课程封面独立存放到 MinIO `course-covers/yyyy-MM/...`，不污染课程资源树。
- 上传接口仅允许 `ADMIN/TEACHER`，限制 jpg/png/webp/gif 等图片类型且最大 5MB；读取接口返回稳定相对地址，供课程卡片 `<img>` 直接展示。
- 前端课程创建/编辑弹窗已增加封面上传、预览、清除和错误提示；`CourseCard.vue` 已支持封面加载失败回退首字母占位。
- 类型边界已收紧：普通课程资源上传仍要求 `resourceId`，课程封面上传只返回 `url`，避免把封面误当作课程资源附件。
- 验证：`scripts/backend-test-docker.ps1 -Test FileServiceImplTest` 通过 4 个后端单测；`npm run check` 通过质量门、类型检查、7 个前端测试文件 28 个测试和生产构建。

### Phase F3 — 课堂任务 + 实时统计（依赖 Phase 4 后端）

- [x] 教师：课时任务管理页基础版（课程详情课时任务面板 + 独立任务创建页）
- [x] 教师：任务数据看板基础版（提交列表 + 统计图表；WebSocket 实时刷新可继续增强）
- [x] 教师：数据看板状态口径拆分（待批改/已批改/未提交/特殊处理）
- [x] 学生：课时任务入口基础版（当前由课程详情承载，独立课时页可继续增强）
- [x] 学生：学习单填写页（JSON Schema 动态表单）
- [x] 学生：作品提交页基础版（与 `TaskSubmit.vue` 共用提交能力）
- [x] Composable：`useRealtime`、`useFileUpload`（WebSocket 封装名称为 `useRealtime`）

### Phase F4 — 评分 + 雷达图（依赖 Phase 5 后端）

- [x] 教师：任务评分页（四维度 A-E 选择器 + 特殊情况标记）
- [x] 教师：批改页补“自动题预评分”文案、人工题校验、底部固定保存操作栏
- [x] 学生：我的评价页（分数卡片 + 学期雷达图 + 任务批改明细；进步雷达继续增强）
- [x] 学生：批改详情页（逐题答案、得分、维度拆分、教师评语、只读展示）
- [x] 学生：首页/课时详情/学习评价页增加“查看批改详情”跳转入口
- [x] 学生：学习评价页默认选择可见课程和当前/首个学期
- [x] 共享组件：`RadarChart.vue`
- [x] Store：`useSemesterStore`

#### `useSemesterStore` 全局学期状态计划

设计约束：
- 教师端多页面已经有一致的课程/学期筛选工具栏，继续沿用现有 `NSelect` 形态，不新增全局弹窗或占位卡。
- Store 只管理“当前教师上下文中的课程与学期选择”，不接管课程详情页内部的学期 CRUD，避免编辑页和跨页面筛选状态耦合过深。
- 页面仍通过 `useCourseSemesterPicker` 获取 `courseOptions/semesterOptions/loading`，但该 composable 内部改为复用 Pinia store，避免每个页面重复保存 `activeCourseId/activeSemesterId`。
- 切换课程时自动加载该课程学期，并优先选择当前日期所在学期；没有当前学期时选择第一个学期；原选中课程/学期仍存在时优先保留。

实现计划：
1. 新增 `frontend/src/stores/semester.ts`，封装 `courses/semesters/activeCourseId/activeSemesterId/loading` 和 `loadCourses/selectCourse/loadSemesters/selectSemester`。
2. 改造 `useCourseSemesterPicker.ts` 为 store 适配层，保持现有返回字段不变，避免 `GradeExport/ExamManage/ProjectManage/ClassStats` 等页面逐个重写。
3. Store 内统一处理课程重新加载、学期清空、当前学期选择和错误前的 loading 收尾，减少页面层重复 watch。
4. 测试：运行 `npm run check`，确认类型、现有组件和生产构建都通过。

执行结果：
- 已新增 `useSemesterStore`，统一保存教师端跨页面课程/学期列表、当前课程、当前学期和 loading 状态。
- `useCourseSemesterPicker` 已改为兼容层，现有 `GradeExport/ExamManage/ProjectManage/ClassStats` 等页面不用改模板即可共享当前选择。
- 切换课程时会重新加载学期，并优先保留仍然存在的学期；否则选当前日期所在学期，再兜底第一个学期。
- 验证：`npm run check` 通过质量门、类型检查、7 个前端测试文件 28 个测试和生产构建。

### Phase 8 UX 补齐 — 批改详情闭环（当前优先级）

> 开发约束：制作新页面前，先用 `ui-ux-pro-max` 完成页面信息架构和交互设计，再写代码；写代码前先写本次任务 plan，优先复用现有 `MarkdownView.vue`、`RadarChart.vue`、`tasks.ts`、`useStudentContext` 等能力。

- [x] 后端：新增 `GET /api/tasks/{taskId}/my-result`，返回学生本人批改详情。
- [x] 后端：补充批改反馈数据落点，支持教师总评、批改时间、逐题评语。
- [x] 后端：收紧 `GET /api/submissions/{id}` 权限，学生只能读取本人提交。
- [x] 前端设计：已明确 `TaskResult.vue` 的桌面/移动布局、空状态、待批改状态、权限错误状态。
- [x] 前端：新增 `/student/tasks/:taskId/result` 路由和 `TaskResult.vue` 页面。
- [x] 前端：首页最近评分跳转到批改详情。
- [x] 前端：课时任务列表中已评分任务跳转到批改详情。
- [x] 前端：学习单提交成功页提供查看提交状态/批改结果入口。
- [x] 前端：学习评价页评分明细跳转到批改详情；评价/提交接口已返回 `taskId/submissionId/taskTitle`。
- [x] 前端：教师数据看板使用 `gradedCount/specialCount/notSubmittedCount/submittedCount` 做清晰状态图例。
- [x] 前端：教师批改页改“自动题预评分”文案，增加人工题未评分校验和底部固定保存栏。
- [x] 测试：后端补权限/API 单测，覆盖本人可见、未提交、待批改、已批改、权限收紧。
- [x] 测试：前端通过类型检查、单测和生产构建。
- [x] 测试：补学生结果页专用组件测试，覆盖 `not_submitted/submitted/graded/special/403`。
- [x] 测试：补教师数据看板状态口径后端回归测试；前端已通过类型检查、组件测试和生产构建。
- [x] 文档：每完成一项，在 `PROGRESS.md` Phase 8 清单中更新状态。

### Phase F5 — 考试 + 项目 + 结果评价（依赖 Phase 6a-c 后端）

- [x] 教师：试卷管理 + 考试任务管理基础版
- [x] 教师：考试应完成人员名单 + 独立路由批改 + 退回/重新批改
- [x] 教师：项目管理基础版
- [x] 教师：独立项目管理页接入已有项目提交查看 + 新评分模型评分
- [x] 学生：考试状态列表 + 开始/恢复 + 草稿保存 + 倒计时 + 结果/退回
- [x] 学生：个人项目列表 + 备注/组员说明 + 作品提交/重交
- [x] 结果评价展示基础版（学生评价页 + 任务批改明细；进步雷达/总评继续增强）

### Phase F6 — 网盘 + 总评导出（依赖 Phase 7 后端）

- [x] 学生：我的网盘页基础版（文件夹树 + 上传/下载/预览/删除）
- [x] 教师：查看学生网盘基础版（学生页弹窗）
- [x] 教师：学期总评预览 + Excel 导出基础产品化
- [x] 教师：班级数据分析基础产品化（`/teacher/stats`）
- [x] Store：`useClassFilterStore` 全教师页面联动（课程列表、任务数据看板、班级分析已联动；无班级参数的接口暂不硬接）

#### `useClassFilterStore` 全教师页面联动计划

设计约束：
- 顶部班级选择器作为教师端“当前班级上下文”，只在能消费班级筛选的页面显示，避免用户选择后页面没有反应。
- 页面内仍保留局部筛选控件；顶部选择器改变时同步局部筛选，局部筛选改变时也写回 store，形成单一班级上下文。
- 任务数据看板已有后端 `classId` 参数，直接按班级重新拉取看板数据。
- 班级数据分析后端总评预览目前不支持 `classId` 参数，先在前端用已有预览行的 `className` 聚合映射全局 `classId`，不新增后端接口。
- 成绩导出、教师首页等页面暂不接入全局班级筛选，原因是当前 API 没有班级维度参数；后续如要接入，应先扩展后端查询。

实现计划：
1. `TeacherLayout.vue` 顶部班级选择器从仅课程页扩展到课程列表、任务数据看板和班级数据分析页，并把 placeholder 改成更通用的“当前班级”。
2. `TaskAnalytics.vue` 接入 `useClassFilterStore`：加载任务所属课程班级后，如果全局班级属于该任务课程范围，则自动应用；页面内切换班级时同步写回全局 store。
3. `ClassStats.vue` 拉取教师班级列表，使用 `grade + '级' + name` 映射到总评预览的 `className`；顶部全局班级变化时同步页面内班级筛选，页面内筛选变化时同步回 store。
4. 测试：运行 `npm run check`，重点确认任务看板和班级分析的类型、watch 链路和构建通过。

执行结果：
- `TeacherLayout.vue` 顶部班级选择器已扩展到课程列表、任务数据看板和班级数据分析页，文案统一为“当前班级”。
- `TaskAnalytics.vue` 已接入全局班级上下文；全局班级属于任务课程范围时自动按班级拉取 `/api/tasks/{taskId}/analytics?classId=...`，页面内切换也会写回 store。
- `ClassStats.vue` 已将全局 `classId` 映射为总评预览中的 `className`，顶部班级和页面内班级筛选双向同步。
- 教师首页、成绩导出暂未接入全局班级筛选，因为当前接口没有班级查询参数；后续应先补后端能力再联动。
- 验证：`npm run check` 通过质量门、类型检查、7 个前端测试文件 28 个测试和生产构建。

---

## 七、设计规范（Quiet Precision 重申）

所有前端页面必须遵守以下规范：

- 配色：浅色 `#fafaf9`/`#1a1a18`，暗色 `#141412`/`#e8e6e1`
- 字体：Geist + 系统中文字体
- 圆角：4/6/8/10/12/16px 六阶
- 间距：内容区 28px 32px，卡片 24px
- 阴影：不用。用 1px solid border 代替
- 动效：150-200ms ease，仅 border-color/transform 变化
- 空状态：SVG 线框图标 + 虚线边框

---

## 八、立即需要执行的改进项

以下为 F1 阶段收尾必须完成的工作：

1. ~~TeacherLayout 改造为侧边栏+顶栏布局~~ ✅
2. ~~StudentLayout 增加侧边栏导航~~ ✅
3. ~~抽取 `CourseCard.vue`~~ ✅
4. ~~抽取 `PageHeader.vue`~~ ✅
5. ~~dayjs 统一日期格式化~~ ✅
6. ~~教师管理页补充删除按钮~~ ✅
