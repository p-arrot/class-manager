# 信息科技课堂管理系统 — 项目规格说明书

## 相关文档

| 文档 | 说明 |
|------|------|
| `SPECIFICATION.md` (本文档) | 项目整体规格：技术栈、架构、角色体系、数据库、API、设计规范 |
| `API.md` | 接口文档（已实现端点详情） |
| `PROGRESS.md` | 开发进度跟踪（各阶段完成情况） |
| `FRONTEND_PLAN.md` | 前端完整规划：页面清单、路由结构、组件设计、交互逻辑、分阶段计划 |
| `BACKEND_PLAN.md` | 后端模块规划：模块依赖、实现模式、技术决策 |

本系统面向中小学信息科技课堂教学，用于教师进行课程管理、课堂任务管理、学习单管理、课堂作品收集、考试管理、项目化学习管理、过程评价、结果评价、学生能力雷达图分析、学生网盘管理和学期总评导出。

所有代码生成、架构设计、数据库设计、接口设计、前端页面开发，必须遵守本文档约定。

---

## 一、技术栈

### 后端技术栈

使用：

- Java 21
- Spring Boot 3.x
- Spring Web MVC
- Spring Security 6
- JWT 鉴权
- MyBatis-Plus
- PostgreSQL 16
- Redis
- MinIO
- kkFileView
- EasyExcel
- Flyway
- Spring WebSocket + STOMP
- SpringDoc OpenAPI
- Lombok
- MapStruct
- Hutool
- Maven

### 前端技术栈

使用：

- Vue 3
- TypeScript
- Vite
- Pinia
- Vue Router
- Naive UI
- ECharts
- Axios
- dayjs
- FormCreate 或自研 JSON Schema 表单设计器/渲染器

### 部署技术栈

使用：

- Docker
- Docker Compose
- Nginx
- PostgreSQL
- Redis
- MinIO
- kkFileView

---

## 二、总体架构

采用“模块化单体”架构，不使用微服务。

后端使用 Spring Boot 单体应用，按照业务领域分包，每个业务模块保持独立边界，方便后期扩展或拆分。

后端模块（✅=已实现，🔲=计划中）：

- auth ✅ — 登录认证
- user ✅ — 用户、学生、教师管理
- classes ✅ — 班级管理
- course ✅ — 课程 + 学期 + 课时 + 课程资源文件夹
- task ✅ — 课堂任务、学习单、课堂作品
- evaluation ✅ — 四维度评价
- exam ✅ — 试卷和考试
- project ✅ — 项目化学习
- drive ✅ — 学生网盘
- stats ✅ — 统计分析、学期总评
- realtime ✅ — WebSocket 实时汇总
- dashboard ✅ — 首页聚合接口
- audit ✅ — 审计日志

> 注：`semester` 和 `lesson` 作为 `course` 模块的子包实现，不独立为顶层模块。`school` 表预留但未实现 CRUD。

后端目录结构：

```text
src/main/java/com/example/edu
├── EduApplication.java
├── common/
│   ├── config/       # SecurityConfig, JwtFilter, MybatisPlusConfig, AdminInitializer
│   ├── security/     # JwtUtils, LoginUser
│   ├── exception/    # BizException, GlobalExceptionHandler
│   ├── result/       # R<T>, PageResult<T>, ErrorCode
│   └── utils/        # SecurityUtils
├── modules/
│   ├── auth/         # ✅ 登录认证
│   ├── user/         # ✅ 用户、教师、学生管理
│   ├── classes/      # ✅ 班级管理
│   ├── course/       # ✅ 课程 + 学期 + 课时 + 资源文件夹
│   ├── audit/        # ✅ 审计日志
│   ├── task/         # ✅
│   ├── evaluation/   # ✅
│   ├── exam/         # ✅
│   ├── project/      # ✅
│   ├── drive/        # ✅
│   ├── stats/        # ✅
│   └── realtime/     # ✅
└── infrastructure/    # ✅ minio / preview

每个业务模块按照以下结构组织：

```text
modules/xxx
├── controller
├── service
│   └── impl
├── mapper
├── entity
├── dto
├── vo
├── enums
└── converter
```

---

## 三、基础编码规范

请严格遵守以下规则：

1. 所有接口返回统一结构：

```json
{
  "code": 0,
  "msg": "ok",
  "data": {}
}
```

2. 使用 `R<T>` 作为统一响应类。

3. 使用 `PageResult<T>` 作为分页响应类。

4. 使用 `BizException` 处理业务异常。

5. 使用 `GlobalExceptionHandler` 统一处理异常。

6. Controller 只负责接收参数、调用 Service、返回结果，不写业务逻辑。

7. Service 负责业务逻辑、权限校验和事务控制。

8. Mapper 只负责数据库访问。

9. Entity 不直接返回前端，必须转换成 VO。

10. 入参使用 DTO，出参使用 VO。

11. DTO 使用 `@Valid` 做参数校验。

12. 涉及多表写入的方法必须添加：

```java
@Transactional(rollbackFor = Exception.class)
```

13. 密码必须使用 BCrypt 加密。

14. 系统不能保存明文密码。

15. 教师不能查看学生原始密码，只能重置学生密码。

16. 删除重要业务数据默认使用逻辑删除。

17. 所有数据库变更必须通过 Flyway SQL 脚本完成。

18. 涉及敏感操作必须写入审计日志。

19. 权限校验不能只依赖 Controller 注解，Service 层必须兜底校验。

20. 所有文件存储到 MinIO，数据库只保存文件元数据和 objectName。

---

## 四、角色体系

系统包含以下角色：

### 1. 管理员 admin

管理员负责系统初始化和基础数据管理。

管理员可以：

- 管理学校
- 管理班级
- 创建教师账号
- 管理教师账号
- 查看全校数据
- 初始化系统配置

### 2. 教师 teacher

教师负责教学相关业务。

教师可以：

- 管理自己负责班级的学生
- 使用 Excel 导入学生
- 重置学生密码
- 查看学生信息
- 按学校、班级筛选学生
- 分析班级间总体数据
- 创建课程
- 为课程选择授课班级
- 创建课程资源
- 创建学期
- 创建课时
- 上传课时学习资源
- 创建学习单任务
- 创建课堂作品任务
- 查看学生提交
- 下载学生作品
- 预览学生作品
- 对学习单、课堂作品、项目进行评分
- 创建试卷
- 创建考试任务
- 创建项目化学习任务
- 查看学生网盘
- 下载或预览学生网盘文件
- 导出学生学期总评 Excel

教师不能：

- 查看学生原始密码
- 管理非负责班级的学生
- 操作非本人创建或未授权的课程
- 查看非负责班级的学生评价数据

### 3. 学生 student

学生不支持自主注册，只能由管理员或教师导入/创建。

学生可以：

- 使用学号和密码登录
- 查看自己班级关联的课程
- 进入课程、学期、课时
- 查看课程资源
- 预览或下载课时学习资源
- 填写学习单
- 提交课堂作品
- 参加考试
- 提交项目化学习作品
- 查看自己的评分结果
- 查看本人任务的批改详情，包括逐题得分、维度拆分、教师总评和逐题评语
- 查看自己的学期能力雷达图
- 查看自己的进步雷达图
- 使用个人网盘上传、下载、预览和管理文件

---

## 五、账号规则

### 学生账号

学生账号字段包括：

- 学校
- 班级
- 学号
- 姓名
- 密码

学生默认密码为：

```text
123456
```

密码必须用 BCrypt 加密保存。

学生登录使用：

```text
学号 + 密码
```

学号唯一规则：

```text
学号全局唯一，不同班级不可重复。
```

因此数据库中学生唯一索引应为：

```text
student_no
```

### 教师与班级关系

一个教师可以负责多个班级。

一个班级也可以有多个教师。

使用关系表：

```text
teacher_classes
```

教师只能管理自己负责班级下的学生、任务、评价和网盘数据。

### 教师账号

教师账号字段包括：

- 学校
- 用户名
- 姓名
- 密码

教师登录使用：

```text
用户名 + 密码
```

教师负责的班级由其与班级的绑定关系决定。

### 管理员账号

管理员账号字段包括：

- 用户名
- 姓名
- 密码

管理员登录使用：

```text
用户名 + 密码
```

管理员不绑定学校，可管理所有学校数据。

### 登录流程

系统不依赖学校区分用户，用户名和学号全局唯一：

1. 先按 **username** 匹配 `admin` 角色
2. 再按 **username** 匹配 `teacher` 角色
3. 最后按 **student_no** 匹配 `student` 角色
4. 首次命中即返回，均未命中则返回用户名或密码错误

### 学生密码重置规则

教师和管理员不能查看学生原始密码。

教师可以对自己负责班级的学生执行密码重置操作。

密码重置方式包括：

- 重置为默认密码 `123456`
- 设置指定新密码

所有密码必须使用 BCrypt 加密保存。

密码重置操作必须写入审计日志。

---

## 六、课程结构

系统课程结构如下：

```text
课程 Course
  ├── 课程资源 CourseResource
  └── 学期 Semester
        └── 课时 Lesson
              ├── 学习资源 LessonResource
              └── 课堂任务 Task
                    ├── 学习单 Worksheet
                    └── 课堂作品 Artifact
