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

| 错误码 | HTTP 状态码 | 说明 |
|--------|------------|------|
| 0 | 200 | 成功 |
| 400 | 400/415 | 请求参数错误 |
| 40001 | 400 | 参数校验失败 |
| 401 | 401 | 未登录或登录已过期 |
| 40101 | 200 | 用户名或密码错误 |
| 40103 | 200 | 账号已被禁用 |
| 403 | 403 | 权限不足 |
| 40301 | 200 | 教师不负责该班级 |
| 404 | 404 | 资源不存在 |
| 40401 | 200 | 用户不存在 |
| 40901 | 200 | 该学校内学号已存在 |
| 500 | 500 | 服务器内部错误 |

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
| schoolId | long | 否 | 教师和学生登录时必填，管理员可省略 |

> **登录逻辑**：
> - `schoolId` 为空 → 仅按 **username** 匹配 `admin` 角色
> - `schoolId` 非空 → 先按 **username + school_id** 匹配 `admin`/`teacher`，未命中再按 **student_no + school_id** 匹配 `student`

**请求示例**

```bash
# 管理员登录
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"account":"admin","password":"admin123"}'

# 学生登录
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"account":"2024001","password":"123456","schoolId":1}'
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
    "schoolId": null,
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
| schoolId | long | 学校 ID（可为 null） |
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
