-- ============================================
-- V5__phase3b.sql - Phase 3b: MinIO 文件基础设施
-- ============================================

-- 1. course_resources 扩展文件支持字段
ALTER TABLE course_resources
    ADD COLUMN IF NOT EXISTS file_size    BIGINT,
    ADD COLUMN IF NOT EXISTS content_type VARCHAR(100),
    ADD COLUMN IF NOT EXISTS object_name  VARCHAR(500);

COMMENT ON COLUMN course_resources.file_size    IS '文件大小（字节），仅 FILE 类型';
COMMENT ON COLUMN course_resources.content_type IS '文件 MIME 类型，仅 FILE 类型';
COMMENT ON COLUMN course_resources.object_name  IS 'MinIO 对象名（路径），仅 FILE 类型';

-- 2. lesson_resources（课时资源，Phase 3b 建表）
CREATE TABLE IF NOT EXISTS lesson_resources (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    lesson_id    BIGINT       NOT NULL,
    name         VARCHAR(200) NOT NULL,
    file_size    BIGINT,
    content_type VARCHAR(100),
    object_name  VARCHAR(500),
    created_at   TIMESTAMP    NOT NULL DEFAULT now(),
    updated_at   TIMESTAMP    NOT NULL DEFAULT now(),
    deleted      SMALLINT     NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_lr_lesson ON lesson_resources(lesson_id);

ALTER TABLE lesson_resources
    ADD CONSTRAINT fk_lr_lesson FOREIGN KEY (lesson_id) REFERENCES lessons(id);