```

### 6.1 课程实体字段

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, GENERATED ALWAYS AS IDENTITY | 课程 ID |
| name | VARCHAR(200) | NOT NULL | 课程名称 |
| description | TEXT | 可空 | 课程介绍 |
| cover_url | VARCHAR(500) | 可空 | 课程封面图片 URL（MinIO 存储） |
| teacher_id | BIGINT | NOT NULL, FK → users(id) | 创建教师 |
| created_at | TIMESTAMP | NOT NULL DEFAULT now() | 创建时间 |
| updated_at | TIMESTAMP | NOT NULL DEFAULT now() | 更新时间 |
| deleted | SMALLINT | NOT NULL DEFAULT 0 | 逻辑删除标记 |

### 6.2 课程-班级关系表

```text
course_classes (course_id, class_id)
```

- 多对多关系，一个课程可绑定多个授课班级
- 关系表无软删除（删除课程时级联删除绑定关系）
- 唯一约束：`(course_id, class_id)`
- 索引：`idx_course_classes_course`，`idx_course_classes_class`

只有课程关联班级中的学生才能看到该课程。

### 6.3 课程接口

```
POST   /api/courses                  创建课程（TEACHER）
GET    /api/courses                  分页列表（ADMIN/TEACHER/STUDENT，按角色过滤）
GET    /api/courses/{id}             课程详情（含学期列表和班级绑定）
PUT    /api/courses/{id}             更新课程（TEACHER，仅创建者）
DELETE /api/courses/{id}             删除课程（ADMIN/TEACHER，需检查学期依赖）
```

**创建课程请求体：**
```json
{
  "name": "Python编程基础",
  "description": "面向初学者的Python入门课程",
  "coverUrl": null,
  "classIds": [1, 2]
}
```

- `classIds` 可选，创建时可同时绑定授课班级
- 课程名称在同一教师下唯一
- 教师只能操作自己创建的课程（admin 可查看全部但不可写）

---

## 七、学期和课时

### 7.1 学期实体字段

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, GENERATED ALWAYS AS IDENTITY | 学期 ID |
| name | VARCHAR(200) | NOT NULL | 学期名称 |
| start_time | TIMESTAMP | NOT NULL | 开始时间 |
| end_time | TIMESTAMP | NOT NULL | 结束时间 |
| course_id | BIGINT | NOT NULL, FK → courses(id) | 所属课程 |
| created_at | TIMESTAMP | NOT NULL DEFAULT now() | 创建时间 |
| updated_at | TIMESTAMP | NOT NULL DEFAULT now() | 更新时间 |
| deleted | SMALLINT | NOT NULL DEFAULT 0 | 逻辑删除标记 |

### 7.2 课时实体字段

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, GENERATED ALWAYS AS IDENTITY | 课时 ID |
| name | VARCHAR(200) | NOT NULL | 课时名称 |
| sort_order | INT | NOT NULL DEFAULT 0 | 排序号（同一学期下递增） |
| semester_id | BIGINT | NOT NULL, FK → semesters(id) | 所属学期 |
| created_at | TIMESTAMP | NOT NULL DEFAULT now() | 创建时间 |
| updated_at | TIMESTAMP | NOT NULL DEFAULT now() | 更新时间 |
| deleted | SMALLINT | NOT NULL DEFAULT 0 | 逻辑删除标记 |

课时中可以添加：

- 学习资源
- 学习单任务
- 课堂作品任务

### 7.3 学期接口（嵌套在课程路径下）

```
GET    /api/courses/{courseId}/semesters      学期列表（按 start_time 倒序）
POST   /api/courses/{courseId}/semesters      创建学期（TEACHER，需验证课程所有权）
GET    /api/semesters/{id}                    学期详情
PUT    /api/semesters/{id}                    更新学期（TEACHER）
DELETE /api/semesters/{id}                    删除学期（TEACHER，需检查课时依赖）
```

**创建学期请求体：**
```json
{
  "name": "2026年秋季学期",
  "startTime": "2026-09-01 00:00:00",
  "endTime": "2027-01-15 00:00:00"
}
```

- 学期名称在同一课程下唯一
- 删除学期前检查是否有课时（有课时则返回 40913）

### 7.4 课时接口（嵌套在学期路径下）

```
GET    /api/semesters/{semesterId}/lessons    课时列表（按 sort_order 升序）
POST   /api/semesters/{semesterId}/lessons    创建课时（TEACHER，sort_order 自动取 max+1）
GET    /api/lessons/{id}                      课时详情
PUT    /api/lessons/{id}                      更新课时（TEACHER）
DELETE /api/lessons/{id}                      删除课时（TEACHER）
PUT    /api/lessons/{id}/sort                 调整顺序（TEACHER，内存重排后批量更新）
```

**调整顺序请求体：**
```json
{
  "targetIndex": 0
}
```

- 新建课时自动追加到末尾（sort_order = 当前最大 + 1）
- 排序调整：将指定课时移到 targetIndex 位置，其他课时顺延
- 权限回溯：操作课时前回溯到学期 → 课程，验证教师所有权

---

## 八、课程资源和课时资源

### 8.1 课程资源文件夹（树形结构）

课程资源用于存放教师提供的课件、参考资料等。Phase 3a 实现文件夹管理（树形目录），Phase 3b 实现文件上传。

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, GENERATED ALWAYS AS IDENTITY | 资源 ID |
| course_id | BIGINT | NOT NULL, FK → courses(id) | 所属课程 |
| parent_id | BIGINT | 可空, FK → course_resources(id) | 父文件夹 ID（null=根目录） |
| name | VARCHAR(200) | NOT NULL | 文件夹名称 |
| type | VARCHAR(20) | NOT NULL DEFAULT 'FOLDER' | 资源类型（目前仅 FOLDER） |
| sort_order | INT | NOT NULL DEFAULT 0 | 同级排序号 |
| created_at | TIMESTAMP | NOT NULL DEFAULT now() | 创建时间 |
| updated_at | TIMESTAMP | NOT NULL DEFAULT now() | 更新时间 |
| deleted | SMALLINT | NOT NULL DEFAULT 0 | 逻辑删除标记 |

文件存储使用 MinIO，文件预览使用 kkFileView，下载使用预签名 GET URL，大文件上传使用预签名 PUT URL 由前端直传。

### 8.2 课程资源接口

```
GET    /api/courses/{courseId}/resources/tree     获取完整资源树（嵌套结构）
GET    /api/courses/{courseId}/resources          获取指定文件夹下的子资源（?parentId=）
POST   /api/courses/{courseId}/resources          创建文件夹（TEACHER）
PUT    /api/resources/{id}                        重命名资源（TEACHER）
DELETE /api/resources/{id}                        删除资源（TEACHER，递归软删除子孙节点）
PUT    /api/resources/{id}/move                   移动资源（TEACHER，可修改 parentId + sortOrder）
```

**创建文件夹请求体：**
```json
{
  "name": "课件资料",
  "parentId": null
}
```

**移动资源请求体：**
```json
{
  "targetParentId": 5,
  "targetSortOrder": 2
}
```

- 移动时校验目标不能是自身的子孙节点
- 树接口返回嵌套 JSON 结构：`{id, name, children: [...]}`
- 删除时递归收集所有子孙节点 ID，逐个软删除

### 8.3 课时资源（Phase 3b 实现）

课时资源属于某一节课，支持上传、下载、预览。存储在 `lesson_resources` 表（Phase 3b 建表）。

课时资源支持格式：Word、PPT、PDF、HTML、图片。预览通过 kkFileView，上传/下载通过 MinIO 预签名 URL。

---

## 九、课堂任务

课堂任务统一使用 `Task` 表。

任务类型包括：

```text
worksheet：学习单
artifact：课堂作品
```

任务字段包括：

- 任务标题
- 任务类型
- 所属课时
- 学习单 schema
- 任务说明
- 截止时间
- 创建时间

### 学习单

学习单类似问卷星表单。教师创建任务时按题目逐题配置，学生端以问卷式页面作答。

学习单结构以 JSON 字符串保存到：

```text
tasks.form_schema
```

学习单支持题型：

- 填空 `blank`
- 单选 `single`
- 多选 `multiple`
- 是非 `true_false`
- 简答 `short`

每道题只有一个题干字段 `stem`。题干使用 Markdown 富文本，教师端可在源码编辑和预览之间切换，学生端只看到渲染后的 Markdown。题目可配置配图 `imageUrl`。

每道题必须设置核心素养维度分值。四个维度为：

- `AWARENESS`：信息意识
- `COMPUTING`：计算思维
- `DIGITAL_LEARNING`：数字化学习与创新
- `RESPONSIBILITY`：信息社会责任

学习单 schema 当前版本为 `version: 3`，示例：

```json
{
  "version": 3,
  "questions": [
    {
      "id": "q1",
      "type": "single",
      "stem": "Python 是什么类型的语言？",
      "required": true,
      "options": ["解释型", "编译型", "汇编"],
      "answer": "解释型",
      "autoGrade": true,
      "dimensionScores": [
        { "dimension": "COMPUTING", "maxScore": 5 },
        { "dimension": "DIGITAL_LEARNING", "maxScore": 3 }
      ]
    },
    {
      "id": "q2",
      "type": "short",
      "stem": "请简述算法的基本特征。",
      "required": true,
      "autoGrade": false,
      "dimensionScores": [
        { "dimension": "COMPUTING", "maxScore": 8 }
      ]
    }
  ]
}
```

学生提交答案保存到：

```text
submissions.content
```

学生答案示例：

```json
{
  "q1": "解释型",
  "q2": "算法具有有穷性、确定性、可行性、输入和输出。"
}
```

选择题、是非题需要设置正确答案并支持自动批改。填空和简答可以选择设置正确答案并启用自动批改，默认不启用。

### 课堂作品

课堂作品任务要求学生提交附件。

附件可以是：

- 文档
- 图片
- 压缩包
- HTML 文件
- 其他允许类型

作品文件存储到 MinIO。

提交内容保存为 JSONB，包含文件列表。

课堂作品任务可配置提交方式：

- 文件
- 文件夹

当提交方式为文件时，教师可以设置允许的文件后缀名；留空表示不限格式。

---

## 十、提交规则

学生提交统一使用 `submissions` 表。

提交状态包括：

```text
draft：草稿
submitted：已提交
graded：已评分
special：特殊情况
```

建议第一版支持：

```text
submitted
graded
special
```

后续再增强草稿功能。

学生查看批改详情时使用任务维度入口，而不是提交 ID：

```text
GET /api/tasks/{taskId}/my-result
```

状态约定：

- `not_submitted`：仅用于接口包装层，表示当前学生没有该任务提交，不写入 `submissions.status`。
- `submitted`：已提交，等待教师批改。
- `graded`：已完成批改，可展示总分、逐题得分、维度汇总和教师评语。
- `special`：特殊处理，不计入评价统计，页面展示原因或总评。

安全规则：

- 学生只能查看自己的批改详情，不能通过 `submissionId` 枚举同学提交。
- 教师查看提交详情时必须校验课程/班级归属。
- 批改详情页不得在 403/404/接口失败时渲染任何提交内容。

默认规则：

- 学生可以在截止时间前提交或修改
- 截止后不能修改
- 教师评分后不能修改
- 特殊情况不计入统计

---

## 十一、实时汇总

学生提交学习单后，教师端需要实时看到汇总结果。

使用：

```text
Spring WebSocket + STOMP
```

学生提交后推送到：

```text
/topic/task/{taskId}
```

教师端订阅该 Topic。

教师端使用 ECharts 实时展示统计结果。

统计规则：

- 单选题：统计各选项选择人数
- 多选题：统计各选项选择次数
- 填空题：列表展示或关键词统计
- 表格题：表格汇总展示

教师端可以按班级筛选汇总结果。

第一版实时汇总在学生提交学习单后触发：

```text
学生提交学习单 -> 后端保存提交 -> WebSocket 推送 -> 教师端刷新统计图表
```

后续可以扩展为填写过程中的草稿级实时同步，但第一版不实现。

---

## 十二、四维度评价体系

评价维度为信息科技核心素养四个维度：

```text
信息意识
计算思维
数字化学习与创新
信息社会责任
```

后端枚举建议：

```text
AWARENESS：信息意识
COMPUTING：计算思维
DIGITAL_LEARNING：数字化学习与创新
RESPONSIBILITY：信息社会责任
```

系统保留 A-E/F 等级评价作为兼容模型，同时引入逐题数值得分模型：

```text
dimension_scores
```

数值得分字段：

```text
student_id
source_type：process / exam / project
source_id：对应提交记录 ID
question_id：题目或评分项 ID
dimension：核心素养维度
earned_score：学生实际得分
max_score：该维度满分
auto_graded：是否自动批改
```

自动批改题根据正确答案写入 `earned_score/max_score`。手动批改题由教师按题目、按维度输入得分。

教师文字反馈不写入 `dimension_scores`，单独保存到 `submission_feedback`：

```text
submission_id：对应 submissions.id
teacher_id：最后批改教师
teacher_comment：整份任务总评语
question_feedback：逐题评语 JSON，包含 questionId/comment/referenceAnswerVisible
graded_at：批改完成时间
```

保存批改时：

- 自动题和人工题数值得分写入/覆盖 `dimension_scores`。
- 整体评语、逐题评语和参考答案可见性写入/覆盖 `submission_feedback`。
- `submissions.status` 从 `submitted` 变为 `graded`，特殊处理时变为 `special`。
- 重新保存同一份提交时必须覆盖旧评分，不产生重复脏数据。

每个核心素养维度最终按百分比折算：该维度学生得分 / 该维度总分。四个维度各自折算为 0-100 分，可用于雷达图和学期总评。

特殊情况：

```text
教师可以将某学生某次任务标记为“特殊情况，不计入评价”，并输入具体原因。
特殊情况不计入过程评价、结果评价、雷达图和总评。
```

特殊情况建议作用于：

```text
某学生的某一次任务提交
```

而不是单个维度。

---

## 十三、评分对象

教师可以对以下内容评分：

1. 学习单
2. 课堂作品
3. 项目化学习作品

评分以“题目/评分项 + 核心素养维度分值”为基础：

- 学习单：每道题设置一个或多个核心素养维度满分。
- 课堂作品：按作品任务中的评分项或题目维度评分。
- 考试：试卷题目同样设置核心素养维度满分。
- 项目：项目评分 rubric 设置核心素养维度满分。

自动批改只负责能明确判断正确答案的题目；填空、简答、项目、课堂作品等开放题由教师手动评分。

---

## 十四、过程评价

过程评价来源：

```text
学习单
课堂作品
```

项目化学习不计入过程评价，而是计入结果评价。

单次任务分数：

```text
单次任务分数 = 被评分维度分数平均值
```

过程评价分：

```text
过程评价某维度分 = 平时任务中该维度 earned_score 总和 / max_score 总和 * 100
过程评价分 = 四个维度分的平均值
```

特殊情况不计入。

如果学生某任务未提交，是否计 0 分由任务补交/缺交规则决定；当前主要统计已产生的有效 `dimension_scores`。

---

## 十五、考试系统

教师可以在学期中创建试卷和考试任务。

### 试卷

试卷包含题目信息，使用 JSONB 存储。

试卷支持题型：

- 单选题
- 多选题
- 判断题
- 填空题
- 简答题

建议第一版优先实现：

- 单选题
- 多选题
- 判断题
- 简答题

试卷字段包括：

- 试卷标题
- 题目 JSON
- 总分
- 创建教师

### 考试任务

考试任务字段包括：

- 所属学期
- 选择试卷
- 考试名称
- 选择班级
- 考试开始时间
- 考试结束时间
- 是否删除

考试不再单独设置评价权重。考试在学期总评中的占比由“学期考核方案”统一设置。

考试题目同样设置核心素养维度分值。多次考试时，按同一来源桶内该维度 `earned_score / max_score` 汇总折算，而不是先给每次考试单独权重。

考试可以删除，但必须逻辑删除，并记录审计日志。

### 考试缺考规则

考试结束后，如果学生没有提交考试，则视为缺考。

缺考默认记为：

```text
0 分
```

缺考成绩计入结果评价。

教师可以将缺考学生标记为特殊情况。

特殊情况需要填写原因。

特殊情况不计入结果评价和学期总评。

---

## 十六、项目化学习

教师可以在学期中创建项目。

项目字段包括：

- 项目名称
- 项目说明
- 所属学期
- 最大组队人数
- 截止时间
- 提交方式：文件 / 文件夹
- 允许文件后缀名
- 项目评分 rubric（核心素养维度满分）

学生需要在项目中提交项目化学习作品。

项目支持：

- 单人提交
- 组队提交

教师创建项目时可以设置最大组队人数。

学生组队后，以队伍为单位提交作品。

项目评分按核心素养维度输入数值得分，写入 `dimension_scores(source_type='project')`。

项目计入结果评价。

### 项目未提交规则

项目截止后，如果学生或队伍没有提交项目作品，则默认记为：

```text
F，即 0 分
```

未提交项目计入结果评价。

教师可以将未提交项目的学生或队伍标记为特殊情况。

特殊情况需要填写原因。

特殊情况不计入结果评价和学期总评。

### 项目评分和雷达图关系

项目、考试、平时任务都可以产生四个核心素养维度得分。雷达图可以按来源展示，也可以展示按学期考核方案折算后的总评维度分。

---

## 十七、结果评价

学期考核方案包含三个来源：

```text
平时任务
考试
项目化学习
```

教师在课程学期中设置三项占比：

```text
平时任务占比 + 考试占比 + 项目占比 = 100%
```

默认方案：

```text
平时任务 50%，考试 50%，项目 0%
```

如果一学期有多次考试或多个项目，不再给单次考试/项目设置权重；同一来源下按维度总得分率聚合：

```text
某来源某维度分 = 该来源该维度 earned_score 总和 / max_score 总和 * 100
```

最终某维度总评分：

```text
最终某维度分 =
  平时任务该维度分 * 平时任务占比
  + 考试该维度分 * 考试占比
  + 项目该维度分 * 项目占比
