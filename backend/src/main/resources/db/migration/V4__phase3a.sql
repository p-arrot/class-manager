-- ============================================
-- V4__phase3a.sql - Phase 3a: 课程/学期/课时/资源
-- ============================================

-- 1. courses（课程表）
CREATE TABLE courses (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name        VARCHAR(200) NOT NULL,
    description TEXT,
    cover_url   VARCHAR(500),
    teacher_id  BIGINT       NOT NULL,
    created_at  TIMESTAMP    NOT NULL DEFAULT now(),
    updated_at  TIMESTAMP    NOT NULL DEFAULT now(),
    deleted     SMALLINT     NOT NULL DEFAULT 0
);
CREATE INDEX idx_courses_teacher ON courses(teacher_id);
CREATE INDEX idx_courses_name ON courses(name);

-- 2. course_classes（课程-班级多对多关系表）
CREATE TABLE course_classes (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    course_id   BIGINT       NOT NULL,
    class_id    BIGINT       NOT NULL,
    created_at  TIMESTAMP    NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX idx_cc_course_class ON course_classes(course_id, class_id);
CREATE INDEX idx_course_classes_course ON course_classes(course_id);
CREATE INDEX idx_course_classes_class ON course_classes(class_id);

-- 3. semesters（学期表）
CREATE TABLE semesters (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name        VARCHAR(200) NOT NULL,
    start_time  TIMESTAMP    NOT NULL,
    end_time    TIMESTAMP    NOT NULL,
    course_id   BIGINT       NOT NULL,
    created_at  TIMESTAMP    NOT NULL DEFAULT now(),
    updated_at  TIMESTAMP    NOT NULL DEFAULT now(),
    deleted     SMALLINT     NOT NULL DEFAULT 0
);
CREATE INDEX idx_semesters_course ON semesters(course_id);
CREATE INDEX idx_semesters_time ON semesters(start_time, end_time);

-- 4. lessons（课时表）
CREATE TABLE lessons (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name        VARCHAR(200) NOT NULL,
    sort_order  INT          NOT NULL DEFAULT 0,
    semester_id BIGINT       NOT NULL,
    created_at  TIMESTAMP    NOT NULL DEFAULT now(),
    updated_at  TIMESTAMP    NOT NULL DEFAULT now(),
    deleted     SMALLINT     NOT NULL DEFAULT 0
);
CREATE INDEX idx_lessons_semester ON lessons(semester_id);
CREATE INDEX idx_lessons_sort ON lessons(semester_id, sort_order);

-- 5. course_resources（课程资源文件夹表，树形结构）
CREATE TABLE course_resources (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    course_id   BIGINT       NOT NULL,
    parent_id   BIGINT,
    name        VARCHAR(200) NOT NULL,
    type        VARCHAR(20)  NOT NULL DEFAULT 'FOLDER',
    sort_order  INT          NOT NULL DEFAULT 0,
    created_at  TIMESTAMP    NOT NULL DEFAULT now(),
    updated_at  TIMESTAMP    NOT NULL DEFAULT now(),
    deleted     SMALLINT     NOT NULL DEFAULT 0
);
CREATE INDEX idx_cr_course ON course_resources(course_id);
CREATE INDEX idx_cr_parent ON course_resources(parent_id);

-- 外键约束
ALTER TABLE courses ADD CONSTRAINT fk_courses_teacher FOREIGN KEY (teacher_id) REFERENCES users(id);
ALTER TABLE course_classes ADD CONSTRAINT fk_cc_course FOREIGN KEY (course_id) REFERENCES courses(id);
ALTER TABLE course_classes ADD CONSTRAINT fk_cc_class FOREIGN KEY (class_id) REFERENCES school_classes(id);
ALTER TABLE semesters ADD CONSTRAINT fk_semesters_course FOREIGN KEY (course_id) REFERENCES courses(id);
ALTER TABLE lessons ADD CONSTRAINT fk_lessons_semester FOREIGN KEY (semester_id) REFERENCES semesters(id);
ALTER TABLE course_resources ADD CONSTRAINT fk_cr_course FOREIGN KEY (course_id) REFERENCES courses(id);
ALTER TABLE course_resources ADD CONSTRAINT fk_cr_parent FOREIGN KEY (parent_id) REFERENCES course_resources(id);
