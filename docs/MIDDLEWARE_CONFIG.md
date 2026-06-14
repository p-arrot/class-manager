# 中间件配置与默认凭据清单

本文档记录 Docker Compose 部署中各个中间件/基础服务的配置来源、默认端口、数据卷和默认用户名密码。

> 注意：下列“默认值”是 `backend/docker-compose.yml` 或配置文件中的 fallback 值。实际运行时如果存在 `backend/.env`，以 `.env` 中的值为准。生产或校园内网部署必须修改所有默认密码和 `JWT_SECRET`。

## 配置文件来源

| 文件 | 作用 |
| --- | --- |
| `backend/docker-compose.yml` | 定义 Postgres、Redis、MinIO、kkFileView、后端、前端容器 |
| `backend/.env.example` | 通用环境变量模板 |
| `backend/.env.intranet.example` | Windows Server / 校园内网部署环境变量模板 |
| `backend/src/main/resources/application-docker.yml` | 后端 docker profile 下的数据库、Redis、MinIO、JWT、日志配置 |
| `frontend/nginx.conf` | 前端 Nginx 静态文件、API 代理、WebSocket 代理配置 |

## 服务总览

| 服务 | 容器名 | 镜像/构建 | 默认端口 | 默认账号 | 默认密码 |
| --- | --- | --- | --- | --- | --- |
| PostgreSQL | `edu-postgres` | `postgres:16.6-alpine` | `127.0.0.1:5432` | `edu` | `edu123` |
| Redis | `edu-redis` | `redis:7.4-alpine` | `127.0.0.1:6379` | 无 | 空 |
| MinIO | `edu-minio` | `minio/minio:latest` | `127.0.0.1:9000`, `127.0.0.1:9001` | `minioadmin` | `minioadmin` |
| kkFileView | `edu-kkfileview` | `keking/kkfileview:latest` | `8012` | 无 | 无 |
| Backend | `edu-backend` | 本地 `backend/Dockerfile` | `127.0.0.1:8080` | 不适用 | 不适用 |
| Frontend | `edu-frontend` | 本地 `frontend/Dockerfile` | `80` | 不适用 | 不适用 |

## PostgreSQL

用途：业务数据库，存储用户、班级、课程、任务、提交、评价、网盘元数据等。

Compose 配置：

| 项 | 值 |
| --- | --- |
| 镜像 | `postgres:16.6-alpine` |
| 容器名 | `edu-postgres` |
| 容器端口 | `5432` |
| 主机绑定 | `127.0.0.1:${POSTGRES_PORT:-5432}` |
| 数据卷 | `postgres_data:/var/lib/postgresql/data` |
| 健康检查 | `pg_isready -U ${DB_USERNAME:-edu}` |

环境变量：

| 变量 | 默认值 | 说明 |
| --- | --- | --- |
| `DB_USERNAME` | `edu` | 数据库用户名，对应 `POSTGRES_USER` |
| `DB_PASSWORD` | `edu123` | 数据库密码，对应 `POSTGRES_PASSWORD` |
| `DB_NAME` | `edu` | 数据库名，对应 `POSTGRES_DB` |
| `POSTGRES_PORT` | `5432` | 主机侧 PostgreSQL 端口 |

后端连接配置：

| 变量 | 默认值 |
| --- | --- |
| `DB_HOST` | `postgres` |
| `DB_PORT` | `5432` |
| `DB_USERNAME` | `edu` |
| `DB_PASSWORD` | `edu123` |
| `DB_NAME` | `edu` |

生产建议：

- 必须修改 `DB_PASSWORD`。
- 保持只绑定 `127.0.0.1`，不要暴露给校园网客户端。
- 定期备份 `postgres_data`，或使用 `pg_dump` 导出 SQL。

## Redis

用途：后端缓存/临时状态存储。

Compose 配置：