```

如果某来源占比为 0，则不参与计算。如果某来源占比大于 0 但暂无有效分数，应在总评预览中提示缺少对应成绩。

---

## 十八、学期总评

教师可以导出学生学期总评。

学期总评计算：

```text
学期总评 = 四个最终核心素养维度分的平均值
```

导出 Excel 时按班级分 Sheet。

Excel 字段包括：

```text
学校
班级
学号
姓名
信息意识
计算思维
数字化学习与创新
信息社会责任
过程评价分
考试评价分
项目评价分
学期总评
等级
备注
```

总评等级建议：

```text
A：90-100
B：75-89.99
C：60-74.99
D：40-59.99
E：0-39.99
```

### 学期总评缺失数据处理规则

如果学期内已经发布过程评价任务，但学生未完成，则按对应规则计分：

- 学习单未交：自动 F，即 0 分
- 课堂作品未交：自动 F，即 0 分

如果学期内已经发布结果评价任务，但学生未完成，则按对应规则计分：

- 考试缺考：0 分
- 项目未交：F，即 0 分

如果某学期完全没有发布过程评价任务，则过程评价显示为“暂无数据”。

如果某学期完全没有发布结果评价任务，则结果评价显示为“暂无数据”。

当过程评价或结果评价任一项为“暂无数据”时，系统默认不生成正式学期总评，只显示“暂无完整评价数据”。

后期可以通过系统配置支持以下策略：

- 缺失项按 0 分计算
- 缺失项不参与总评，已有项按 100% 计算
- 缺失项导致总评不可生成

第一版采用：

```text
缺失项导致总评不可生成
```

---

## 十九、雷达图

学生可以查看自己的四维度能力雷达图。

雷达图包括：

### 1. 学期雷达图

展示当前学期四个维度的平均分。

计算来源：

```text
有效评价记录中的四维度分数
```

特殊情况不计入。

### 2. 进步雷达图

展示当前学期与上一学期的对比。

计算：

```text
进步值 = 当前学期该维度平均分 - 上一学期该维度平均分
```

如果上一学期没有数据，前端应显示暂无对比数据。

---

## 二十、学生网盘

学生有个人网盘功能。

学生可以：

- 创建文件夹
- 上传文件
- 下载文件
- 预览文件
- 删除自己的文件

教师可以：

- 查看自己负责班级学生的网盘
- 预览学生文件
- 下载学生文件

教师如需删除或移动学生文件，必须记录审计日志。

网盘文件存储使用 MinIO。

数据库中保存文件元数据。

网盘支持树形目录结构。

建议默认限制：

```text
单个学生网盘容量：1GB
单个文件大小：200MB
禁止上传 exe、bat、sh 等高风险文件
```

---

## 二十一、文件系统

所有文件统一存储到 MinIO，包括：

- 课程封面
- 课程资源
- 课时资源
- 学生课堂作品
- 项目化学习作品
- 学生网盘文件
- 导出的 Excel 文件

文件下载使用 MinIO 预签名 GET URL。

大文件上传使用 MinIO 预签名 PUT URL。

文件预览使用 kkFileView。

支持预览：

- doc
- docx
- ppt
- pptx
- pdf
- html
- htm
- 图片

上传文件需要校验：

- 文件大小
- 文件后缀
- MIME 类型
- 用户权限

---

## 二十二、Excel 导入导出

### 学生导入

教师可以使用 Excel 导入学生。

导入模板字段：

```text
入学年份
班级
学号
姓名
```

导入规则：

- 如果班级不存在，自动创建班级
- 学生默认密码为 123456
- 密码使用 BCrypt 加密
- 学号全局唯一
- 教师只能导入到自己负责的班级
- 返回成功数量、失败数量和失败原因

### 总评导出

教师可以导出学生学期总评。

导出要求：

- 使用 EasyExcel
- 按班级分 Sheet
- 包含四维度分、过程分、考试分、项目分、结果分、总评和等级
- 导出操作写审计日志

---

## 二十三、班级间总体数据分析

教师可以在统计分析模块中查看自己负责班级之间的总体数据对比。

班级分析支持按以下条件筛选：

- 学校
- 课程
- 学期
- 班级
- 时间范围

班级分析指标包括：

- 班级平均过程评价分
- 班级平均结果评价分
- 班级平均学期总评分
- 信息意识平均分
- 计算思维平均分
- 数字化学习与创新平均分
- 信息社会责任平均分
- 学习单完成率
- 课堂作品提交率
- 任务未交率
- 考试平均分
- 项目平均分
- 优秀率
- 合格率
- 班级四维度雷达图对比

教师只能查看自己负责班级的数据。

---

## 二十四、审计日志

以下操作必须记录审计日志：

```text
登录失败次数过多
创建教师账号
导入学生
重置学生密码
删除课程
删除学期
删除课时
删除任务
删除考试
修改评分
标记特殊情况
导出成绩
教师操作学生网盘文件
```

审计日志字段：

```text
id
user_id
action
target_type
target_id
detail
ip
user_agent
created_at
```

---

## 二十五、数据库要求

数据库使用 PostgreSQL。使用 Flyway 管理 DDL。JSON 数据使用 JSONB。

### 命名约定

- 表名：复数 snake_case（`school_classes`，不是 `classes`）
- PK：`id BIGINT GENERATED ALWAYS AS IDENTITY`
- FK：`{表名单数}_id`（如 `course_id`, `teacher_id`）
- 时间戳：`created_at`, `updated_at`，类型 TIMESTAMP，NOT NULL DEFAULT now()
- 逻辑删除：`deleted SMALLINT NOT NULL DEFAULT 0`
- 索引：`idx_{表}_{列}`

### 已完成表（Phase 1-3a 建表）

**schools** — 学校（预留，暂未使用）
| 列 | 类型 | 说明 |
|----|------|------|
| id | BIGINT PK | |
| name | VARCHAR(200) NOT NULL | 学校名称 |
| created_at, updated_at | TIMESTAMP | |
| deleted | SMALLINT | |

**school_classes** — 班级
| 列 | 类型 | 说明 |
|----|------|------|
| id | BIGINT PK | |
| school_id | BIGINT FK 可空 | 所属学校（预留） |
| grade | VARCHAR(50) NOT NULL | 入学年份，如"2026"，配合 name 组成"2026级1班" |
| name | VARCHAR(100) NOT NULL | 班级名，如"1班" |
| created_at, updated_at | TIMESTAMP | |
| deleted | SMALLINT | |

**users** — 用户（管理员/教师/学生统一表）
| 列 | 类型 | 说明 |
|----|------|------|
| id | BIGINT PK | |
| username | VARCHAR(100) 可空 | 教师/管理员用户名（全局唯一） |
| student_no | VARCHAR(100) 可空 | 学号（全局唯一） |
| name | VARCHAR(100) NOT NULL | 真实姓名 |
| password | VARCHAR(255) NOT NULL | BCrypt 加密 |
| role | VARCHAR(20) NOT NULL | admin/teacher/student |
| school_id | BIGINT FK 可空 | 学校（预留） |
| class_id | BIGINT FK 可空 | 班级（仅学生） |
| phone | VARCHAR(30) 可空 | |
| email | VARCHAR(100) 可空 | |
| enabled | SMALLINT NOT NULL DEFAULT 1 | |
| created_at, updated_at | TIMESTAMP | |
| deleted | SMALLINT | |

**teacher_classes** — 教师-班级绑定（多对多）
| 列 | 类型 | 说明 |
|----|------|------|
| id | BIGINT PK | |
| teacher_id | BIGINT NOT NULL FK | |
| class_id | BIGINT NOT NULL FK | |
| created_at | TIMESTAMP | |

**courses** — 课程
| 列 | 类型 | 说明 |
|----|------|------|
| id | BIGINT PK | |
| name | VARCHAR(200) NOT NULL | |
| description | TEXT 可空 | |
| cover_url | VARCHAR(500) 可空 | |
| teacher_id | BIGINT NOT NULL FK→users | 创建教师 |
| created_at, updated_at | TIMESTAMP | |
| deleted | SMALLINT | |

**course_classes** — 课程-班级绑定（多对多）
| 列 | 类型 | 说明 |
|----|------|------|
| id | BIGINT PK | |
| course_id | BIGINT NOT NULL FK | |
| class_id | BIGINT NOT NULL FK | |
| created_at | TIMESTAMP | |
| UNIQUE(course_id, class_id) | | |

**semesters** — 学期
| 列 | 类型 | 说明 |
|----|------|------|
| id | BIGINT PK | |
| name | VARCHAR(200) NOT NULL | |
| start_time | TIMESTAMP NOT NULL | |
| end_time | TIMESTAMP NOT NULL | |
| course_id | BIGINT NOT NULL FK→courses | |
| created_at, updated_at | TIMESTAMP | |
| deleted | SMALLINT | |

**lessons** — 课时
| 列 | 类型 | 说明 |
|----|------|------|
| id | BIGINT PK | |
| name | VARCHAR(200) NOT NULL | |
| sort_order | INT NOT NULL DEFAULT 0 | |
| semester_id | BIGINT NOT NULL FK→semesters | |
| created_at, updated_at | TIMESTAMP | |
| deleted | SMALLINT | |

**course_resources** — 课程资源文件夹（树形结构）
| 列 | 类型 | 说明 |
|----|------|------|
| id | BIGINT PK | |
| course_id | BIGINT NOT NULL FK→courses | |
| parent_id | BIGINT FK 可空→course_resources | null=根目录 |
| name | VARCHAR(200) NOT NULL | |
| type | VARCHAR(20) NOT NULL DEFAULT 'FOLDER' | |
| sort_order | INT NOT NULL DEFAULT 0 | |
| created_at, updated_at | TIMESTAMP | |
| deleted | SMALLINT | |

**audit_logs** — 审计日志
| 列 | 类型 | 说明 |
|----|------|------|
| id | BIGINT PK | |
| user_id | BIGINT 可空 | |
| action | VARCHAR(200) NOT NULL | 操作描述 |
| target_type | VARCHAR(100) NOT NULL | 目标类型 |
| target_id | BIGINT 可空 | 目标 ID |
| detail | TEXT 可空 | 详情 |
| ip | VARCHAR(50) 可空 | |
| user_agent | TEXT 可空 | |
| created_at | TIMESTAMP | |

### 待建表（Phase 3b-7，字段为设计预期）

**lesson_resources** — 课时资源（Phase 3b，MinIO 文件）
| 列 | 类型 | 说明 |
|----|------|------|
| id | BIGINT PK | |
| lesson_id | BIGINT NOT NULL FK | |
| name | VARCHAR(200) NOT NULL | 文件名 |
| file_size | BIGINT | 文件大小（字节） |
| content_type | VARCHAR(100) | MIME 类型 |
| object_name | VARCHAR(500) | MinIO 对象名 |
| created_at | TIMESTAMP | |
| updated_at | TIMESTAMP | |
| deleted | SMALLINT | |

**tasks** — 课堂任务（Phase 4）
| 列 | 类型 | 说明 |
|----|------|------|
| id | BIGINT PK | |
| title | VARCHAR(200) NOT NULL | 任务标题 |
| type | VARCHAR(20) NOT NULL | worksheet / artifact |
| lesson_id | BIGINT NOT NULL FK→lessons | 所属课时 |
| form_schema | JSONB 可空 | 学习单 schema（仅 worksheet） |
| description | TEXT 可空 | 任务说明 |
| deadline | TIMESTAMP 可空 | 截止时间 |
| created_at, updated_at | TIMESTAMP | |
| deleted | SMALLINT | |

**submissions** — 学生提交（Phase 4-5）
| 列 | 类型 | 说明 |
|----|------|------|
| id | BIGINT PK | |
| task_id | BIGINT NOT NULL FK→tasks | |
| student_id | BIGINT NOT NULL FK→users | |
| status | VARCHAR(20) NOT NULL | submitted / graded / special |
| content | JSONB 可空 | 学习单答案 / 作品文件列表 |
| submitted_at | TIMESTAMP | |
| created_at, updated_at | TIMESTAMP | |

**evaluations** — 四维度评分（Phase 5）
| 列 | 类型 | 说明 |
|----|------|------|
| id | BIGINT PK | |
| student_id | BIGINT NOT NULL FK→users | |
| source_type | VARCHAR(20) NOT NULL | worksheet / artifact / project |
| source_id | BIGINT NOT NULL | 任务或项目 ID |
| dimension | VARCHAR(30) NOT NULL | AWARENESS/COMPUTING/DIGITAL_LEARNING/RESPONSIBILITY |
| grade | VARCHAR(1) NOT NULL | A/B/C/D/E/F |
| is_special | SMALLINT DEFAULT 0 | 特殊情况标记 |
| created_at | TIMESTAMP | |

**dimension_scores** — 逐题/逐评分项核心素养数值得分（Phase 8）
| 列 | 类型 | 说明 |
|----|------|------|
| id | BIGINT PK | |
| student_id | BIGINT NOT NULL FK→users | |
| source_type | VARCHAR(20) NOT NULL | process / exam / project |
| source_id | BIGINT NOT NULL | 对应提交记录 ID |
| question_id | VARCHAR(80) | 题目或评分项 ID |
| dimension | VARCHAR(30) NOT NULL | AWARENESS/COMPUTING/DIGITAL_LEARNING/RESPONSIBILITY |
| earned_score | DECIMAL(8,2) NOT NULL | 实际得分 |
| max_score | DECIMAL(8,2) NOT NULL | 该维度满分 |
| auto_graded | BOOLEAN NOT NULL | 是否自动批改 |
| created_at, updated_at | TIMESTAMP | |
| deleted | SMALLINT | |

**submission_feedback** — 提交批改反馈（Phase 8）
| 列 | 类型 | 说明 |
|----|------|------|
| submission_id | BIGINT PK FK→submissions | 对应学生提交 |
| teacher_id | BIGINT FK→users | 最后批改教师 |
| teacher_comment | TEXT | 整份任务总评语 |
| question_feedback | TEXT | 逐题反馈 JSON，含 questionId/comment/referenceAnswerVisible |
| graded_at | TIMESTAMP | 批改完成时间 |
| created_at, updated_at | TIMESTAMP | |

**exam_papers** — 试卷（Phase 6a）
| 列 | 类型 | 说明 |
|----|------|------|
| id | BIGINT PK | |
| title | VARCHAR(200) NOT NULL | 试卷标题 |
| content | JSONB NOT NULL | 题目 JSON |
| total_score | INT NOT NULL | 总分 |
| teacher_id | BIGINT NOT NULL FK | 创建教师 |
| created_at, updated_at | TIMESTAMP | |
| deleted | SMALLINT | |

**exams** — 考试任务（Phase 6a）
| 列 | 类型 | 说明 |
|----|------|------|
| id | BIGINT PK | |
| name | VARCHAR(200) NOT NULL | 考试名称 |
| semester_id | BIGINT NOT NULL FK | |
| paper_id | BIGINT NOT NULL FK→exam_papers | |
| start_time | TIMESTAMP NOT NULL | |
| end_time | TIMESTAMP NOT NULL | |
| weight | DECIMAL(3,2) DEFAULT 1.0 | 历史字段；总评权重由 assessment_schemes 管理 |
| deleted | SMALLINT | |
| created_at, updated_at | TIMESTAMP | |

**exam_classes** — 考试-班级绑定（Phase 6a）
| 列 | 类型 | 说明 |
|----|------|------|
| id | BIGINT PK | |
| exam_id | BIGINT NOT NULL FK | |
| class_id | BIGINT NOT NULL FK | |

**exam_submissions** — 考试提交（Phase 6a）
| 列 | 类型 | 说明 |
|----|------|------|
| id | BIGINT PK | |
| exam_id | BIGINT NOT NULL FK | |
| student_id | BIGINT NOT NULL FK | |
| answers | JSONB | 答案 |
| score | INT 可空 | 得分 |
| status | VARCHAR(20) | submitted / absent / special |
| submitted_at | TIMESTAMP | |

**projects** — 项目化学习（Phase 6b）
| 列 | 类型 | 说明 |
|----|------|------|
| id | BIGINT PK | |
| name | VARCHAR(200) NOT NULL | |
| description | TEXT | |
| semester_id | BIGINT NOT NULL FK | |
| max_team_size | INT DEFAULT 1 | 最大组队人数 |
| deadline | TIMESTAMP | |
| weight | DECIMAL(3,2) DEFAULT 1.0 | 历史字段；总评权重由 assessment_schemes 管理 |
| deleted | SMALLINT | |
| created_at, updated_at | TIMESTAMP | |

**project_teams** — 项目队伍（Phase 6b）
| 列 | 类型 | 说明 |
|----|------|------|
| id | BIGINT PK | |
| project_id | BIGINT NOT NULL FK | |
| name | VARCHAR(200) | 队伍名 |
| created_at | TIMESTAMP | |

**project_team_members** — 队伍成员（Phase 6b）
| 列 | 类型 | 说明 |
|----|------|------|
| id | BIGINT PK | |
| team_id | BIGINT NOT NULL FK | |
| student_id | BIGINT NOT NULL FK | |
| created_at | TIMESTAMP | |

**project_submissions** — 项目提交（Phase 6b）
| 列 | 类型 | 说明 |
|----|------|------|
| id | BIGINT PK | |
| project_id | BIGINT NOT NULL FK | |
| team_id | BIGINT FK 可空 | 队伍提交时有值 |
| student_id | BIGINT NOT NULL FK | |
| content | JSONB | 作品文件列表 |
| submitted_at | TIMESTAMP | |
| created_at | TIMESTAMP | |

**project_scores** — 项目评分（Phase 6b）
| 列 | 类型 | 说明 |
|----|------|------|
| id | BIGINT PK | |
| project_id | BIGINT NOT NULL FK | |
| student_id | BIGINT NOT NULL FK | |
| grade | VARCHAR(1) NOT NULL | A-F |
| is_special | SMALLINT DEFAULT 0 | |
| created_at | TIMESTAMP | |

> 当前主要项目评分入口为 `project_submissions` + `dimension_scores(source_type='project')`；`project_scores` 为历史兼容表。

**assessment_schemes** — 学期考核方案（Phase 8）
| 列 | 类型 | 说明 |
|----|------|------|
| id | BIGINT PK | |
| semester_id | BIGINT NOT NULL UNIQUE FK→semesters | |
| process_percent | INT NOT NULL DEFAULT 50 | 平时任务占比 |
| exam_percent | INT NOT NULL DEFAULT 50 | 考试占比 |
| project_percent | INT NOT NULL DEFAULT 0 | 项目占比 |
| created_at, updated_at | TIMESTAMP | |
| deleted | SMALLINT | |

约束：三项占比均在 0-100，且总和必须为 100。

**user_drive** — 学生网盘（Phase 7）
| 列 | 类型 | 说明 |
|----|------|------|
| id | BIGINT PK | |
| user_id | BIGINT NOT NULL FK | |
| parent_id | BIGINT FK 可空 | 树形目录 |
| name | VARCHAR(200) NOT NULL | |
| type | VARCHAR(20) NOT NULL | FOLDER / FILE |
| file_size | BIGINT | |
| content_type | VARCHAR(100) | |
| object_name | VARCHAR(500) | MinIO 对象名 |
| created_at, updated_at | TIMESTAMP | |
| deleted | SMALLINT | |

### 常用索引

```sql
-- users
CREATE UNIQUE INDEX idx_users_student_no ON users(student_no) WHERE student_no IS NOT NULL AND deleted = 0;
CREATE UNIQUE INDEX idx_users_username ON users(username) WHERE username IS NOT NULL AND deleted = 0;
CREATE UNIQUE INDEX idx_users_single_admin ON users(role) WHERE role = 'admin' AND deleted = 0;
CREATE INDEX idx_users_class_role ON users(class_id, role);

