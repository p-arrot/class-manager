# 开发进程

## 已完成

### Phase 1 — 后端基础工程 ✅

- Spring Boot 3.4.1 项目 + Maven + application.yml
- Docker Compose 开发环境（PostgreSQL 16, Redis 7, MinIO, kkFileView）
- Flyway 数据库迁移（V1: users, schools, school_classes, teacher_classes, audit_logs）
- 统一响应 `R<T>` / `PageResult<T>` / `ErrorCode`（27 个错误码）
- `BizException` / `GlobalExceptionHandler`
- Spring Security 6 + JWT 鉴权（全局查找：admin/teacher 按 username，student 按 student_no）
- BCrypt 密码加密，默认管理员自动初始化（admin/admin123）
- SpringDoc OpenAPI（`/api-docs`）
- MyBatis-Plus + 分页插件 + 元数据自动填充
- 审计日志基础设施

### Phase 2 — 班级、教师、学生管理 ✅

- 班级管理 CRUD（`/api/classes`），含学生/教师关联检查
- 教师管理 CRUD（`/api/teachers`），含班级批量绑定/解绑
- 学生 Excel 导入（EasyExcel，含失败原因反馈）
- 学生分页列表查询，按班级筛选
- 学生密码重置（默认密码/指定新密码，BCrypt 加密，审计日志记录）
- 教师数据隔离：教师只能访问自己负责班级的学生
- Flyway V2 + V3 迁移（全局唯一索引、性能索引）
- 16 个 REST 端点，全部通过 API 测试

### Phase F0 — 前端脚手架 + 登录 + 三套 Layout ✅

- Vite + Vue 3 + TypeScript 项目
- Naive UI 主题系统（浅色/深色两套 themeOverrides，"Quiet Precision" 设计风格）
- Geist 字体 + 系统中文字体
- Axios 封装（token 注入、双格式响应处理、401 自动跳转）
- 登录页（左右双栏布局，"让每一堂课都有迹可循"）
- 路由守卫（按 role 分发，未登录 → `/login`，角色不匹配 → 跳回首页）
- AdminLayout / TeacherLayout / StudentLayout 三套骨架
- Pinia auth store + theme store（localStorage 持久化）
- 5 个 API 模块（auth, classes, teachers, students）
- TypeScript 类型定义（LoginRequest/Response, ClassVO, TeacherVO, StudentVO 等）
- 3 个管理员占位页 + 教师/学生首页
- 署名：Tatakai

---

## 未完成

### Phase 3a — 后端：课程、学期、课时

- [ ] Course 实体 + CRUD
- [ ] CourseClass 关系表（课程绑定授课班级）
- [ ] Semester 实体 + CRUD（属于课程）
- [ ] Lesson 实体 + CRUD（属于学期，含排序号）
- [ ] 课程资源文件夹（树形目录结构）
- [ ] 权限校验：教师管理自己的课程；学生只看到关联班级的课程

### Phase F1 — 前端：管理员端 + 教师课程页

- [ ] 管理员端：班级管理页（列表/新建/编辑/删除）
- [ ] 管理员端：教师管理页（列表/新建/编辑/班级绑定）
- [ ] 管理员端：学生管理页（列表/Excel 导入/重置密码）
- [ ] 教师端：课程列表 + 新建/编辑课程
- [ ] 教师端：学期管理 + 课时管理（嵌套路由）

### Phase 3b — 后端：MinIO 文件基础设施

- [ ] MinIO 客户端配置
- [ ] 文件上传（预签名 PUT URL，前端直传）
- [ ] 文件下载（预签名 GET URL）
- [ ] kkFileView 预览对接
- [ ] 课程封面上传
- [ ] 课时资源上传/下载/预览

### Phase F2 — 前端：文件组件 + 课程资源

- [ ] FileUpload 组件（拖拽/点击、进度条）
- [ ] FilePreview 组件（kkFileView iframe 嵌入）
- [ ] FileList 组件（图标/列表视图）
- [ ] 课程资源文件夹树 + 上传/下载/预览
- [ ] 课时资源管理

### Phase 4 — 后端：课堂任务 + 实时汇总

- [ ] Task 模块（worksheet / artifact）
- [ ] 学习单 JSON Schema 设计（radio / checkbox / text / textarea / table）
- [ ] 教师创建学习单 + 课堂作品任务
- [ ] 学生填写学习单 / 提交作品附件
- [ ] 教师查看/下载/预览提交
- [ ] WebSocket + STOMP 配置
- [ ] 学习单提交实时推送 + 教师端统计汇总

