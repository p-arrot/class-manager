-- ============================================
-- V1__init.sql - 初始建表脚本
-- ============================================

-- 学校表
CREATE TABLE schools (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    address     VARCHAR(255),
    created_at  TIMESTAMP    NOT NULL DEFAULT now(),
    updated_at  TIMESTAMP    NOT NULL DEFAULT now(),
    deleted     SMALLINT     NOT NULL DEFAULT 0
);
CREATE INDEX idx_schools_name ON schools(name);

-- 班级表 (school_classes)
CREATE TABLE school_classes (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    school_id   BIGINT       NOT NULL,
    grade       VARCHAR(50),
    name        VARCHAR(100) NOT NULL,
    created_at  TIMESTAMP    NOT NULL DEFAULT now(),
    updated_at  TIMESTAMP    NOT NULL DEFAULT now(),
    deleted     SMALLINT     NOT NULL DEFAULT 0
);
CREATE INDEX idx_school_classes_school ON school_classes(school_id);

-- 用户表
CREATE TABLE users (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    username    VARCHAR(50),
    student_no  VARCHAR(50),
    name        VARCHAR(100) NOT NULL,
    password    VARCHAR(255) NOT NULL,
    role        VARCHAR(20)  NOT NULL CHECK (role IN ('admin', 'teacher', 'student')),
    school_id   BIGINT,
    class_id    BIGINT,
    phone       VARCHAR(20),
    email       VARCHAR(100),
    enabled     BOOLEAN      NOT NULL DEFAULT true,
    created_at  TIMESTAMP    NOT NULL DEFAULT now(),
    updated_at  TIMESTAMP    NOT NULL DEFAULT now(),
    deleted     SMALLINT     NOT NULL DEFAULT 0
);
CREATE UNIQUE INDEX idx_users_school_student_no ON users(school_id, student_no)
    WHERE student_no IS NOT NULL AND school_id IS NOT NULL;
CREATE INDEX idx_users_class_role ON users(class_id, role);
CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_users_role ON users(role);

-- 教师-班级关系表
CREATE TABLE teacher_classes (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    teacher_id  BIGINT NOT NULL,
    class_id    BIGINT NOT NULL,
    created_at  TIMESTAMP NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX idx_tc_teacher_class ON teacher_classes(teacher_id, class_id);
CREATE INDEX idx_teacher_classes_teacher ON teacher_classes(teacher_id);
CREATE INDEX idx_teacher_classes_class ON teacher_classes(class_id);

-- 审计日志表
CREATE TABLE audit_logs (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id     BIGINT,
    action      VARCHAR(100) NOT NULL,
    target_type VARCHAR(50),
    target_id   BIGINT,
    detail      TEXT,
    ip          VARCHAR(50),
    user_agent  VARCHAR(500),
    created_at  TIMESTAMP    NOT NULL DEFAULT now()
);
CREATE INDEX idx_audit_logs_user ON audit_logs(user_id);
CREATE INDEX idx_audit_logs_action ON audit_logs(action);
CREATE INDEX idx_audit_logs_time ON audit_logs(created_at);

-- 外键约束
ALTER TABLE school_classes ADD CONSTRAINT fk_classes_school FOREIGN KEY (school_id) REFERENCES schools(id);
ALTER TABLE users ADD CONSTRAINT fk_users_school FOREIGN KEY (school_id) REFERENCES schools(id);
ALTER TABLE users ADD CONSTRAINT fk_users_class FOREIGN KEY (class_id) REFERENCES school_classes(id);
ALTER TABLE teacher_classes ADD CONSTRAINT fk_tc_teacher FOREIGN KEY (teacher_id) REFERENCES users(id);
ALTER TABLE teacher_classes ADD CONSTRAINT fk_tc_class FOREIGN KEY (class_id) REFERENCES school_classes(id);
ALTER TABLE audit_logs ADD CONSTRAINT fk_audit_logs_user FOREIGN KEY (user_id) REFERENCES users(id);

-- 确保只有一个管理员账号（逻辑删除场景下的唯一约束）
CREATE UNIQUE INDEX idx_users_single_admin ON users(role) WHERE role = 'admin' AND deleted = 0;