-- teacher_classes
CREATE INDEX idx_teacher_classes_teacher ON teacher_classes(teacher_id);
CREATE INDEX idx_teacher_classes_class ON teacher_classes(class_id);

-- courses
CREATE INDEX idx_courses_teacher ON courses(teacher_id);
CREATE INDEX idx_courses_name ON courses(name);

-- course_classes
CREATE UNIQUE INDEX idx_cc_course_class ON course_classes(course_id, class_id);
CREATE INDEX idx_course_classes_course ON course_classes(course_id);
CREATE INDEX idx_course_classes_class ON course_classes(class_id);

-- semesters
CREATE INDEX idx_semesters_course ON semesters(course_id);
CREATE INDEX idx_semesters_time ON semesters(start_time, end_time);

-- lessons
CREATE INDEX idx_lessons_semester ON lessons(semester_id);
CREATE INDEX idx_lessons_sort ON lessons(semester_id, sort_order);

-- course_resources
CREATE INDEX idx_cr_course ON course_resources(course_id);
CREATE INDEX idx_cr_parent ON course_resources(parent_id);

-- tasks
CREATE INDEX idx_tasks_lesson ON tasks(lesson_id);

-- submissions
CREATE INDEX idx_submissions_task_student ON submissions(task_id, student_id);