| 项 | 值 |
| --- | --- |
| 镜像 | `redis:7.4-alpine` |
| 容器名 | `edu-redis` |
| 容器端口 | `6379` |
| 主机绑定 | `127.0.0.1:${REDIS_PORT:-6379}` |
| 数据卷 | `redis_data:/data` |
| 健康检查 | `redis-cli ping` |

环境变量：

| 变量 | 默认值 | 说明 |
| --- | --- | --- |
| `REDIS_HOST` | `redis` | 后端容器内访问 Redis 的主机名 |
| `REDIS_PORT` | `6379` | Redis 端口 |
| `REDIS_PASSWORD` | 空 | 当前 compose 未给 Redis 容器设置 requirepass |
| `REDIS_DATABASE` | `0` | Redis database index |

生产建议：

- 当前 Redis 默认无密码，并且只绑定 `127.0.0.1`。不要把 Redis 端口暴露给校园网。
- 如果未来要启用 Redis 密码，需要同步修改 Redis 启动命令和后端 `REDIS_PASSWORD`。

## MinIO

用途：对象存储，保存课程资源、学生网盘文件、上传文件对象。

Compose 配置：

| 项 | 值 |
| --- | --- |
| 镜像 | `minio/minio:latest` |
| 容器名 | `edu-minio` |
| 启动命令 | `server /data --console-address ":9001"` |
| API 端口 | `127.0.0.1:${MINIO_API_PORT:-9000}:9000` |
| 控制台端口 | `127.0.0.1:${MINIO_CONSOLE_PORT:-9001}:9001` |
| 数据卷 | `minio_data:/data` |
| 健康检查 | `http://localhost:9000/minio/health/live` |

默认凭据：

| 变量 | 默认值 | 说明 |
| --- | --- | --- |
| `MINIO_ROOT_USER` | `minioadmin` | MinIO root 用户 |
| `MINIO_ROOT_PASSWORD` | `minioadmin` | MinIO root 密码 |
| `MINIO_BUCKET` | `edu` | 后端使用的 bucket |
| `MINIO_API_PORT` | `9000` | API 端口 |
| `MINIO_CONSOLE_PORT` | `9001` | 控制台端口 |

后端连接配置：

| 变量 | 默认值 |
| --- | --- |
| `MINIO_ENDPOINT` | `http://minio:9000` |
| `MINIO_ACCESS_KEY` | `${MINIO_ROOT_USER:-minioadmin}` |
| `MINIO_SECRET_KEY` | `${MINIO_ROOT_PASSWORD:-minioadmin}` |
| `MINIO_BUCKET` | `edu` |

生产建议：

- 必须修改 `MINIO_ROOT_PASSWORD`。
- 默认仅绑定服务器本机，普通客户端不需要直接访问 MinIO。
- 文件预览通过后端生成预签名 URL，再交给 kkFileView 访问。

## kkFileView

用途：Office、PDF 等文件在线预览。

Compose 配置：

| 项 | 值 |
| --- | --- |
| 镜像 | `keking/kkfileview:latest` |
| 容器名 | `edu-kkfileview` |
| 端口 | `${KKFILEVIEW_PORT:-8012}:8012` |
| 默认账号密码 | 当前项目未配置 |

环境变量：

| 变量 | 默认值 | 说明 |
| --- | --- | --- |
| `KKFILEVIEW_PORT` | `8012` | 主机侧访问端口 |
| `KKFILEVIEW_BASE_URL` | `http://localhost:8012` | 后端返回给浏览器的预览服务地址 |

内网部署建议：

- `KKFILEVIEW_BASE_URL` 必须改为校园内网可访问地址，例如 `http://192.168.1.100:8012`。
- Windows 防火墙需要放行 `KKFILEVIEW_PORT`。

## Backend

用途：Spring Boot API 服务。

Compose 配置：

