#!/bin/bash
# 一键部署脚本
# Usage: ./deploy.sh [--prod]

set -e

echo "=== 信息科技课堂管理系统 — 部署 ==="

# 1. 后端编译
echo "[1/4] 编译后端..."
cd backend
mvn package -DskipTests -q

# 2. 前端编译
echo "[2/4] 编译前端..."
cd ../frontend
npm install --silent
npm run build

# 3. Docker 构建
echo "[3/4] 构建 Docker 镜像..."
cd ../backend
docker compose build

# 4. 启动
echo "[4/4] 启动服务..."
if [ "$1" = "--prod" ]; then
  docker compose up -d --scale frontend=1
  echo ""
  echo "✅ 生产部署完成!"
  echo "   访问: http://localhost"
else
  docker compose up -d backend postgres redis minio kkfileview
  echo ""
  echo "✅ 开发部署完成!"
  echo "   后端: http://localhost:8080"
  echo "   前端: cd frontend && npm run dev  (http://localhost:5173)"
  echo "   Swagger: http://localhost:8080/swagger-ui.html"
fi

echo "   健康检查: http://localhost:8080/api/health"