-- evaluations
CREATE INDEX idx_eval_student_dim_time ON evaluations(student_id, dimension, created_at);
CREATE INDEX idx_eval_source ON evaluations(source_type, source_id);

-- user_drive
CREATE INDEX idx_drive_user_parent ON user_drive(user_id, parent_id);
```

---

## 二十六、接口设计规范

接口统一前缀 `/api`，REST 风格。

### URL 设计约定

| 规则 | 示例 |
|------|------|
| CRUD 主资源 | `GET/POST /api/courses`, `GET/PUT/DELETE /api/courses/{id}` |
| 嵌套子资源 | `/api/courses/{courseId}/semesters`, `/api/semesters/{semesterId}/lessons` |
| 特殊操作 | `PUT /api/lessons/{id}/sort`, `PUT /api/resources/{id}/move` |
| 下拉列表 | `GET /api/courses/list-all`（无分页，用于选择器） |

### 分页参数

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| page | int | 1 | 页码 |
| size | int | 20 | 每页条数 |
| keyword | string | — | 模糊搜索（通常按名称） |

分页返回结构：

```json
{
  "code": 0,
  "msg": "ok",
  "data": {
    "records": [],
    "total": 100,
    "page": 1,
    "size": 10
  }
}
```

### 权限控制

- Controller 层：`@PreAuthorize("hasRole('TEACHER')")` 等方法级注解
- Service 层：所有写操作回溯到顶层资源（Course）验证所有权
- 规则：教师只能操作自己创建的课程及其子资源；学生只能查看关联班级的课程
- admin 可查看全部但不可直接操作课程（课程属于教师）

### 审计日志约定

以下操作必须调用 `auditLogService.record(action, targetType, targetId, detail)`：

- 创建/更新/删除课程 → targetType = "course"
- 创建/更新/删除学期 → targetType = "semester"
- 创建/更新/删除课时 → targetType = "lesson"
- 创建/重命名/删除/移动资源 → targetType = "course_resource"
- 审计日志写入失败不应影响主操作（catch 后 log.warn）

### 各模块端点汇总

**认证（Phase 1）**
```
POST /api/auth/login
```

**班级管理（Phase 2）**
```
GET    /api/classes                分页列表（ADMIN）
GET    /api/classes/list-all       全部下拉（ADMIN/TEACHER）
GET    /api/classes/{id}           详情（ADMIN）
POST   /api/classes                创建（ADMIN）
PUT    /api/classes/{id}           更新（ADMIN）
DELETE /api/classes/{id}           删除（ADMIN）
```

**教师管理（Phase 2）**
```
GET    /api/teachers               分页列表（ADMIN）
GET    /api/teachers/{id}          详情（ADMIN）
POST   /api/teachers               创建（ADMIN）
PUT    /api/teachers/{id}          更新（ADMIN）
GET    /api/teachers/{id}/classes  查看绑定班级（ADMIN）
POST   /api/teachers/{id}/classes  批量绑定班级（ADMIN）
DELETE /api/teachers/{id}/classes  批量解绑班级（ADMIN）
DELETE /api/teachers/{id}          删除教师（ADMIN）
```

**学生管理（Phase 2）**
```
GET    /api/students                分页列表（ADMIN/TEACHER，教师只看到负责班级）
POST   /api/students                创建学生（ADMIN/TEACHER）
PUT    /api/students/{id}           编辑学生（ADMIN/TEACHER）
DELETE /api/students/{id}           删除学生（ADMIN/TEACHER，软删除）
POST   /api/students/import         Excel 导入（ADMIN/TEACHER，multipart/form-data）
PUT    /api/students/{id}/password  重置密码（ADMIN/TEACHER）
POST   /api/students/batch/delete   批量删除（ADMIN/TEACHER）
POST   /api/students/batch/password 批量重置密码（ADMIN/TEACHER）
```

**课程管理（Phase 3a）**
```
GET    /api/courses                分页列表（ADMIN/TEACHER/STUDENT，按角色过滤）
POST   /api/courses                创建（TEACHER，可带 classIds 绑定班级）
GET    /api/courses/{id}           详情含学期列表+classIds
PUT    /api/courses/{id}           更新（TEACHER，仅创建者）
DELETE /api/courses/{id}           删除（ADMIN/TEACHER，需检查学期依赖）
```

**学期管理（Phase 3a）**
```
GET    /api/courses/{courseId}/semesters      列表（按 start_time 倒序）
POST   /api/courses/{courseId}/semesters      创建（TEACHER）
GET    /api/semesters/{id}                    详情
PUT    /api/semesters/{id}                    更新（TEACHER）
DELETE /api/semesters/{id}                    删除（TEACHER，需检查课时依赖）
```

**课时管理（Phase 3a）**
```
GET    /api/semesters/{semesterId}/lessons    列表（按 sort_order 升序）
POST   /api/semesters/{semesterId}/lessons    创建（TEACHER）
GET    /api/lessons/{id}                      详情
PUT    /api/lessons/{id}                      更新（TEACHER）
DELETE /api/lessons/{id}                      删除（TEACHER）
PUT    /api/lessons/{id}/sort                 调整顺序（TEACHER，targetIndex）
```

**课程资源（Phase 3a，文件夹树）**
```
GET    /api/courses/{courseId}/resources/tree   完整资源树
GET    /api/courses/{courseId}/resources        子资源列表（?parentId=）
POST   /api/courses/{courseId}/resources        创建文件夹（TEACHER）
PUT    /api/resources/{id}                      重命名（TEACHER）
DELETE /api/resources/{id}                      删除（TEACHER，递归软删除）
PUT    /api/resources/{id}/move                 移动（TEACHER，targetParentId+targetSortOrder）
```

**文件操作（Phase 3b）**
```
POST /api/files/upload/presigned      获取预签名 PUT URL
POST /api/files/download/presigned    获取预签名 GET URL
GET  /api/files/preview/{id}          kkFileView 预览
```

**课堂任务（Phase 4）**
```
GET    /api/lessons/{lessonId}/tasks         任务列表
POST   /api/lessons/{lessonId}/tasks         创建任务（worksheet/artifact）
GET    /api/tasks/{id}                       任务详情
PUT    /api/tasks/{id}                       编辑任务
DELETE /api/tasks/{id}                       删除任务
POST   /api/tasks/{id}/submit               学生提交（学习单答案/作品附件）
GET    /api/tasks/{id}/submissions           教师查看提交列表
GET    /api/tasks/{id}/my-submission         学生查看本人提交
GET    /api/tasks/{id}/my-result             学生查看本人批改详情
GET    /api/submissions/{id}                 查看单个提交详情
POST   /api/submissions/{id}/evaluate        教师批改提交（维度得分 + 教师反馈）
```

**评价（Phase 5）**
```
POST   /api/evaluations                     教师评分（按维度 A-E）
GET    /api/students/{id}/evaluation         学生查看自己的评价
GET    /api/students/{id}/radar              学期雷达图数据
GET    /api/students/{id}/radar/progress     进步雷达图（前后学期对比）
```

**考试（Phase 6a）**
```
GET    /api/exam-papers                      试卷列表
POST   /api/exam-papers                      创建试卷
GET    /api/exams                            考试任务列表
POST   /api/exams                            创建考试任务
POST   /api/exams/{id}/start                 学生开始考试
POST   /api/exams/{id}/submit                学生提交考试
GET    /api/exams/{id}/submissions           教师查看考试提交
```

**项目化学习（Phase 6b）**
```
GET    /api/projects                         项目列表
POST   /api/projects                         创建项目
POST   /api/projects/{id}/teams              创建队伍
POST   /api/teams/{id}/join                  加入队伍
POST   /api/projects/{id}/submit             提交项目作品
POST   /api/projects/{id}/scores             教师评分（组队同分）
```

**学生网盘（Phase 7）**
```
GET    /api/drive/tree                       个人网盘树
POST   /api/drive/folders                    创建文件夹
POST   /api/drive/files                      上传文件
DELETE /api/drive/{id}                       删除文件/文件夹
GET    /api/drive/{id}/download              下载文件
```

**成绩导出（Phase 7）**
```
GET    /api/stats/semester/{semesterId}/export   导出学期总评 Excel
```

### WebSocket 端点

```
/topic/task/{taskId}                         学习单提交实时推送（教师订阅）
/app/submit                                  学生提交消息
```

所有接口都需要考虑当前登录用户权限。

---

## 二十七、前端页面体系

> **详细规划文档：** `docs/FRONTEND_PLAN.md`（31 个页面的功能说明、路由结构、组件设计、分阶段计划）

### 页面总览

```
/login                           登录页

