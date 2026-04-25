# 信息科技课堂管理系统开发总 Prompt

你现在是一名资深全栈架构师和开发助手，请协助我开发一个“信息科技课堂管理系统”。

本系统面向中小学信息科技课堂教学，用于教师进行课程管理、课堂任务管理、学习单管理、课堂作品收集、考试管理、项目化学习管理、过程评价、结果评价、学生能力雷达图分析、学生网盘管理和学期总评导出。

请在后续所有代码生成、架构设计、数据库设计、接口设计、前端页面开发中，始终遵守本文档约定。

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

后端模块包括：

- auth：登录认证
- user：用户、学生、教师管理
- school：学校和班级
- course：课程和课程资源
- semester：学期
- lesson：课时和课时资源
- task：课堂任务、学习单、课堂作品
- evaluation：四维度评价
- exam：试卷和考试
- project：项目化学习
- drive：学生网盘
- stats：统计分析、雷达图、学期总评
- realtime：WebSocket 实时汇总
- audit：审计日志

后端目录结构参考：

```text
src/main/java/com/example/edu
├── EduApplication.java
├── common
│   ├── config
│   ├── security
│   ├── exception
│   ├── result
│   ├── utils
│   └── constants
├── modules
│   ├── auth
│   ├── user
│   ├── school
│   ├── course
│   ├── semester
│   ├── lesson
│   ├── task
│   ├── evaluation
│   ├── exam
│   ├── project
│   ├── drive
│   ├── stats
│   ├── realtime
│   └── audit
└── infrastructure
    ├── minio
    ├── redis
    └── preview
```

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
学号 + 密码 + 学校ID
```

学号唯一规则：

```text
同一学校内学号唯一，不同学校可以重复。
```

因此数据库中学生唯一索引应为：

```text
school_id + student_no
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
用户名 + 密码 + 学校ID
```

教师所属学校决定了其可以管理的班级范围。

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

系统根据请求参数判断登录方式：

1. 不传 `schoolId` → 按 **username** 匹配 `admin` 角色（仅管理员可无学校登录）
2. 传 `schoolId` → 先按 **username + schoolId** 匹配 `admin`/`teacher`，未命中则按 **student_no + schoolId** 匹配 `student`

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

课程字段包括：

- 课程名
- 课程介绍
- 课程封面
- 创建教师
- 创建时间

课程需要关联授课班级。

使用关系表：

```text
course_classes
```

只有课程关联班级中的学生才能看到该课程。

---

## 七、学期和课时

教师可以在课程中创建学期。

学期字段包括：

- 学期名
- 开始时间
- 结束时间
- 所属课程

教师可以在学期中创建课时。

课时字段包括：

- 课时名称
- 所属学期
- 排序号

课时中可以添加：

- 学习资源
- 学习单任务
- 课堂作品任务

---

## 八、课程资源和课时资源

课程中有课程资源文件夹。

课程资源支持树形目录结构。

学生可以在课程资源中下载教师提供的资料。

课时资源属于某一节课。

课时资源支持：

- 上传
- 下载
- 预览

资源预览支持：

- Word
- PPT
- PDF
- HTML

文件存储使用 MinIO。

文件预览使用 kkFileView。

下载使用 MinIO 预签名 GET URL。

大文件上传使用 MinIO 预签名 PUT URL，由前端直传。

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

学习单类似问卷星表单。

学习单结构使用 JSONB 保存到：

```text
tasks.form_schema
```

学习单支持题型：

- 单选 radio
- 多选 checkbox
- 填空 text
- 长回答 textarea
- 表格 table

学习单 schema 示例：

```json
{
  "version": 1,
  "fields": [
    {
      "id": "q1",
      "type": "radio",
      "label": "Python 是什么类型的语言？",
      "options": ["解释型", "编译型", "汇编"],
      "required": true
    },
    {
      "id": "q2",
      "type": "checkbox",
      "label": "以下哪些是 Python 数据类型？",
      "options": ["int", "str", "list", "tree"]
    },
    {
      "id": "q3",
      "type": "text",
      "label": "请简述算法的基本特征",
      "maxLength": 500
    },
    {
      "id": "q4",
      "type": "table",
      "label": "填写实验数据",
      "columns": [
        {
          "key": "step",
          "label": "步骤",
          "type": "text"
        },
        {
          "key": "result",
          "label": "结果",
          "type": "text"
        }
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
  "q2": ["int", "str", "list"],
  "q3": "算法具有有穷性、确定性、可行性、输入和输出。",
  "q4": [
    {
      "step": "步骤1",
      "result": "完成"
    }
  ]
}
```

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

评分等级为六级：

| 等级 | 分数 |
| ---- | ---: |
| A    |  100 |
| B    |   80 |
| C    |   60 |
| D    |   40 |
| E    |   20 |
| F    |    0 |

教师手动评分时只能选择：

```text
A、B、C、D、E
```

学生未提交且任务截止后，系统自动评为：

```text
F
```

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

学习单和课堂作品评分可以选择 1 到 4 个维度。

即：

- 可以只评一个维度
- 可以评两个维度
- 可以评三个维度
- 可以四个维度都评

每个被评分维度选择 A-E。

未提交自动 F。

项目化学习评分使用 A-F。

如果项目为组队提交，则同一队伍成员分数相同。

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

任务权重：

```text
学习单权重 = 1.0
课堂作品权重 = 1.5
```

过程评价分：

```text
过程评价分 = 所有有效任务分数的加权平均值
```

特殊情况不计入。

如果学生某任务未提交，系统自动评 F，计入过程评价。

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
- 权重
- 是否删除

考试结果以分数为准。

考试计入结果评价。

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
- 权重

学生需要在项目中提交项目化学习作品。

项目支持：

- 单人提交
- 组队提交

教师创建项目时可以设置最大组队人数。

学生组队后，以队伍为单位提交作品。

项目评分等级：

| 等级 | 分数 |
| ---- | ---: |
| A    |  100 |
| B    |   80 |
| C    |   60 |
| D    |   40 |
| E    |   20 |
| F    |    0 |

若组队提交，则同一队伍所有成员分数相同。

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

项目化学习默认计入结果评价。

第一版项目评分只计算项目分数，不进入四维度雷达图。

如果后续需要让项目也参与四维度能力雷达图，可以扩展为项目四维度评分，并写入 `evaluations` 表。

第一版规则：

```text
雷达图主要来源于学习单和课堂作品的四维度评价。
项目和考试默认不参与雷达图。
```

---

## 十七、结果评价

结果评价来源：

```text
考试
项目化学习
```

考试和项目都可以设置权重。

结果评价分：

```text
结果评价分 = 所有有效考试和项目分数的加权平均值
```

如果没有考试或项目，结果评价分显示为“暂无数据”，不能简单按 0 分处理。

---

## 十八、学期总评

教师可以导出学生学期总评。

学期总评计算：

```text
学期总评 = 过程评价分 * 50% + 结果评价分 * 50%
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
结果评价分
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
学校
年级
班级
学号
姓名
```

导入规则：

- 如果学校不存在，自动创建学校
- 如果班级不存在，自动创建班级
- 学生默认密码为 123456
- 密码使用 BCrypt 加密
- 同一学校内学号不能重复
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

数据库使用 PostgreSQL。

使用 Flyway 管理 DDL。

JSON 数据使用 JSONB。

核心表包括：

```text
schools
school_classes
users
teacher_classes
courses
course_classes
course_resources
semesters
lessons
lesson_resources
tasks
submissions
evaluations
exam_papers
exams
exam_classes
exam_submissions
projects
project_teams
project_team_members
project_submissions
project_scores
user_drive
audit_logs
```

班级表命名为：

```text
school_classes
```

不要使用 `classes` 作为表名。

对应实体类命名为：

```text
SchoolClass
```

常用索引包括：

```sql
CREATE INDEX idx_users_school_student_no ON users(school_id, student_no);
CREATE INDEX idx_users_class_role ON users(class_id, role);
CREATE INDEX idx_teacher_classes_teacher ON teacher_classes(teacher_id);
CREATE INDEX idx_teacher_classes_class ON teacher_classes(class_id);
CREATE INDEX idx_course_classes_course ON course_classes(course_id);
CREATE INDEX idx_course_classes_class ON course_classes(class_id);
CREATE INDEX idx_lessons_semester ON lessons(semester_id);
CREATE INDEX idx_tasks_lesson ON tasks(lesson_id);
CREATE INDEX idx_submissions_task_student ON submissions(task_id, student_id);
CREATE INDEX idx_eval_student_dim_time ON evaluations(student_id, dimension, created_at);
CREATE INDEX idx_eval_source ON evaluations(source_type, source_id);
CREATE INDEX idx_drive_user_parent ON user_drive(user_id, parent_id);
```

---

## 二十六、接口设计规范

接口统一前缀：

```text
/api
```

REST 风格：

```text
GET    /api/courses
GET    /api/courses/{id}
POST   /api/courses
PUT    /api/courses/{id}
DELETE /api/courses/{id}
```

分页参数统一：

```text
page
size
keyword
```

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

所有接口都需要考虑当前登录用户权限。

---

## 二十七、前端 UI 要求

整体界面要求：

- 简洁
- 美观
- 现代
- 操作清晰
- 适合课堂教学场景
- 支持深色主题和浅色主题切换

学生端重点：

- 课程入口明显
- 当前课时清晰
- 待完成任务突出
- 雷达图直观
- 网盘易用

教师端重点：

- 班级筛选方便
- 数据统计清晰
- 批改效率高
- 实时汇总直观
- Excel 导入导出方便
- 资源管理方便

---

## 二十八、开发顺序

请按以下顺序开发，不要一次性堆所有功能。

### 阶段一：后端基础工程

完成：

1. Spring Boot 项目初始化
2. Maven 依赖
3. application.yml
4. Docker Compose 开发环境
5. PostgreSQL 连接
6. Redis 连接
7. Flyway 配置
8. 统一响应 R
9. 分页响应 PageResult
10. 错误码 ErrorCode
11. 业务异常 BizException
12. 全局异常 GlobalExceptionHandler
13. SpringDoc OpenAPI
14. Spring Security + JWT
15. 登录接口
16. User 表
17. 默认管理员初始化

### 阶段二：学校、班级、用户

完成：

1. 学校管理
2. 班级管理
3. 教师管理
4. 教师与班级绑定
5. 学生 Excel 导入
6. 学生列表查询
7. 按学校和班级筛选学生
8. 重置学生密码

### 阶段三：课程、学期、课时

完成：

1. 课程管理
2. 课程封面上传
3. 课程绑定班级
4. 课程资源文件夹
5. 学期管理
6. 课时管理
7. 课时资源上传
8. 资源下载和预览

### 阶段四：课堂任务

完成：

1. Task 模块
2. 学习单创建
3. 学习单 JSON Schema 保存
4. 学生填写学习单
5. 课堂作品任务
6. 学生提交作品附件
7. 教师查看提交
8. 教师下载/预览附件

### 阶段五：评分和雷达图

完成：

1. Evaluation 模块
2. 教师四维度评分
3. 特殊情况标记
4. 截止未交自动评 F
5. 过程评价计算
6. 学期雷达图
7. 进步雷达图

### 阶段六：实时汇总

完成：

1. WebSocket 配置
2. 学习单提交后推送
3. 教师端订阅任务 Topic
4. ECharts 实时统计展示

### 阶段七：考试和项目

完成：

1. 试卷管理
2. 考试任务
3. 考试提交
4. 考试评分
5. 项目创建
6. 项目组队
7. 项目作品提交
8. 项目评分
9. 结果评价计算

### 阶段八：网盘和总评导出

完成：

1. 学生网盘
2. 教师查看学生网盘
3. 文件预览下载
4. 学期总评计算
5. EasyExcel 导出
6. 按班级分 Sheet

---

## 二十九、代码生成要求

当我要求你生成代码时，请遵守：

1. 先说明将创建或修改哪些文件。
2. 再逐个输出完整代码。
3. 代码必须完整，不要省略 import。
4. 不要写伪代码。
5. 涉及数据库时必须同时给出 Flyway SQL。
6. 涉及接口时必须给出 Controller、DTO、VO、Service、ServiceImpl、Mapper、Entity。
7. 涉及前端时必须给出 API 封装、页面、组件和路由。
8. 涉及权限时必须说明权限校验逻辑。
9. 涉及敏感操作时必须写审计日志。
10. 代码过多时请分批生成，并告诉我下一步继续生成什么。
11. 生成的代码应尽量可直接运行。
12. 不要擅自更换技术栈。
13. 不要把 Entity 直接返回给前端。
14. 不要保存明文密码。
15. 不要绕过 Service 层权限校验。

---

## 三十、首次启动任务

如果我要你开始生成项目，请先从“后端基础工程”开始。

首次任务请生成：

1. `pom.xml`
2. `application.yml`
3. 基础包结构
4. `R<T>` 统一响应
5. `PageResult<T>` 分页响应
6. `ErrorCode`
7. `BizException`
8. `GlobalExceptionHandler`
9. `SecurityConfig`
10. `JwtUtils`
11. `LoginUser`
12. `User` 实体
13. `UserMapper`
14. `AuthController`
15. `AuthService`
16. `AuthServiceImpl`
17. 登录 DTO
18. 登录 VO
19. Flyway 初始 SQL，至少包含：
    - `schools`
    - `school_classes`
    - `users`
    - `teacher_classes`
    - `audit_logs`
20. 默认管理员初始化逻辑
21. Swagger 配置
22. Docker Compose 开发环境，包含：
    - PostgreSQL
    - Redis
    - MinIO
    - kkFileView

要求：

- 代码完整
- 可运行
- 数据库使用 PostgreSQL
- 使用 MyBatis-Plus
- 使用 JWT
- 使用 BCrypt
- 使用 Spring Security 6
- 使用 Spring Boot 3