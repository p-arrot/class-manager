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

##### 10.4.4 上传课程封面

课程封面不进入课程资源树，上传后返回稳定只读地址，前端写入课程 `coverUrl`。

```
POST /api/files/course-cover/upload
Content-Type: multipart/form-data
```

权限：`ADMIN` / `TEACHER`

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| file | file | 是 | 图片文件，支持 jpg/png/webp/gif，最大 5MB |

**响应示例**
```json
{
  "code": 0,
  "msg": "ok",
  "data": {
    "url": "/api/files/course-cover/Y291cnNlLWNvdmVycy8yMDI2LTA2L2FiY19jb3Zlci5wbmc"
  }
}
```

封面读取：

```
GET /api/files/course-cover/{token}
```

该接口公开只读，用于课程卡片 `<img>` 直接加载。

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
GET    /api/tasks/{taskId}/my-result     学生查看自己的批改详情
GET    /api/submissions/{id}            提交详情
GET    /api/students/{id}/submissions   学生提交历史
GET    /api/tasks/{taskId}/submission-stats 提交统计
GET    /api/tasks/{taskId}/analytics     教师任务数据看板
```

`GET /api/tasks/{taskId}/my-result` 用于学生端批改详情页：

- 权限：仅 `STUDENT` 可查看自己的提交；教师仍使用提交列表/批改接口查看班级提交。
- 安全要求：必须按当前登录学生和 `taskId` 查找提交，不能让学生通过 `submissionId` 枚举其他同学提交。
- 状态值：API 使用现有小写值 `submitted/graded/special`；`not_submitted` 仅作为 `my-result` 包装层状态，不写入 `submissions.status`。
- 未提交：返回任务基本信息、`status=not_submitted`、`submission=null`。
- 已提交未批改：返回学生答案、提交时间和 `status=submitted`，不返回空分数；如存在自动题预评分，可返回 `autoGraded=true` 的题目结果，但前端必须展示为“自动题预评分/等待教师批改”。
- 已批改：返回任务题目、学生答案、逐题得分、维度得分拆分、总分、批改时间、教师总评语。
- 特殊处理：返回 `status=special`、特殊说明/总评语和批改时间；前端展示“不计入评价/特殊处理”，不参与维度总分汇总。
- 数据来源：逐题/维度得分优先读取 `dimension_scores`，保留兼容 `evaluations` 的等级字段。
- 前端消费目标：学生首页、课时任务列表、提交成功页、学习评价页统一跳转到 `/student/tasks/:taskId/result`。

前端渲染契约：

| status | 页面主状态 | 分数/维度展示 | 操作入口 |
|--------|------------|---------------|----------|
| `not_submitted` | 未提交 | 不展示分数 | 返回课时；若未逾期，引导去填写/提交 |
| `submitted` | 已提交待批改 | 隐藏人工分数；自动题仅标记为预评分 | 返回课时/首页 |
| `graded` | 已批改 | 展示总分、逐题得分、维度拆分、教师评语 | 返回课时/学习评价 |
| `special` | 特殊处理 | 不计入评价；展示原因/总评 | 返回学习评价 |

异常状态：

- 401：跳转登录。
- 403：显示“只能查看自己的提交批改详情”，不得渲染任何提交内容。
- 404：显示“任务不存在或已被删除”。
- 5xx/网络错误：显示重试按钮，保留返回入口。

响应结构建议：

```json
{
  "task": {
    "id": 1,
    "title": "课堂练习：Python 条件判断小测",
    "type": "worksheet",
    "courseId": 1,
    "courseName": "七年级信息科技：Python 入门",
    "lessonId": 1,
    "lessonName": "第 6 课 条件判断与分支结构"
  },
  "status": "graded",
  "submission": {
    "id": 50,
    "status": "graded",
    "content": "{\"q1\":\"score >= 60\"}",
    "submittedAt": "2026-06-13T19:32:20",
    "gradedAt": "2026-06-13T19:38:42",
    "teacherComment": "条件和结果分析清楚，表达可以更完整。"
  },
  "questions": [
    {
      "id": "q1",
      "index": 1,
      "type": "blank",
      "stem": "在 Python 中，判断分数大于等于 60 应使用哪一个表达式？",
      "autoGrade": true,
      "referenceAnswerVisible": true,
      "referenceAnswer": "score >= 60"
    }
  ],
  "answers": {
    "q1": "score >= 60"
  },
  "questionResults": [
    {
      "questionId": "q1",
      "correct": true,
      "autoGraded": true,
      "earnedScore": 5,
      "maxScore": 5,
      "comment": null,
      "dimensionScores": [
        { "dimension": "COMPUTING", "earnedScore": 4, "maxScore": 4 },
        { "dimension": "AWARENESS", "earnedScore": 1, "maxScore": 1 }
      ]
    }
  ],
  "dimensionSummary": [
    { "dimension": "COMPUTING", "earnedScore": 8, "maxScore": 10, "rate": 0.8, "grade": "B" }
  ]
}
```

数据落点：

- `dimension_scores` 继续作为逐题/维度数值得分来源，不承载教师评语。
- 新增迁移 `V14__submission_feedback.sql`：
  - `submission_feedback.submission_id`：主键，关联 `submissions.id`。
  - `teacher_id`：最后批改教师。
  - `teacher_comment TEXT`：整份任务总评语。
  - `question_feedback TEXT`：JSON 数组，元素包含 `questionId/comment/referenceAnswerVisible`。
  - `graded_at TIMESTAMP`：批改完成时间。
  - `created_at/updated_at`：审计时间。
- `EvaluateDTO` 增加 `teacherComment` 和 `questionFeedback`，其中 `questionFeedback` 与 `questionScores` 按 `questionId` 对齐。
- 自动批改只写 `dimension_scores`；教师保存批改时写入/覆盖 `submission_feedback`，并将 `submissions.status` 置为 `graded` 或 `special`。

配套 API 约定：

- `GET /api/submissions/{id}` 已收紧：学生角色只能读取本人提交；教师角色只能读取自己负责班级/课程下的提交。
- 教师数据看板使用 `GET /api/tasks/{taskId}/analytics`。当前约定：`submittedCount` 表示“已提交待批改”人数，`gradedCount` 表示“已评分”人数，`specialCount` 表示特殊处理人数，`notSubmittedCount` 表示未提交人数；`submissionRate` 按 `(submittedCount + gradedCount + specialCount) / totalStudents` 计算。
- `TaskAnalyticsVO.submissions` 是教师批改收件箱行，不再只是已提交记录。每个应完成学生都会返回一行：
  - 已提交/已批改/特殊处理：`submissionId/status/content/submittedAt` 来自 `submissions` 表。
  - 未提交：`submissionId=null`、`status=not_submitted`、`content=null`、`submittedAt=null`。
  - 每行包含 `studentId/studentName/studentNo/classId/className`，前端按班级展示并决定是否允许进入批改。
- 评分接口持久化 `gradedAt`、`teacherComment`、逐题 `comment`，供学生批改详情展示。
- 时间字段统一约定：后端 `LocalDateTime` 返回 `yyyy-MM-dd'T'HH:mm:ss`，语义为 `Asia/Shanghai` 课堂本地时间；前端统一通过 `formatDate` 展示无时区字符串，不做浏览器时区隐式换算。

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

`GET /api/students/{id}/evaluations?semesterId=...` 返回所选学期内该学生已评分任务的维度记录。查询必须先定位该学期课时下的任务，再按学生在这些任务下的提交 ID 过滤评价，不能把其他学期的评分混入当前页面。

评价记录字段补充：

| 字段 | 说明 |
|------|------|
| `sourceType` | `worksheet/artifact/project`，课堂任务当前为 `worksheet/artifact` |
| `sourceId` | 当前课堂任务评价中为 `submissionId` |
| `submissionId` | 学生提交 ID |
| `taskId` | 可跳转到 `/student/tasks/{taskId}/result` 的任务 ID |
| `taskTitle` | 任务标题，供学习评价页展示任务批改明细 |
| `taskStatus` | `submitted/graded/special`，供前端展示待批改/已批改/特殊处理 |
| `dimension/grade/score/label` | 原有维度、等级、分数、中文标签 |
| `evaluatedAt` | 评价创建时间 |

`GET /api/students/{id}/submissions?semesterId=...` 的 `SubmissionVO` 同步返回 `taskTitle/taskType`，学习评价页使用提交记录生成批改详情入口；已批改和特殊处理进入“查看批改详情”，待批改进入“查看提交状态”。

## 17. 考试 (Phase 6a)

```
GET    /api/exam-papers                 试卷列表
POST   /api/exam-papers                 创建试卷
GET    /api/semesters/{id}/exams        考试列表
POST   /api/semesters/{id}/exams        创建考试
GET    /api/exams/{id}                  考试详情
PUT    /api/exams/{id}                  更新考试
DELETE /api/exams/{id}                  删除考试
POST   /api/exams/{id}/start            开始或恢复考试
PUT    /api/exams/{id}/draft            保存答题草稿
GET    /api/exams/{id}/my-submission    查看本人答题记录
POST   /api/exams/{id}/submit           提交考试
GET    /api/exams/{id}/submissions      查看提交
PUT    /api/exam-submissions/{id}/grade 考试评分
PUT    /api/exam-submissions/{id}/return 退回修改
```

考试列表会返回关联试卷的 `paperContent`，学生答题页按同一套 `version: 3` 题目 schema 渲染。选择、是非等自动批改题提交后写入 `dimension_scores`。

`GET /api/exams/{id}/submissions` 返回教师批改收件箱，不再只是已提交记录：

- 接口会按 `Exam -> Semester -> Course` 校验课程归属，再读取课程绑定班级下的所有学生。
- 已提交、已批改、缺考学生返回真实 `id/submissionId/answers/submittedAt/score/status`。
- 未提交学生也返回一行，`id/submissionId/answers/submittedAt/score` 为空，`status=not_submitted`。
- 每行包含 `studentId/studentName/studentNo/classId/className`，前端据此按班级展示并禁止未提交行保存批改。

`PUT /api/exam-submissions/{id}/grade` 请求体：

```json
{
  "score": 88,
  "absent": false,
  "dimensionScores": [
    {
      "questionId": "q1",
      "dimension": "COMPUTING",
      "earnedScore": 8,
      "maxScore": 10,
      "autoGraded": false
    }
  ]
}
```

说明：

- `absent=true` 时后端写入 `status=absent`、`score=0`，并清空该提交已有的考试维度得分。
- `dimensionScores` 会写入 `dimension_scores(source_type='exam', source_id=submissionId)`，供雷达图和学期总评复用。
- 教师查看提交和批改前，服务层会按 `Exam -> Semester -> Course` 校验课程归属；非任课教师不能查看或批改其他课程考试提交。
- 学生开始考试后状态为 `in_progress`；草稿可反复覆盖。正式提交后，只有 `submitted` 或教师退回后的 `returned` 状态能在截止前修改。
- `PUT /api/exam-submissions/{id}/return` 必须提交 `reason`，会清除总分和维度分；已批改、缺考、特殊处理状态不能由学生直接覆盖，冲突返回 HTTP 409。

## 18. 项目化学习 (Phase 6b)

```
GET    /api/semesters/{id}/projects     项目列表
POST   /api/semesters/{id}/projects     创建项目
GET    /api/projects/{id}               项目详情
PUT    /api/projects/{id}               更新项目
DELETE /api/projects/{id}               删除项目
GET    /api/projects/{id}/my-submission 查看本人项目提交
POST   /api/projects/{id}/submit        提交项目
GET    /api/projects/{id}/submissions   教师查看项目提交
POST   /api/project-submissions/{id}/score 项目提交逐维度评分（主入口）
PUT    /api/project-submissions/{id}/return 退回项目修改
```

项目权限约定：

- 项目继承课程权限链路：`Project -> Semester -> Course`。
- 项目仅支持个人提交；学生必须属于项目所在课程绑定班级。协作成员通过提交内容中的 `note` 备注说明，不产生队伍关系或队伍成绩。
- 教师查看提交和逐维度评分时，必须是项目所在课程创建教师；管理员保留管理权限。
- 历史队伍接口和 `/api/projects/{id}/scores` 已删除；评分统一通过 `/api/project-submissions/{id}/score` 写入 `dimension_scores(source_type='project')`。

`GET /api/projects/{id}/submissions` 返回教师项目批改收件箱：

- 接口按 `Project -> Semester -> Course` 校验课程归属，再读取课程绑定班级下的所有学生。
- 已提交学生返回真实 `id/submissionId/content/submittedAt`；未提交学生也返回一行，`id/submissionId/content/submittedAt/score` 为空。
- 每行包含 `studentId/studentName/studentNo/classId/className/status/score`。
- `status=not_submitted` 表示未提交，`submitted` 表示待评分，`graded` 表示已评分，`returned` 表示已退回；详情同时返回 `returnReason/revisionCount/canResubmit`。
- `score` 为该项目提交已保存维度得分的 `earnedScore` 汇总，前端只用于展示状态和复核参考，最终评分仍通过 `POST /api/project-submissions/{id}/score` 保存。
- 评分后学生提交被锁定；教师退回必须填写原因，退回时清除旧维度分，学生在截止前重新提交后恢复为 `submitted`。

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

权限：

- `ADMIN` 可预览/导出任意学期总评。
- `TEACHER` 只能预览/导出自己课程下的学期总评；服务层会按 `semester -> course -> CoursePermissionHelper` 校验归属。

预览响应字段：

| 字段 | 说明 |
|------|------|
| `studentId` | 学生 ID |
| `school` | 学校字段，当前预留 |
| `className` | 班级名称 |
| `studentNo` | 学号 |
| `studentName` | 姓名 |
| `awareness/computing/digitalLearn/responsibility` | 四个核心素养维度最终分 |
| `processScore/examScore/projectScore` | 平时任务、考试、项目来源分 |
| `resultScore` | 结果评价分，按考试/项目折算 |
| `totalScore` | 学期总评分 |
| `totalGrade` | 学期总评等级 |
| `remark` | 缺失数据或无法生成完整总评的原因 |

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

用于替代前端首页逐层请求课程、学期、课时、任务、提交记录的串行加载。学生接口返回课程、即将截止任务、最近评分；教师接口返回待批改数量、近期提交、即将截止任务等概览。

## 23. 健康检查

```
GET    /api/health                      服务状态 (公开)
```