| 项 | 值 |
| --- | --- |
| Dockerfile | `backend/Dockerfile` |
| 容器名 | `edu-backend` |
| 容器端口 | `8080` |
| 主机绑定 | `127.0.0.1:${BACKEND_PORT:-8080}:8080` |
| 数据卷 | `backend_data:/app/data` |
| Profile | `docker` |

关键环境变量：

| 变量 | 默认值 | 说明 |
| --- | --- | --- |
| `SPRING_PROFILES_ACTIVE` | `docker` | Spring profile |
| `BACKEND_PORT` | `8080` | 主机侧后端端口 |
| `JWT_SECRET` | `dev-only-change-this-secret-32chars-min` | JWT 签名密钥，compose fallback |
| `APP_LOG_LEVEL` | `info` | 应用日志级别 |
| `SECURITY_LOG_LEVEL` | `warn` | Spring Security 日志级别 |

生产建议：

- 必须修改 `JWT_SECRET`，长度至少 32 字符，建议使用随机字符串。
- 后端默认只绑定 `127.0.0.1`，由前端 Nginx 代理 `/api/` 和 `/ws/`。
- 不建议直接向校园网暴露 `8080`。

## Frontend / Nginx

用途：Vue 静态页面、API 反向代理、WebSocket 代理。

Compose 配置：

| 项 | 值 |
| --- | --- |
| Dockerfile | `frontend/Dockerfile` |
| 容器名 | `edu-frontend` |
| 端口 | `${FRONTEND_PORT:-80}:80` |

Nginx 配置：

| 路径 | 行为 |
| --- | --- |
| `/` | 静态文件，`try_files $uri $uri/ /index.html` |
| `/api/` | 代理到 `http://backend:8080` |
| `/ws/` | WebSocket 代理到 `http://backend:8080` |

关键配置：

| 配置 | 值 |
| --- | --- |
| `client_max_body_size` | `220m` |
| `proxy_read_timeout` | `300s` |
| `proxy_send_timeout` | `300s` |

生产建议：

- 校园网客户端通常只需要访问前端端口 `80`。
- 如果改为非 80 端口，同步修改防火墙和访问地址。

## 应用内置账号

这些不是中间件账号，但属于部署后可能出现的默认应用账号，需要单独记录。

| 来源 | 账号 | 密码 | 说明 |
| --- | --- | --- | --- |
| `AdminInitializer` | `admin` | `admin123` | 当数据库中不存在管理员时自动创建 |
| `backend/test-data.sql` | `zhang` / `li` / `wang` / `chen` | `teacher123` | 测试教师账号，仅在手动导入测试数据后存在 |
| `backend/test-data.sql` | 示例学号如 `2024001` | `123456` | 测试学生账号，仅在手动导入测试数据后存在 |

生产建议：

- 首次登录后立即修改 `admin / admin123`。
- 生产数据库不要导入 `backend/test-data.sql`，除非明确是演示环境。

## 默认值速查

| 配置项 | 默认值 |
| --- | --- |
| 前端访问地址 | `http://服务器IP:${FRONTEND_PORT:-80}` |
| 后端健康检查 | `http://127.0.0.1:${BACKEND_PORT:-8080}/api/health` |
| PostgreSQL | `edu / edu123 @ 127.0.0.1:5432/edu` |
| Redis | `127.0.0.1:6379`，无密码 |
| MinIO API | `http://127.0.0.1:9000` |
| MinIO Console | `http://127.0.0.1:9001` |
| MinIO root | `minioadmin / minioadmin` |
| kkFileView | `http://服务器IP:8012` |
| 应用管理员 | `admin / admin123` |

## 上线前必须修改

- `DB_PASSWORD`
- `MINIO_ROOT_PASSWORD`
- `JWT_SECRET`
- 应用管理员 `admin` 的密码
- `SERVER_IP`
- `KKFILEVIEW_BASE_URL`

## 不应提交到 Git 的文件

- `backend/.env`
- 数据库备份文件
- MinIO 数据备份包
- 任何包含真实密码、JWT 密钥、生产 IP 的临时记录
