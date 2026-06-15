# Windows Server 校园内网部署指南

本文档适用于在学校内网服务器上部署本系统，并让教师、学生通过 `http://服务器IP` 访问。

## 1. 部署架构

单台 Windows Server 上运行 Docker Desktop 或 Docker Engine，使用 Docker Compose 启动：

- `frontend`：Nginx + Vue 静态页面，默认对外端口 `80`
- `backend`：Spring Boot API，默认端口 `8080`
- `postgres`：业务数据库，仅绑定服务器本机 `127.0.0.1`
- `redis`：缓存，仅绑定服务器本机 `127.0.0.1`
- `minio`：文件对象存储，默认仅绑定服务器本机
- `kkfileview`：Office/PDF 等文件预览，默认对外端口 `8012`

校园网客户端通常只需要访问：

- 系统页面：`http://服务器IP`
- 文件预览：`http://服务器IP:8012`

## 2. 服务器要求

建议配置：

- CPU：4 核或以上
- 内存：8 GB 或以上
- 磁盘：100 GB SSD 起步，课程资源和学生作品多时建议 300 GB+
- 系统：Windows Server 2019/2022
- 网络：固定校园内网 IP，例如 `192.168.1.100`

小规模试用可以使用 2 核 4 GB，但文件预览和多人同时提交时会更容易卡顿。

## 3. 安装前置软件

在 Windows Server 上安装：

1. Git
2. Docker Desktop 或 Docker Engine，确保支持 `docker compose`
3. Node.js 22、JDK 21、Maven 3.9+ 仅本地开发需要；生产部署构建前端和后端时都会在 Docker 容器内完成

安装后在 PowerShell 中确认：

```powershell
git --version
docker --version
docker compose version
```

## 4. 首次部署

以管理员身份打开 PowerShell：

```powershell
cd C:\deploy
git clone https://github.com/p-arrot/class-manager.git
cd C:\deploy\class-manager
```

初始化内网环境变量文件：

```powershell
.\deploy.ps1 init -ServerIp 192.168.1.100
```

打开 `backend\.env`，至少修改这些值：

```env
SERVER_IP=192.168.1.100
DB_PASSWORD=请改成数据库强密码
MINIO_ROOT_PASSWORD=请改成文件服务强密码
JWT_SECRET=请改成32字符以上随机字符串
KKFILEVIEW_BASE_URL=http://192.168.1.100:8012
```

启动服务：

```powershell
.\deploy.ps1 start
```

启动成功后访问：

```text
http://192.168.1.100
http://192.168.1.100/api/health
```

也可以在服务器上执行：

```powershell
.\scripts\intranet-health-check.ps1 -BaseUrl http://192.168.1.100
```

## 5. 防火墙放行

Windows 防火墙至少放行：

- TCP `80`：系统页面
- TCP `8012`：文件预览

后端 `8080` 默认只绑定服务器本机，前端 Nginx 会代理 `/api/` 和 `/ws/`。正式使用时不建议给普通客户端暴露后端、数据库、Redis、MinIO 管理端口。

PowerShell 示例：

```powershell
New-NetFirewallRule -DisplayName "Class Manager Web" -Direction Inbound -Protocol TCP -LocalPort 80 -Action Allow
New-NetFirewallRule -DisplayName "Class Manager Preview" -Direction Inbound -Protocol TCP -LocalPort 8012 -Action Allow
```

## 6. 日常运维命令

查看状态：

```powershell
.\deploy.ps1 status
```

查看日志：

```powershell
.\deploy.ps1 logs
```

重启：

```powershell
.\deploy.ps1 restart
```

停止：

```powershell
.\deploy.ps1 stop
```

更新代码并重新部署：

```powershell
.\deploy.ps1 update
```

## 7. 备份

执行：

```powershell
.\deploy.ps1 backup
```

备份会生成到：

```text
C:\deploy\class-manager\backups\yyyyMMdd-HHmmss
```

包含：

- PostgreSQL SQL 备份
- MinIO 文件数据卷打包

建议使用 Windows 任务计划程序每天夜间执行一次备份，并把 `backups` 目录同步到另一块磁盘或 NAS。

## 8. 常见问题

### 页面能打开，但上传大文件失败

确认重新构建过前端镜像。当前 Nginx 已配置 `client_max_body_size 220m`，匹配系统 200 MB 单文件限制。

```powershell
.\deploy.ps1 restart
```

### 已经构建过，只想快速重启服务

可以跳过镜像重建：

```powershell
.\deploy.ps1 restart -SkipBuild
```

### 文件预览打不开

确认：

1. 客户端电脑能访问 `http://服务器IP:8012`
2. `backend\.env` 中 `KKFILEVIEW_BASE_URL` 是校园内网 IP，不是 `localhost`
3. `kkfileview` 容器状态正常：

```powershell
docker ps
docker logs --tail=100 edu-kkfileview
```

### 后端启动失败

查看日志：

```powershell
docker logs --tail=200 edu-backend
```

重点检查：

- `backend\.env` 的数据库密码是否和 PostgreSQL 容器一致
- `JWT_SECRET` 是否配置
- Docker volume 是否损坏
- 服务器磁盘是否已满

### 修改 IP 后访问异常

修改 `backend\.env`：

```env
SERVER_IP=新的IP
KKFILEVIEW_BASE_URL=http://新的IP:8012
```

然后重启：

```powershell
.\deploy.ps1 restart
```

## 9. 不建议的做法

- 不要在生产环境使用默认密码
- 不要把 PostgreSQL、Redis 暴露给整个校园网
- 不要只依赖 Docker volume 而不做备份
- 不要把 `.env` 提交到 Git
- 不要把系统部署在动态 IP 电脑上长期使用
