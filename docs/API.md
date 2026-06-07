# API 接口文档

> **用途：** 已实现 API 端点的参考文档。包含请求/响应示例、错误码。
> **相关文档：** 完整规格见 `SPECIFICATION.md`，进度见 `PROGRESS.md`
> 后端地址: `http://localhost:8080` | Swagger: `http://localhost:8080/swagger-ui.html`

---

## 通用约定

### 响应格式

所有接口返回统一 JSON 结构 `R<T>`：

```json
{
  "code": 0,
  "msg": "ok",
  "data": {}
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| code | int | 状态码，`0` 表示成功，非 `0` 表示错误 |
| msg | string | 提示信息 |
| data | T | 业务数据（成功时返回） |

### 分页响应

分页接口返回 `PageResult<T>`：

```json
{
  "code": 0,
  "msg": "ok",
  "data": {
    "records": [],
    "total": 0,
    "page": 1,
    "size": 20
  }
}
```

### 认证方式

所有接口（除 `/api/auth/login` 外）需在请求头携带 JWT Token：

```
Authorization: Bearer <token>
```

Token 有效期：24 小时（可配置）。

### 错误码

| 错误码 | 说明 |
|--------|------|
| 0 | 成功 |
| **通用** | |
| 400 | 请求参数错误 |
| 40001 | 参数校验失败 |
| 40002 | 文件解析失败 |
| 40003 | 文件为空 |
| 40004 | Excel格式错误或缺少必要列 |
| **认证** | |
| 401 | 未登录或登录已过期 |
| 40101 | 用户名或密码错误 |
| 40102 | 学号或密码错误 |
| 40103 | 账号已被禁用 |
| **权限** | |
| 403 | 权限不足 |
| 40301 | 您不负责该班级 |
| **资源** | |
| 404 | 资源不存在 |
| 40401 | 用户不存在 |
| 40402 | 班级不存在 |
| 40403 | 教师不存在 |
| 40404 | 学生不存在 |
| **课程权限** | |
| 40310 | 无权操作该课程 |
| **课程模块** | |
| 40410 | 课程不存在 |
| 40411 | 学期不存在 |
| 40412 | 课时不存在 |
| 40413 | 资源不存在 |
| **冲突** | |
| 409 | 数据冲突 |
| 40901 | 学号已存在 |
| 40902 | 班级名称已存在 |
| 40903 | 班级下还有学生，无法删除 |
| 40904 | 班级还有关联教师，无法删除 |
| 40905 | 用户名已存在 |
| 40910 | 课程名称已存在 |
| 40911 | 该课程下学期名称已存在 |
| 40912 | 课程下还有学期，无法删除 |
| 40913 | 学期下还有课时，无法删除 |
| 40914 | 文件夹下还有子资源，无法删除 |
| **服务器** | |
| 500 | 服务器内部错误 |
| 50001 | 文件服务异常 |
| 50002 | 文件上传失败 |
| 50003 | 密码重置失败 |

---

## 接口列表

### 1. 认证 - 登录

```
POST /api/auth/login
```

**请求体 (JSON)**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| account | string | 是 | 教师/管理员用用户名，学生用学号 |
| password | string | 是 | 密码 |

> **登录逻辑**：
> - 先按 **username** 匹配 `admin` 角色
> - 再按 **username** 匹配 `teacher` 角色
> - 最后按 **student_no** 匹配 `student` 角色
> - 首次命中即返回，用户名和学号全局唯一

**请求示例**

```bash
# 管理员登录
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"account":"admin","password":"admin123"}'

# 教师登录
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"account":"teacher01","password":"123456"}'

