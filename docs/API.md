# API 接口文档

> 信息科技课堂管理系统 v1.0.0 | 后端地址: `http://localhost:8080` | Swagger: `http://localhost:8080/swagger-ui.html`

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
| **冲突** | |
| 409 | 数据冲突 |
| 40901 | 学号已存在 |
| 40902 | 班级名称已存在 |
| 40903 | 班级下还有学生，无法删除 |
| 40904 | 班级还有关联教师，无法删除 |
| 40905 | 用户名已存在 |
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

### 7. 测试辅助脚本

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
```

脚本自动缓存 Token，支持在一个 shell 会话中连续调用。测试 JSON 文件在 `backend/test-payloads/` 目录下。

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
