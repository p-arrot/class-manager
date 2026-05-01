#!/bin/bash
# API 测试辅助脚本 — 解决 Windows bash 下 curl 中文参数编码问题
# 用法:
#   ./api-test.sh login admin                    # 管理员登录
#   ./api-test.sh login teacher                   # 教师登录
#   ./api-test.sh class create                    # 创建班级
#   ./api-test.sh class list                      # 班级列表
#   ./api-test.sh class list-all                  # 全部班级（教师也可）
#   ./api-test.sh teacher create                  # 创建教师
#   ./api-test.sh teacher list                    # 教师列表
#   ./api-test.sh teacher bind 2                  # 绑定班级到教师ID=2
#   ./api-test.sh student list                    # 学生列表

BASE_URL="http://localhost:8080"
PAYLOADS="$(dirname "$0")/test-payloads"

# 登录并缓存 token
login() {
    local role="${1:-admin}"
    local payload
    case "$role" in
        admin)    payload="$PAYLOADS/login-admin.json" ;;
        teacher)  payload="$PAYLOADS/login-teacher01.json" ;;
        *)        echo "未知角色: $role"; exit 1 ;;
    esac
    local resp=$(curl -s -X POST "$BASE_URL/api/auth/login" \
        -H "Content-Type: application/json" \
        --data-binary "@$payload")
    echo "$resp"
    TOKEN=$(echo "$resp" | grep -o '"token":"[^"]*"' | cut -d'"' -f4)
}

# 封装 curl，自动带 token
api() {
    local method="$1"
    local url="$2"
    local payload="$3"
    if [ -n "$payload" ]; then
        curl -s -X "$method" "$BASE_URL$url" \
            -H "Authorization: Bearer $TOKEN" \
            -H "Content-Type: application/json" \
            --data-binary "@$payload"
    else
        curl -s -X "$method" "$BASE_URL$url" \
            -H "Authorization: Bearer $TOKEN"
    fi
}

cmd="$1"
sub="$2"
arg="$3"

case "$cmd" in
    login)
        login "$sub"
        echo ">>> Token cached"
        ;;

    class)
        login admin
        case "$sub" in
            create) api POST /api/classes "$PAYLOADS/create-class.json" ;;
            list)   api GET '/api/classes?page=1&size=10' ;;
            list-all) api GET /api/classes/list-all ;;
            update) api PUT "/api/classes/$arg" "$PAYLOADS/update-class.json" ;;
            delete) api DELETE "/api/classes/$arg" ;;
            *)      echo "未知子命令: $sub" ;;
        esac
        ;;

    teacher)
        login admin
        case "$sub" in
            create) api POST /api/teachers "$PAYLOADS/create-teacher.json" ;;
            list)   api GET '/api/teachers?page=1&size=10' ;;
            get)    api GET "/api/teachers/$arg" ;;
            bind)   api POST "/api/teachers/$arg/classes" "$PAYLOADS/bind-classes.json" ;;
            *)      echo "未知子命令: $sub" ;;
        esac
        ;;

    student)
        login "${arg:-admin}"
        case "$sub" in
            list)   api GET '/api/students?page=1&size=10' ;;
            passwd) api PUT "/api/students/$arg/password" "$PAYLOADS/reset-password.json" ;;
            import) echo "学生导入需使用前端上传，此脚本暂不支持" ;;
            *)      echo "未知子命令: $sub" ;;
        esac
        ;;

    *)
        echo "用法: $0 {login|class|teacher|student} [子命令] [参数]"
        ;;
esac

echo ""