# 学生登录
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"account":"2024001","password":"123456"}'
```

**成功响应 (200)**

```json
{
  "code": 0,
  "msg": "ok",
  "data": {
    "token": "eyJhbGciOiJIUzUxMiJ9...",
    "userId": 1,
    "username": "admin",
    "name": "系统管理员",
    "role": "admin",
    "classId": null
  }
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| token | string | JWT 令牌 |
| userId | long | 用户 ID |
| username | string | 用户名或学号 |
| name | string | 真实姓名 |
| role | string | 角色：`admin` / `teacher` / `student` |
| classId | long | 班级 ID（学生有值，其余为 null） |

**错误响应示例**

```json
// 40101 - 用户名或密码错误
{"code":40101,"msg":"用户名或密码错误"}

// 40103 - 账号已禁用
{"code":40103,"msg":"账号已被禁用"}

// 40001 - 参数校验失败
{"code":40001,"msg":"账号不能为空"}

// 400 - 不支持的 Content-Type
{"code":400,"msg":"不支持的Content-Type: text/plain"}

// 405 - 不支持的请求方法
{"code":400,"msg":"不支持的请求方法: GET"}
```

---

### 默认管理员账号

| 属性 | 值 |
|------|-----|
| 用户名 | `admin` |
| 密码 | `admin123` |
| 角色 | `admin` |

应用首次启动时自动创建，后续启动跳过。

> **注意**：此处仅列出部分常用字段，完整字段定义请参考 Swagger 文档 `http://localhost:8080/swagger-ui.html`。

---

### 3. 班级管理

所有班级接口需要 `ADMIN` 角色，`/api/classes/list-all` 同时允许 `TEACHER`。

#### 3.1 创建班级

```
POST /api/classes
```

**请求体**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| grade | string | 是 | 年级，如"三年级" |
| name | string | 是 | 班级名，如"1班" |

> 同年级下班级名必须唯一。

**请求示例**
```bash
curl -X POST http://localhost:8080/api/classes \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"grade":"三年级","name":"1班"}'
```

**成功响应**
```json
{"code":0,"msg":"ok","data":{"id":1,"grade":"三年级","name":"1班","createdAt":"...","updatedAt":"..."}}
```

---

#### 3.2 更新班级

```
PUT /api/classes/{id}
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| grade | string | 是 | 年级 |
| name | string | 是 | 班级名 |

#### 3.3 删除班级

```
DELETE /api/classes/{id}
```

> 班级下有学生或有关联教师时无法删除（分别返回 40903 / 40904）。

#### 3.4 获取班级详情

```
GET /api/classes/{id}
```

#### 3.5 分页查询班级

```
GET /api/classes?page=1&size=20&grade=三年级&keyword=1班
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| page | int | 否 | 页码，默认 1 |
| size | int | 否 | 每页条数，默认 20 |
| grade | string | 否 | 按年级筛选 |
| keyword | string | 否 | 按班级名模糊搜索 |

#### 3.6 获取全部班级（下拉列表用）

```
GET /api/classes/list-all
```

> 返回所有班级的简化列表，admin 和 teacher 均可调用。

---

### 4. 教师管理

所有教师接口需要 `ADMIN` 角色。

#### 4.1 创建教师

```
POST /api/teachers
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| username | string | 是 | 用户名（全局唯一） |
| name | string | 是 | 姓名 |
| password | string | 是 | 密码（最少6位） |

**请求示例**
```bash
curl -X POST http://localhost:8080/api/teachers \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"username":"teacher01","name":"张老师","password":"123456"}'
```

**成功响应**
```json
{"code":0,"msg":"ok","data":{"id":2,"username":"teacher01","name":"张老师","enabled":true,"classIds":[],"createdAt":"..."}}
```

---

#### 4.2 更新教师

```
PUT /api/teachers/{id}
```

> 不可修改用户名和密码（有单独流程）。

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| name | string | 否 | 姓名 |
| phone | string | 否 | 电话 |
| email | string | 否 | 邮箱 |
| enabled | boolean | 否 | 启用状态 |

#### 4.3 获取教师详情

```
GET /api/teachers/{id}
```

> 返回教师信息及负责的班级 ID 列表。

#### 4.4 分页查询教师

```
GET /api/teachers?page=1&size=20&keyword=张
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| page | int | 否 | 页码 |
| size | int | 否 | 每页条数 |
| keyword | string | 否 | 按用户名或姓名搜索 |

---

### 5. 教师班级绑定

需要 `ADMIN` 角色。

#### 5.1 查询教师负责班级

```
GET /api/teachers/{id}/classes
```

#### 5.2 批量绑定班级

```
POST /api/teachers/{id}/classes
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| classIds | long[] | 是 | 班级 ID 数组 |

> 已存在的绑定自动跳过。返回值为实际新增的绑定数。

**请求示例**
```bash
curl -X POST http://localhost:8080/api/teachers/2/classes \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"classIds":[1,2]}'
```

#### 5.3 批量解绑班级

```
DELETE /api/teachers/{id}/classes
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| classIds | long[] | 是 | 要解绑的班级 ID 数组 |

#### 5.4 删除教师

```
DELETE /api/teachers/{id}
```

需要 `ADMIN` 角色。删除前自动解绑所有班级关系，软删除用户记录并写入审计日志。

---

### 6. 学生管理

需要 `ADMIN` 或 `TEACHER` 角色。**教师只能操作自己负责班级的学生**（教师数据隔离）。

#### 6.1 分页查询学生

```
GET /api/students?page=1&size=20&classId=1&keyword=2024
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| page | int | 否 | 页码 |
| size | int | 否 | 每页条数 |
| classId | long | 否 | 按班级筛选 |
| keyword | string | 否 | 按姓名或学号搜索 |

> 教师只能查看自己负责班级的学生；管理员可查看所有。

**响应**
```json
{
  "code":0,"msg":"ok",
  "data":{
    "records":[{
      "id":3,"studentNo":"2024001","name":"张三",
      "classId":1,"grade":"三年级","className":"1班",
      "enabled":true,"createdAt":"..."
    }],
    "total":1,"page":1,"size":20
  }
}
```

---

#### 6.2 Excel 导入学生

```
POST /api/students/import
Content-Type: multipart/form-data
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| file | file | 是 | Excel 文件，表头须含：年级、班级、学号、姓名 |

> - 每行独立处理，单行失败不影响其他行
> - 班级不存在时自动创建
> - 默认密码 `123456`
> - 教师只能导入到自己负责的班级

**请求示例**
```bash
curl -X POST http://localhost:8080/api/students/import \
  -H "Authorization: Bearer $TOKEN" \
  -F "file=@students.xlsx"
```

**响应**
```json
{
  "code":0,"msg":"ok",
  "data":{
    "successCount":30,
    "failCount":2,
    "errors":[
      {"rowNum":3,"studentNo":"2024001","name":"张三","errorMsg":"学号已存在: 2024001"}
    ]
  }
}
```

---

#### 6.3 重置学生密码

```
PUT /api/students/{id}/password
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| newPassword | string | 否 | 新密码，不传则重置为 `123456` |

> 教师只能重置自己负责班级学生的密码。

**请求示例**
```bash
curl -X PUT http://localhost:8080/api/students/3/password \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{}'
```

---

#### 6.4 创建学生

```
POST /api/students
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| studentNo | string | 是 | 学号（全局唯一） |
| name | string | 是 | 姓名 |
| classId | long | 是 | 班级 ID |
| password | string | 否 | 密码，默认 `123456` |

> 教师只能创建到自己负责的班级。

**请求示例**
```bash
curl -X POST http://localhost:8080/api/students \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"studentNo":"2025001","name":"李四","classId":1}'
```

---

#### 6.5 编辑学生

```
PUT /api/students/{id}
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| name | string | 否 | 姓名 |
| classId | long | 否 | 班级 ID |
| enabled | boolean | 否 | 启用/禁用 |

---

#### 6.6 删除学生

```
DELETE /api/students/{id}
```

> 软删除。教师只能删除自己负责班级的学生。

#### 6.7 批量删除学生

```
POST /api/students/batch/delete
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| ids | long[] | 是 | 学生 ID 数组 |

#### 6.8 批量重置密码

```
POST /api/students/batch/password
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| ids | long[] | 是 | 学生 ID 数组 |
| newPassword | string | 否 | 新密码，不传则重置为 `123456` |

---

### 7. 课程管理

需要 `TEACHER` 角色进行写操作，`ADMIN`/`TEACHER`/`STUDENT` 角色可读。教师只能管理自己创建的课程。

#### 7.1 创建课程

```
POST /api/courses
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| name | string | 是 | 课程名称 |
| description | string | 否 | 课程介绍 |
| coverUrl | string | 否 | 封面图片 URL |
| classIds | long[] | 否 | 绑定的授课班级 ID |

> 课程名称在同一个教师下唯一。

**请求示例**
```bash
curl -X POST http://localhost:8080/api/courses \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"name":"Python编程基础","description":"面向初学者的Python入门课程","classIds":[1,2]}'
```

**成功响应**
```json
{"code":0,"msg":"ok","data":{"id":1,"name":"Python编程基础","teacherId":2,"teacherName":"张老师","classCount":2,"createdAt":"..."}}
```

---

#### 7.2 分页查询课程

```
GET /api/courses?page=1&size=20&keyword=Python
```

> 教师只看到自己创建的课程；学生只看到关联班级的课程；管理员可看到所有。

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| page | int | 否 | 页码，默认 1 |
| size | int | 否 | 每页条数，默认 20 |
| keyword | string | 否 | 按课程名称模糊搜索 |

---

#### 7.3 获取课程详情

```
GET /api/courses/{id}
```

> 返回课程信息（含班级绑定）和学期列表。权限：课程创建者 / 管理员 / 关联班级的学生。

**响应示例**
```json
{
  "code":0,"msg":"ok",
  "data":{
    "id":1,"name":"Python编程基础",
    "teacherId":2,"teacherName":"张老师",
    "classIds":[1,2],"classCount":2,
    "semesters":[
      {"id":1,"name":"2026年秋季学期","startTime":"...","endTime":"...","lessonCount":5}
    ],
    "createdAt":"...","updatedAt":"..."
  }
}
```

---

#### 7.4 更新课程

```
PUT /api/courses/{id}
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| name | string | 否 | 课程名称 |
| description | string | 否 | 课程介绍 |
| coverUrl | string | 否 | 封面图片 URL |
| classIds | long[] | 否 | 授课班级（传 null 不修改，传 [] 清空绑定） |

---

#### 7.5 删除课程

```
DELETE /api/courses/{id}
```

> 教师只能删除自己创建的课程，管理员可删除任何课程。课程下有学期时无法删除（返回 40912）。

---

### 8. 学期管理

需要 `TEACHER` 角色进行写操作，`TEACHER`/`STUDENT` 角色可读。操作前回溯课程验证权限。

#### 8.1 创建学期

```
POST /api/courses/{courseId}/semesters
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| name | string | 是 | 学期名称 |
| startTime | string | 是 | 开始时间，格式 `yyyy-MM-dd HH:mm:ss` |
| endTime | string | 是 | 结束时间 |

> 学期名称在同一课程下唯一。

**请求示例**
```bash
curl -X POST http://localhost:8080/api/courses/1/semesters \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"name":"2026年秋季学期","startTime":"2026-09-01 00:00:00","endTime":"2027-01-15 00:00:00"}'
```

---

#### 8.2 获取课程下的学期列表

```
GET /api/courses/{courseId}/semesters
```

> 按 start_time 倒序排列。

---

#### 8.3 其他学期操作

```
GET    /api/semesters/{id}       详情
PUT    /api/semesters/{id}       更新（参数同创建）
DELETE /api/semesters/{id}       删除（学期下有课时时返回 40913）
```

---

### 9. 课时管理

需要 `TEACHER` 角色进行写操作。操作前回溯学期→课程验证权限。

#### 9.1 创建课时

```
POST /api/semesters/{semesterId}/lessons
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| name | string | 是 | 课时名称 |

> 新建课时自动追加到末尾（sort_order = 当前最大 + 1）。

**请求示例**
```bash
curl -X POST http://localhost:8080/api/semesters/1/lessons \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"name":"第一课：认识Python"}'
```

---

#### 9.2 获取学期下的课时列表

```
GET /api/semesters/{semesterId}/lessons
```

> 按 sort_order 升序排列。

---

#### 9.3 调整课时顺序

```
PUT /api/lessons/{id}/sort
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| targetIndex | int | 是 | 目标位置（从 0 开始） |

**请求示例**
```bash
curl -X PUT http://localhost:8080/api/lessons/3/sort \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"targetIndex":0}'
```

---

#### 9.4 其他课时操作

```
GET    /api/lessons/{id}         详情
PUT    /api/lessons/{id}         更新名称
DELETE /api/lessons/{id}         删除
```

---

### 10. 课程资源管理

需要 `TEACHER` 角色写操作。课程资源为树形文件夹+文件结构（文件操作见 10.4）。

#### 10.1 获取资源树

```
GET /api/courses/{courseId}/resources/tree
```

> 返回完整嵌套树结构。

**响应示例**
```json
{
  "code":0,"msg":"ok",
  "data":[
    {"id":1,"name":"课件资料","type":"FOLDER","parentId":null,"sortOrder":1,
      "children":[
        {"id":2,"name":"第1课","type":"FOLDER","parentId":1,"sortOrder":1,"children":[]}
      ]
    }
  ]
}
```

---

#### 10.2 创建文件夹

```
POST /api/courses/{courseId}/resources
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| name | string | 是 | 文件夹名称 |
| parentId | long | 否 | 父文件夹 ID，空则创建在根目录 |

---

#### 10.3 其他资源操作

```
GET    /api/courses/{courseId}/resources?parentId=    获取子资源列表
PUT    /api/resources/{id}                            重命名
DELETE /api/resources/{id}                            删除（递归软删除子节点）
PUT    /api/resources/{id}/move                       移动（targetParentId + targetSortOrder）
```

---

#### 10.4 文件上传/下载/预览（Phase 3b）

文件通过 MinIO 预签名 URL 由前端直传，后端只存元数据。预览通过 kkFileView。

##### 10.4.1 获取预签名上传 URL

```
POST /api/files/upload/presigned
```
> TEACHER 角色。创建 CourseResource(type=FILE) 记录并返回预签名 PUT URL。

**请求体**
```json
{
  "fileName": "report.pdf",
  "contentType": "application/pdf",
  "fileSize": 1048576,
  "courseId": 1,
  "parentId": null
}
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| fileName | string | 是 | 原始文件名 |
| contentType | string | 是 | 文件 MIME 类型 |
| fileSize | long | 是 | 文件大小（字节），最大 200MB |
| courseId | long | 是 | 所属课程 ID |
| parentId | long | 否 | 父文件夹 ID，空则上传到根目录 |

**响应示例**
```json
{
  "code":0,"msg":"ok",
  "data":{
    "presignedUrl": "http://localhost:9000/edu/1/2026-05/a1b2c3d4_report.pdf?...",
    "resourceId": 15,
    "objectName": "1/2026-05/a1b2c3d4_report.pdf"
  }
}
```

前端拿到 `presignedUrl` 后直接 `PUT` 到 MinIO（Header: `Content-Type`）。

##### 10.4.2 获取文件下载 URL

```
GET /api/files/{resourceId}/download
```
> TEACHER/STUDENT 角色。返回预签名 GET URL。

**响应示例**
```json
{
  "code":0,"msg":"ok",
  "data":{
    "url": "http://localhost:9000/edu/1/2026-05/a1b2c3d4_report.pdf?..."
  }
}
```

##### 10.4.3 获取文件预览 URL

```
GET /api/files/{resourceId}/preview
```
> TEACHER/STUDENT 角色。返回 kkFileView 预览 URL。

**响应示例**
```json
{
  "code":0,"msg":"ok",
  "data":{
    "url": "http://localhost:8012/onlinePreview?url=http%3A%2F%2F..."
  }
}
```

##### 文件校验规则

| 规则 | 说明 |
|------|------|
| 文件大小 | ≤ 200MB（40005 FILE_SIZE_EXCEEDED） |
| 文件类型 | 仅允许：doc/docx/ppt/pptx/pdf/xls/xlsx/txt/html/htm/jpg/jpeg/png/gif/bmp/mp3/mp4/zip/rar（40006 FILE_TYPE_NOT_ALLOWED） |
| 对象命名 | `{courseId}/{yyyy-MM}/{uuid8}_{fileName}` — 服务端生成，消除冲突 |

##### 资源树中的文件

获取资源树（10.1）时，FILE 类型节点包含文件元数据：
```json
{
  "id":15,"name":"report.pdf","type":"FILE","parentId":1,"sortOrder":3,
  "fileSize":1048576,"contentType":"application/pdf",
  "objectName":"1/2026-05/a1b2c3d4_report.pdf",
  "children":[]
}
```

---


项目提供了 `backend/api-test.sh`，解决 Windows bash 下 curl 中文参数编码问题：

```bash
cd backend
bash api-test.sh login admin          # 管理员登录
bash api-test.sh class create         # 创建班级
bash api-test.sh class list           # 班级列表
bash api-test.sh class list-all       # 全部班级
bash api-test.sh teacher create       # 创建教师
bash api-test.sh teacher bind 2       # 绑定班级到教师ID=2
bash api-test.sh student list         # 学生列表
bash api-test.sh course create        # 创建课程
bash api-test.sh course list          # 课程列表
bash api-test.sh semester create 1    # 在课程1下创建学期
bash api-test.sh lesson create 1      # 在学期1下创建课时
```

脚本自动缓存 Token，支持在一个 shell 会话中连续调用。测试 JSON 文件在 `backend/test-payloads/` 目录下。

---

### 11. 教师密码重置

```
PUT /api/teachers/{id}/password
```
> ADMIN 角色。将教师密码重置为默认密码或指定新密码。

**请求体（可选）** `{ "newPassword": "newPass123" }` — 不传则重置为 `123456`。

---

## 状态码速查

| HTTP Status | 场景 |
|-------------|------|
| 200 | 业务正常响应（含业务错误如密码错误） |
| 400 | 参数校验失败、参数类型不匹配、请求方法不支持 |
| 401 | 未认证、Token 无效或过期 |
| 403 | 已认证但权限不足 |
| 404 | 资源不存在（需认证） |
| 415 | Content-Type 不支持 |
| 500 | 服务器内部错误 |

---

## 12. 文件管理 (Phase 3b)

```
POST   /api/files/upload/presigned      预签名 PUT URL
POST   /api/files/upload                直接上传 (multipart)
GET    /api/files/{id}/download         获取下载 URL
GET    /api/files/{id}/preview          kkFileView 预览 URL
GET    /api/files/{id}/stream           直链 URL
GET    /api/files/{id}/raw              直接下载文件流
```

## 13. 课堂任务 (Phase 4)

```
GET    /api/lessons/{lessonId}/tasks    任务列表
POST   /api/lessons/{lessonId}/tasks    创建任务
GET    /api/tasks/{id}                  任务详情
PUT    /api/tasks/{id}                  编辑任务
DELETE /api/tasks/{id}                  删除任务
```

任务创建支持两类：

- `worksheet`：题目式练习，`formSchema` 使用 JSON 字符串保存题目配置。
- `artifact`：课堂作品，`formSchema` 可保存提交方式、文件后缀限制等配置。

当前题目 schema 使用 `version: 3`：

```json
{
  "version": 3,
  "questions": [
    {
      "id": "q1",
      "type": "single",
      "stem": "题干 Markdown",
      "required": true,
      "imageUrl": "https://...",
      "options": ["A", "B"],
      "answer": "A",
      "autoGrade": true,
      "dimensionScores": [
        { "dimension": "COMPUTING", "maxScore": 5 },
        { "dimension": "DIGITAL_LEARNING", "maxScore": 3 }
      ]
    }
  ]
}
```

题型包括：

- `blank`：填空
- `single`：单选
- `multiple`：多选
- `true_false`：是非
- `short`：简答

`stem` 为唯一题干字段，前端用 Markdown/Monaco 编辑，学生端渲染 Markdown。单选、多选、是非默认可自动批改；填空、简答可选择开启自动批改。

## 14. 学生提交 (Phase 4)

```
POST   /api/tasks/{taskId}/submit       提交任务
GET    /api/tasks/{taskId}/submissions  教师查看提交列表
GET    /api/tasks/{taskId}/my-submission 学生查看自己提交
GET    /api/submissions/{id}            提交详情
GET    /api/students/{id}/submissions   学生提交历史
GET    /api/tasks/{taskId}/submission-stats 提交统计
```

## 15. WebSocket (Phase 4)

```
STOMP /ws                              实时推送
订阅 /topic/task/{taskId}              任务提交通知
```

## 16. 评价与雷达图 (Phase 5)

```
POST   /api/submissions/{id}/evaluate   教师评分
GET    /api/students/{id}/evaluations   学生评价汇总
GET    /api/students/{id}/radar         雷达图数据
POST   /api/tasks/{id}/auto-grade       自动评F
GET    /api/evaluations/grade-scores    评分对照表
```

当前评分同时支持旧的等级评价表和新的数值得分表：

- `evaluations`：保留 A-E/F 等级评价与特殊情况。
- `dimension_scores`：按来源、题目、核心素养维度记录 `earnedScore/maxScore`。

自动批改和逐题手动评分写入 `dimension_scores`。雷达图和学期总评优先按四维度数值得分折算。

## 17. 考试 (Phase 6a)

```
GET    /api/exam-papers                 试卷列表
POST   /api/exam-papers                 创建试卷
GET    /api/semesters/{id}/exams        考试列表
POST   /api/semesters/{id}/exams        创建考试
PUT    /api/exams/{id}                  更新考试
DELETE /api/exams/{id}                  删除考试
POST   /api/exams/{id}/start            开始考试
POST   /api/exams/{id}/submit           提交考试
GET    /api/exams/{id}/submissions      查看提交
PUT    /api/exam-submissions/{id}/grade 考试评分
```

考试列表会返回关联试卷的 `paperContent`，学生答题页按同一套 `version: 3` 题目 schema 渲染。选择、是非等自动批改题提交后写入 `dimension_scores`。

## 18. 项目化学习 (Phase 6b)

```
GET    /api/semesters/{id}/projects     项目列表
POST   /api/semesters/{id}/projects     创建项目
PUT    /api/projects/{id}               更新项目
DELETE /api/projects/{id}               删除项目
POST   /api/projects/{id}/teams         创建队伍
POST   /api/teams/{id}/join             加入队伍
POST   /api/projects/{id}/submit        提交项目
GET    /api/projects/{id}/submissions   教师查看项目提交
POST   /api/project-submissions/{id}/score 项目提交逐维度评分
POST   /api/projects/{id}/scores        旧项目评分接口（兼容保留）
GET    /api/projects/{id}/scores        旧项目评分查询（兼容保留）
```

项目说明当前以 JSON 文本保存扩展配置，例如提交方式、允许文件后缀和项目评分维度：

```json
{
  "text": "项目说明",
  "artifact": { "submitMode": "file", "allowedExtensions": ["zip", "py"] },
  "rubric": [
    { "dimension": "COMPUTING", "maxScore": 10 },
    { "dimension": "RESPONSIBILITY", "maxScore": 5 }
  ]
}
```

## 19. 学生网盘 (Phase 7)

```
GET    /api/drive/tree                  网盘文件树
POST   /api/drive/upload                上传文件 (multipart)
POST   /api/drive/folders               创建文件夹
DELETE /api/drive/{id}                  删除文件
GET    /api/drive/{id}/download         下载 URL
GET    /api/drive/{id}/preview          kkFileView 预览
GET    /api/drive/{id}/raw              直接下载
```

## 20. 统计导出 (Phase 7)

```
GET    /api/stats/semester/{id}/preview 总评预览
GET    /api/stats/semester/{id}/export  Excel 导出
```

## 21. 学期考核方案

```
GET    /api/semesters/{id}/assessment-scheme 获取考核方案
PUT    /api/semesters/{id}/assessment-scheme 设置考核方案
```

请求体：

```json
{
  "processPercent": 50,
  "examPercent": 50,
  "projectPercent": 0
}
```

约束：

- 三项占比均为 0-100。
- `processPercent + examPercent + projectPercent = 100`。
- 未设置时默认 `50/50/0`。

## 22. 首页聚合

```
GET    /api/dashboard/student           学生首页聚合
GET    /api/dashboard/teacher           教师工作台聚合
```

用于替代前端首页逐层请求课程、学期、课时、任务、提交记录的串行加载。学生接口返回课程、即将截止任务、最近评分；教师接口返回待评分数量、近期提交、即将截止任务等概览。

## 23. 健康检查

```
GET    /api/health                      服务状态 (公开)
```
