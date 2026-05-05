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
| 管理员-教师管理 | `views/admin/TeacherManage.vue` | ✅ 合格 | 表格+创建/编辑/班级绑定。缺少删除按钮，需补充 |
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
  /teacher/tasks/:id/submissions            任务提交列表 + 实时统计
  /teacher/tasks/:id/grading                评分页（四维度 A-E）
  /teacher/exams                            考试管理（试卷列表 + 考试任务）
  /teacher/exams/:id/submissions            考试提交查看
  /teacher/projects                         项目管理
  /teacher/projects/:id/teams               项目队伍 + 评分
  /teacher/stats                            班级数据分析（跨班级对比）
  /teacher/students/:id/drive               查看学生网盘
  /teacher/export                           学期总评导出

/student                        学生布局（顶部导航）
  /student/home                 我的课程
  /student/courses/:id          课程详情
  /student/courses/:id/resources 课程资源（只读）
  /student/lessons/:lid         课时详情（资源 + 任务入口）
  /student/tasks/:id             学习单填写 / 作品提交
  /student/exams                 我的考试
  /student/exams/:id             考试答题
  /student/projects              我的项目
  /student/projects/:id          项目详情 + 组队 + 提交
  /student/grades                我的评价 + 雷达图
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
- **缺少：** 删除教师按钮（需后端补充）

**状态：** ✅ 基本完成，需补充删除功能

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
- 功能入口卡片网格
- 课程管理（可点击跳转，标记"已上线"）
- 课堂任务（标记"即将上线" → Phase F3）
- 学生评价（标记"即将上线" → Phase F4）
- 考试管理（标记"即将上线" → Phase F5）
- 项目管理（标记"即将上线" → Phase F5）
- 数据分析（标记"即将上线" → Phase 5b）
- 学期总评导出（标记"即将上线" → Phase F6）

**状态：** ✅ 基本完成，后续阶段逐步激活各卡片

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

**缺失：** 课程资源文件夹管理（Phase F2 补充）

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

**状态：** ⬜ 未开始

---

#### 教师-课时任务管理 `/teacher/semesters/:sid/lessons/:lid/tasks` 🔨 Phase F3

**功能：**
- 任务列表（学习单 + 课堂作品混合展示）
- 创建学习单任务：标题 + 表单设计器（radio/checkbox/text/textarea/table）
- 创建课堂作品任务：标题 + 说明 + 截止时间
- 编辑/删除任务
- 从课时详情页可导航至此

**状态：** ⬜ 未开始

---

#### 教师-任务提交列表 `/teacher/tasks/:id/submissions` 🔨 Phase F3

**功能：**
- 提交列表表格：学生姓名、学号、提交状态、提交时间
- 按班级筛选
- 学习单提交：查看答案内容（按题目展示）
- 课堂作品提交：下载/预览附件
- 实时统计面板（ECharts）：
  - 学习单：各选项统计（单选/多选用柱状图/饼图，填空/表格用列表）
  - 课堂作品：提交/未提交人数统计
- WebSocket 实时更新（新提交自动刷新统计）

**状态：** ⬜ 未开始

---

#### 教师-评分页 `/teacher/tasks/:id/grading` 🔨 Phase F4

**功能：**
- 学生提交列表（含提交内容预览）
- 四维度评分选择器（每个维度 A/B/C/D/E 下拉或按钮组）
- 可选 1-4 个维度评分
- 特殊情况标记（填写原因）
- 批量评分（同分快速操作）
- 评分后状态变更（submitted → graded）

**状态：** ⬜ 未开始

---

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

**状态：** ⬜ 未开始

---

#### 教师-考试提交查看 `/teacher/exams/:id/submissions` 🔨 Phase F5

**功能：**
- 提交列表：学生姓名、学号、得分、状态
- 查看答题详情（题目+学生答案+得分）
- 简答题手动评分
- 缺考/特殊情况标记

**状态：** ⬜ 未开始

---

#### 教师-项目管理 `/teacher/projects` 🔨 Phase F5

**功能：**
- 项目列表（名称、学期、组队人数、截止时间）
- 创建项目：名称 + 说明 + 学期 + 最大组队人数 + 截止时间 + 权重
- 编辑/删除项目

**状态：** ⬜ 未开始

---

#### 教师-项目队伍与评分 `/teacher/projects/:id/teams` 🔨 Phase F5

**功能：**
- 队伍列表（队名、成员、提交状态）
- 查看队伍提交作品（下载/预览）
- 项目评分（A-F，组队同分）
- 特殊情况标记

**状态：** ⬜ 未开始

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

**状态：** ⬜ 未开始

---

#### 教师-查看学生网盘 `/teacher/students/:id/drive` 🔨 Phase F6

**功能：**
- 网盘文件树（只读浏览）
- 文件预览（kkFileView）
- 文件下载
- **不可：** 上传、删除（需审计日志的写操作暂不开放）

**状态：** ⬜ 未开始

---

#### 教师-学期总评导出 `/teacher/export` 🔨 Phase F6

**功能：**
- 选择课程 + 学期
- 总评预览表格（学号、姓名、班级、四维度分、过程分、结果分、总评、等级）
- 导出 Excel 按钮（按班级分 Sheet）
- 导出进度反馈