/admin                            管理员布局（侧边栏导航）
  /admin/classes                  班级管理
  /admin/teachers                 教师管理
  /admin/students                 学生管理

/teacher                          教师布局（侧边栏 + 顶栏，含班级选择器）
  /teacher/home                   教师工作台（仪表板）
  /teacher/courses                课程列表
  /teacher/courses/:id            课程详情
  /teacher/courses/:id/resources  课程资源管理
  /teacher/tasks                  作业与评分工作台
  /teacher/tasks/:taskId/analytics 任务数据看板
  /teacher/grading/:taskId        教师批改页
  /teacher/exams                  考试管理与提交批改
  /teacher/projects               项目管理与提交批改
  /teacher/stats                  班级数据分析
  /teacher/grade-export           学期总评导出

/student                          学生布局（侧边栏导航）
  /student/home                   我的课程
  /student/courses/:id            课程详情
  /student/courses/:id/resources  课程资源浏览
  /student/tasks/:taskId          学习单/作品提交
  /student/tasks/:taskId/result   批改详情
  /student/exams                  考试列表与答题
  /student/projects               项目列表、组队与作品提交
  /student/evaluation             学习评价
  /student/drive                  我的网盘
```

### 布局设计

| 角色 | 布局结构 | 导航方式 |
|------|----------|----------|
| 管理员 AdminLayout | 侧边栏 (230px) + 内容区 | NMenu（班级管理 / 教师管理 / 学生管理） |
| 教师 TeacherLayout | 侧边栏 + 顶栏（含班级选择器） | NMenu（工作台 / 课程 / 考试 / 项目 / 分析 / 导出） |
| 学生 StudentLayout | 侧边栏 + 顶栏 | NMenu（课程 / 考试 / 项目 / 评价 / 网盘） |

### 共享组件

| 组件 | 用途 |
|------|------|
| `CourseCard` | 课程卡片（封面、名称、描述、操作插槽） |
| `PageHeader` | 页面标题栏（标题 + 描述 + 操作按钮插槽） |
| `FileUpload` | 文件上传（拖拽、进度条、类型校验） |
| `FilePreview` | kkFileView iframe 预览 |
| `FileTree` | 文件夹树组件 |
| `RadarChart` | ECharts 雷达图封装 |

### 角色体验重点

学生端：
- 课程入口明显、当前课时清晰、待完成任务突出
- 雷达图直观、网盘易用

教师端：
- 班级筛选方便、数据统计清晰
- 批改效率高、实时汇总直观
- Excel 导入导出方便、资源管理方便

---

## 二十八、前端设计规范 — Quiet Precision

> 署名：**Tatakai** | 组件与页面清单见 `docs/FRONTEND_PLAN.md`

### 设计理念

简约、高级、不花哨。温暖克制的极简主义，自信地使用留白。无渐变、无花哨阴影、无紫色。

### 字体

- UI / 标题：**Geist**（Vercel 字体，Google Fonts 引入）
- 中文：系统默认中文字体（PingFang SC / Microsoft YaHei）
- 字重：标题 600，正文 400-500，辅助文字 500
- 标题 letter-spacing: -0.01em

### 配色

**浅色模式：**
- 页面背景 `#fafaf9`，侧边栏 `#f5f4f1`
- 主文字 `#1a1a18`，次文字 `#6b6b65`，辅助文字 `#8a8a84`
- 边框 `#eae8e4`，浅分隔 `#d8d6d0`
- 主按钮 `#1a1a18`（近黑），悬停 `#2e2e2c`

