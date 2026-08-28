# Class Manager 信息科技课堂管理系统

面向信息科技课程的课堂管理系统，覆盖一个学期内的建班建课、课程资源、任务发布、学生提交、教师批改、考试项目、学生网盘、学习评价、数据分析和成绩导出等流程。

项目主要面向学校内网单机部署。生产环境推荐使用 Docker Compose 一次性启动前端、后端、PostgreSQL、MinIO 和 kkFileView；Redis 为可选缓存组件。

## 功能特性

- 管理员：班级管理、教师管理、学生管理、学生导入、密码重置
- 教师：课程、学期、课次、课程资源、任务发布、批改收件箱、考试、项目、学生档案、学生网盘、数据分析、成绩导出
- 学生：首页待办、我的课程、课程资源、任务提交、考试入口、项目提交备注、学习评价、个人网盘
- 批改：支持学习单逐题评分、逐题评语、教师总评、维度评价和学生查看批改详情
- 文件：通过 MinIO 存储课程资源、学生作品和网盘文件，通过 kkFileView 进行文件预览
- 部署：Docker 优先，使用 `docker compose` 一条命令启动全部服务

## 技术栈

| 层级 | 技术 |
| --- | --- |
| 前端 | Vue 3、TypeScript、Vite、Pinia、Vue Router、Naive UI、ECharts |
| 后端 | Spring Boot 3、Java 21、MyBatis-Plus、Spring Security、Flyway |
| 数据 | PostgreSQL；可选 Redis 缓存 |
| 文件 | MinIO、kkFileView |
| 部署 | Docker Compose、Nginx |

## 目录结构

```text
backend/    Spring Boot API、数据库迁移、Docker Compose
frontend/   Vue 前端、生产 Nginx 镜像
docs/       产品、API、部署、中间件和用户操作文档
scripts/    Docker 测试辅助脚本
```

## 快速部署：校园内网 Docker 部署

复制内网环境变量模板（模板内默认 IP 为 `192.168.1.100`，替换成服务器实际 IP）：

```powershell
copy backend\.env.intranet.example backend\.env
```

打开 `backend/.env`，至少修改：

- `DB_PASSWORD`
- `MINIO_ROOT_PASSWORD`
- `JWT_SECRET`
- `INITIAL_ADMIN_PASSWORD`
- `APP_TIME_ZONE`（默认 `Asia/Shanghai`，用于考试、任务截止时间等服务端时间判断）
- `SERVER_IP`
- `KKFILEVIEW_BASE_URL`

启动系统：

```powershell
cd backend
docker compose up -d --build
```

访问：

```text
http://192.168.1.100
```

详细步骤见 [docs/INTRANET_WINDOWS_DEPLOYMENT.md](docs/INTRANET_WINDOWS_DEPLOYMENT.md)。

## 本地开发

后端：

```powershell
cd backend
docker compose up -d postgres minio kkfileview
mvn test
mvn spring-boot:run
```

需要启用可选 Redis 缓存时，将 `CACHE_TYPE=redis`、`APP_CACHE_REDIS_ENABLED=true` 写入 `.env`，并使用 `docker compose --profile cache up -d`。

前端：

```powershell
cd frontend
npm install
npm run dev
```

前端开发服务运行在 `http://localhost:5173`，并将 `/api` 代理到 `http://localhost:8080`。Docker profile 启动时必须显式提供 `JWT_SECRET` 和 `INITIAL_ADMIN_PASSWORD`。

## 测试

前端质量检查：

```powershell
cd frontend
npm run check
```

后端测试可以使用本机 Maven，也可以使用带 Maven 缓存的 Docker 辅助脚本：

```powershell
powershell -ExecutionPolicy Bypass -File scripts\backend-test-docker.ps1
```

## 文档

- [中间件配置](docs/MIDDLEWARE_CONFIG.md)
- [Windows 校园内网部署](docs/INTRANET_WINDOWS_DEPLOYMENT.md)
- [API 说明](docs/API.md)
- [需求规格](docs/SPECIFICATION.md)
- [开发进度](docs/PROGRESS.md)
- [用户学期流程测试报告](docs/USER_SEMESTER_FLOW_REPORT.md)

## 安全提醒

不要提交真实 `.env` 文件、生产 IP、JWT 密钥、数据库密码、MinIO 密码、备份文件或真实学生数据。漏洞报告和部署加固建议见 [SECURITY.md](SECURITY.md)。

生产或校园内网部署前，务必修改：

- 应用默认管理员密码
- `DB_PASSWORD`
- `MINIO_ROOT_PASSWORD`
- `JWT_SECRET`

## 许可证

本项目使用 [MIT License](LICENSE)。