**状态：** ⬜ 未开始

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

**缺失：** 课程资源浏览（Phase F2 补充）

---

#### 学生-课程资源 `/student/courses/:id/resources` 🔨 Phase F2

**功能：**
- 文件夹树浏览（只读）
- 文件下载
- 文件预览（kkFileView）
- **不可：** 上传、新建文件夹、删除、重命名

**状态：** ⬜ 未开始

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

**状态：** ⬜ 未开始

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

**状态：** ⬜ 未开始

---

#### 学生-作品提交 `/student/tasks/:id` (artifact类型) 🔨 Phase F3

**功能：**
- 任务说明展示
- 文件上传（拖拽/点击，进度条）
- 已上传文件列表（可删除重传，截止前）
- 提交按钮
- 截止后禁用上传和提交

**状态：** ⬜ 未开始

---

#### 学生-我的考试 `/student/exams` 🔨 Phase F5

**功能：**
- 考试列表（名称、时间、状态：即将开始/进行中/已结束）
- 进行中的考试：点击进入答题
- 已结束：查看成绩

**状态：** ⬜ 未开始

---

#### 学生-考试答题 `/student/exams/:id` 🔨 Phase F5

**功能：**
- 计时器（倒计时，时间到自动提交）
- 题目渲染（单选/多选/判断/简答）
- 题目导航（快速跳转）
- 提交确认对话框
- 已提交后显示只读状态

**状态：** ⬜ 未开始

---

#### 学生-我的项目 `/student/projects` 🔨 Phase F5

**功能：**
- 项目列表（名称、截止时间、组队状态、提交状态）
- 组队：创建队伍 / 加入已有队伍
- 提交：上传项目作品

**状态：** ⬜ 未开始

---

#### 学生-项目详情 `/student/projects/:id` 🔨 Phase F5

**功能：**
- 项目说明
- 队伍管理：创建队伍、查看队伍成员、加入队伍
- 作品提交：文件上传 + 提交
- 查看评分结果

**状态：** ⬜ 未开始

---

#### 学生-我的评价 `/student/grades` 🔨 Phase F4

**功能：**
- 当前学期四维度分数卡片
- 学期雷达图（ECharts radar）
- 进步雷达图（双学期叠加对比）
- 各任务评分详情列表
- 过程评价分 / 结果评价分 / 学期总评

**状态：** ⬜ 未开始

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

**状态：** ⬜ 未开始

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
| 成绩导出 | `/teacher/export` | F6 |

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
| 我的评价 | `/student/grades` | F4 |
| 我的网盘 | `/student/drive` | F6 |

**状态：** ⬜ 需改造

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
| `RadarChart.vue` | ECharts 雷达图封装 | 🔲 Phase F4 |

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
  /teacher/tasks/:id/submissions    // TaskSubmissions (Phase F3)
  /teacher/tasks/:id/grading        // TaskGrading (Phase F4)
  /teacher/exams                    // ExamManage (Phase F5)
  /teacher/exams/:id/submissions    // ExamSubmissions (Phase F5)
  /teacher/projects                 // ProjectManage (Phase F5)
  /teacher/projects/:id/teams       // ProjectTeams (Phase F5)
  /teacher/stats                    // ClassStats (Phase 5b)
  /teacher/students/:id/drive       // StudentDriveView (Phase F6)
  /teacher/export                   // GradeExport (Phase F6)
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
  /student/grades                   // StudentGrades (Phase F4)
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
- [ ] 课程封面图片上传

### Phase F3 — 课堂任务 + 实时统计（依赖 Phase 4 后端）

- [ ] 教师：课时任务管理页（学习单编辑器 + 作品任务表单）
- [ ] 教师：任务提交列表 + 实时统计图表（ECharts + WebSocket）
- [ ] 学生：课时详情页（资源 + 任务入口）
- [ ] 学生：学习单填写页（JSON Schema 动态表单）
- [ ] 学生：作品提交页（FileUpload）
- [ ] Composable：`useWebSocket`、`useFileUpload`

### Phase F4 — 评分 + 雷达图（依赖 Phase 5 后端）

- [ ] 教师：任务评分页（四维度 A-E 选择器 + 特殊情况标记）
- [ ] 学生：我的评价页（分数卡片 + 学期雷达图 + 进步雷达图）
- [ ] 共享组件：`RadarChart.vue`
- [ ] Store：`useSemesterStore`

### Phase F5 — 考试 + 项目 + 结果评价（依赖 Phase 6a-c 后端）

- [ ] 教师：试卷管理 + 考试任务管理
- [ ] 教师：考试提交查看 + 评分
- [ ] 教师：项目管理 + 队伍查看 + 评分
- [ ] 学生：考试列表 + 答题页（计时器）
- [ ] 学生：项目列表 + 组队 + 作品提交
- [ ] 结果评价展示（学生评价页扩展）

### Phase F6 — 网盘 + 总评导出（依赖 Phase 7 后端）

- [ ] 学生：我的网盘页（文件夹树 + 上传/下载/预览/删除）
- [ ] 教师：查看学生网盘
- [ ] 教师：学期总评预览 + Excel 导出
- [ ] Store：`useClassFilterStore` 完整接入

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