**暗色模式：**
- 页面背景 `#141412`，侧边栏 `#1a1a18`
- 主文字 `#e8e6e1`，次文字 `#8a8a84`
- 边框 `#272725`
- 主按钮 `#e8e6e1`（浅色），悬停 `#fafaf9`

### 圆角阶梯

```
4px — 小元素
6px — 默认（Tag, Badge）
8px — 输入框、按钮、菜单项
10px — 大按钮
12px — 卡片、空状态区域
16px — 登录卡片
```

### 间距

- 内容区 padding: `28px 32px`，max-width: `900px`（管理页）/ `1200px`（宽页面）
- 卡片 padding: `24px`
- 页面标题 font-size: `22px`，副标题 `14px`，间距 `6px + 36-40px`
- 顶栏高度: `52px`

### 边框与阴影

- 默认使用 1px solid border 代替阴影
- 空状态使用 1px dashed border
- 不使用 drop-shadow 或大阴影

### 动效

- 过渡: 150-200ms ease
- 悬停: border-color 变化
- 主题切换: background/color 过渡
- 无弹跳、无缩放、无渐变动画

### 组件规范

- Naive UI 全局 themeOverrides 控制（浅色 + 暗色两套）
- 自定义元素使用 `theme.isDark` computed 做条件样式
- 空状态：SVG 线框图标（stroke-width: 1.2-1.5, opacity: 0.25-0.3）+ 虚线边框
- 标签 / Badge：小号文字 + 圆角背景