### Phase F3 — 前端：课堂任务 + 实时统计

- [ ] 教师端：任务创建页（学习单 schema 编辑器 + 作品任务表单）
- [ ] 学生端：学习单填写（JSON Schema 动态渲染表单）
- [ ] 学生端：作品提交
- [ ] 教师端：提交列表 + 实时统计图表（ECharts）
- [ ] WebSocket 订阅 Hook（`useSocket` composable）

### Phase 5 — 后端：评分和雷达图

- [ ] Evaluation 模块（四维度评分 A-E）
- [ ] 教师评分（可选 1-4 个维度）
- [ ] 特殊情况标记
- [ ] 截止未交自动评 F
- [ ] 过程评价计算（worksheet 权重 1.0 + artifact 权重 1.5）
- [ ] 学期雷达图 + 进步雷达图数据

### Phase F4 — 前端：评分 + 雷达图

- [ ] 教师端：评分页（四维度 A-E 选择器）
- [ ] 学生端：我的评分 + 学期雷达图（ECharts radar）
- [ ] 学生端：进步雷达图（双雷达叠加对比）
- [ ] 雷达图组件封装

### Phase 6a — 后端：考试系统

- [ ] 试卷管理（JSONB 题目）
- [ ] 考试任务创建（定时、班级、权重）
- [ ] 学生参加考试（计时答题）
- [ ] 考试提交与评分
- [ ] 缺考处理

### Phase 6b — 后端：项目化学习

- [ ] 项目创建 + 组队
- [ ] 项目作品提交
- [ ] 教师评分（组队同分）

### Phase 6c — 后端：结果评价

- [ ] 考试/项目加权平均
- [ ] 无数据时"暂无数据"处理

### Phase F5 — 前端：考试 + 项目 + 结果评价

- [ ] 教师端：试卷编辑器 + 考试任务创建
- [ ] 学生端：考试答题页（计时器、题型渲染）
- [ ] 学生端：项目组队 + 作品提交
- [ ] 教师端：项目评分页
- [ ] 结果评价展示

### Phase 7 — 后端：网盘和总评导出

- [ ] 学生网盘（树形目录、MinIO、容量限制）
- [ ] 教师查看负责班级学生网盘
- [ ] 学期总评计算（过程 × 50% + 结果 × 50%）
- [ ] EasyExcel 按班级分 Sheet 导出

### Phase F6 — 前端：网盘 + 总评导出

- [ ] 学生端：网盘页（树形目录、上传/下载/预览/删除）
- [ ] 教师端：学生网盘查看
- [ ] 教师端：总评导出按钮 + 导出进度反馈

---

## 进度概览

| 阶段 | 内容 | 状态 |
|------|------|------|
| Phase 1 | 后端基础工程 | ✅ 已完成 |
| Phase 2 | 班级、教师、学生管理 | ✅ 已完成 |
| Phase F0 | 前端脚手架 + 登录 + Layout | ✅ 已完成 |
| Phase 3a | 后端：课程、学期、课时 | ⬜ 待开始 |
| Phase F1 | 前端：管理员端 + 教师课程页 | ⬜ 待开始 |
| Phase 3b | 后端：MinIO 文件基础设施 | ⬜ 待开始 |
| Phase F2 | 前端：文件组件 + 课程资源 | ⬜ 待开始 |
| Phase 4 | 后端：课堂任务 + 实时汇总 | ⬜ 待开始 |
| Phase F3 | 前端：课堂任务 + 实时统计 | ⬜ 待开始 |
| Phase 5 | 后端：评分和雷达图 | ⬜ 待开始 |
| Phase F4 | 前端：评分 + 雷达图 | ⬜ 待开始 |
| Phase 6a | 后端：考试系统 | ⬜ 待开始 |
| Phase 6b | 后端：项目化学习 | ⬜ 待开始 |
| Phase 6c | 后端：结果评价 | ⬜ 待开始 |
| Phase F5 | 前端：考试 + 项目 + 结果评价 | ⬜ 待开始 |
| Phase 7 | 后端：网盘和总评导出 | ⬜ 待开始 |
| Phase F6 | 前端：网盘 + 总评导出 | ⬜ 待开始 |
