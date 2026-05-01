-- ============================================
-- V3__phase2.sql - Phase 2: 班级/教师/学生管理
--   1. school_classes.school_id 改为可空（当前不依赖学校）
--   2. 添加性能索引
-- ============================================

-- school_id 改为可空（FK 约束仍然有效，PostgreSQL 允许 NULL FK）
ALTER TABLE school_classes ALTER COLUMN school_id DROP NOT NULL;

-- 班级名称索引（学生导入时按名称查找班级）
CREATE INDEX idx_school_classes_name ON school_classes(name);

-- 年级索引（班级列表按年级筛选）
CREATE INDEX idx_school_classes_grade ON school_classes(grade);

-- 学生姓名索引（学生列表关键词搜索）
CREATE INDEX idx_users_name ON users(name);