### 登录页

左右双栏布局。左侧品牌区：英文小标题 + 大号标题 + 描述文字。右侧表单区：白色卡片 + 自定义暗色按钮。响应式：窄屏时上下堆叠。

---

## 二十九、开发顺序

> **进度状态与待办清单见 `PROGRESS.md`**。本节仅概述各阶段核心目标。前端页面细节见 `FRONTEND_PLAN.md`。

后端与前端交错推进，每个后端阶段完成后，下一个前端阶段交付对应 UI。前端阶段编号 F0-F6，后端阶段编号 1-7。

| 阶段 | 核心目标 | 状态 |
|------|----------|------|
| Phase 1 | 后端基础：Spring Boot + JWT + Flyway + Docker | ✅ |
| Phase 2 | 班级/教师/学生管理 + Excel 导入 | ✅ |
| Phase F0 | 前端脚手架：登录 + 三套 Layout + 路由守卫 | ✅ |
| Phase 3a | 后端：Course/Semester/Lesson + 课程资源文件夹 | ✅ |
| Phase F1 | 前端：管理员管理页 + 教师/学生课程页 | ✅ |
| Phase 3b | 后端：MinIO 文件基础设施（预签名 URL、kkFileView） | ✅ |
| Phase F2 | 前端：文件组件（Upload/Preview/Tree）+ 课程资源管理 | ✅ |
| Phase 4 | 后端：课堂任务（worksheet/artifact）+ WebSocket | ✅ |
| Phase F3 | 前端：任务创建 + 学习单填写 + 作品提交 + 实时统计 | ✅ |
| Phase 5 | 后端：四维度评价 + 过程评价计算 + 雷达图数据 | ✅ |
| Phase F4 | 前端：评分页 + 雷达图 + 学生评价页 | ✅ |
| Phase 6a | 后端：试卷 + 考试任务 + 缺考处理 | ✅ |
| Phase 6b | 后端：项目化学习 + 组队 + 评分 | ✅ |
| Phase 6c | 后端：结果评价（加权平均、"暂无数据"处理） | ✅ |
| Phase F5 | 前端：试卷编辑器 + 考试答题 + 项目组队 + 评分 | ✅ |
| Phase 7 | 后端：学生网盘 + 学期总评 Excel 导出 | ✅ |
| Phase F6 | 前端：网盘页 + 总评预览 + Excel 导出 | ✅ |

---

## 三十、开发约定

所有新功能开发必须遵守以下规则：

1. 先列出将创建或修改的文件清单，再逐个实现。
2. 代码必须完整，不可省略 import 或使用伪代码。
3. 涉及数据库时必须同时给出 Flyway SQL 迁移脚本。
4. 涉及接口时必须给出 Controller、DTO、VO、Service、ServiceImpl、Mapper、Entity。
5. 涉及前端时必须给出 API 封装、页面、组件和路由。
6. 涉及权限时必须说明权限校验逻辑（Controller 层 + Service 层双重校验）。
7. 涉及敏感操作时必须写审计日志（审计失败不影响主操作）。
8. 代码应可直接运行，不可擅自更换技术栈。
9. Entity 不直接返回前端（始终转换为 VO）。
10. 密码必须 BCrypt 加密，不可保存明文。
11. Service 层权限校验不可绕过。

### 后端文件清单（新模块标准结构）

```
modules/xxx/
├── controller/XxxController.java
├── service/XxxService.java
├── service/impl/XxxServiceImpl.java
├── mapper/XxxMapper.java
├── entity/Xxx.java
├── dto/XxxCreateDTO.java / XxxUpdateDTO.java / ...
├── vo/XxxVO.java / XxxDetailVO.java
└── enums/XxxEnum.java
```

### 前端文件清单（新功能标准结构）

```
src/
├── api/xxx.ts               # API 请求封装
├── types/api.ts              # 类型定义（追加）
├── views/<role>/XxxPage.vue  # 页面组件
├── components/Xxx.vue        # 共享组件（如有）
└── router/index.ts           # 路由（追加）
```

---

## 三十一、Phase 1 启动清单（已实现）

项目启动时需完成以下基础工程（Phase 1 已全部实现）：

1. `pom.xml` + `application.yml`
2. 基础包结构（common/ + modules/）
3. `R<T>` / `PageResult<T>` / `ErrorCode`（27 个错误码）
4. `BizException` / `GlobalExceptionHandler`
5. `SecurityConfig` + `JwtUtils` + `LoginUser`
6. `User` Entity + `UserMapper`
7. `AuthController` + `AuthService`（登录流程）
8. Flyway V1 建表：schools, school_classes, users, teacher_classes, audit_logs
9. AdminInitializer（自动创建 admin/admin123）
10. SpringDoc OpenAPI 配置
11. Docker Compose：PostgreSQL 16 + Redis 7 + MinIO + kkFileView

## 三十二、开发流程与质量保障

开发过程中应遵循系统化的开发流程：

### 开发流程

| 步骤 | 说明 |
|------|------|
| 1. 需求确认 | 明确功能范围，对照本文档和 `FRONTEND_PLAN.md` / `BACKEND_PLAN.md` |
| 2. 方案设计 | 编写实现计划（文件清单、数据流、组件树） |
| 3. 逐步实现 | 后端：Entity → Mapper → Service → Controller；前端：API → Store → 页面 → 路由 |
| 4. 编译验证 | `mvn clean compile` + `vue-tsc --noEmit` |
| 5. 功能测试 | Playwright 测试关键用户流程，检查控制台错误 |
| 6. 代码审查 | 权限漏洞、N+1 查询、安全风险 |
| 7. 文档更新 | 更新 `PROGRESS.md`、`FRONTEND_PLAN.md`、`BACKEND_PLAN.md` |

### 质量检查清单

- [ ] 多表写入添加 `@Transactional(rollbackFor = Exception.class)`
- [ ] 敏感操作写入审计日志（审计失败不抛异常）
- [ ] Service 层权限校验（不绕过 Controller 注解）
- [ ] Entity → VO 转换（不直接返回 Entity）
- [ ] 新表通过 Flyway 迁移脚本创建
- [ ] 前端类型定义与后端 DTO/VO 保持一致
- [ ] 暗色/浅色主题下均可正常使用
