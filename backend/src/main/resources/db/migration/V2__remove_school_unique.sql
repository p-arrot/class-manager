-- ============================================
-- V2__remove_school_unique.sql
-- 去除业务逻辑中的学校依赖：
--   1. 学号全局唯一（不再依赖 school_id）
--   2. 用户名全局唯一（教师和管理员用户名不可重复）
--   3. school_id 列保留不动，仅供未来扩展
-- ============================================

-- 删除旧的 (school_id, student_no) 联合唯一索引
DROP INDEX IF EXISTS idx_users_school_student_no;

-- 学号全局唯一
CREATE UNIQUE INDEX idx_users_student_no ON users(student_no)
    WHERE student_no IS NOT NULL AND deleted = 0;

-- 用户名全局唯一（先删除 V1 创建的普通索引，再创建唯一索引）
DROP INDEX IF EXISTS idx_users_username;
CREATE UNIQUE INDEX idx_users_username ON users(username)
    WHERE username IS NOT NULL AND deleted = 0;
