-- ============================================
-- V10__phase7.sql - Phase 7: 学生网盘 + 总评导出
-- ============================================

CREATE TABLE IF NOT EXISTS user_drive (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id      BIGINT       NOT NULL,
    parent_id    BIGINT,
    name         VARCHAR(200) NOT NULL,
    type         VARCHAR(20)  NOT NULL CHECK (type IN ('FOLDER','FILE')),
    file_size    BIGINT,
    content_type VARCHAR(100),
    object_name  VARCHAR(500),
    created_at   TIMESTAMP    NOT NULL DEFAULT now(),
    updated_at   TIMESTAMP    NOT NULL DEFAULT now(),
    deleted      SMALLINT     NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_drive_user_parent ON user_drive(user_id, parent_id);
ALTER TABLE user_drive ADD CONSTRAINT fk_drive_user FOREIGN KEY (user_id) REFERENCES users(id);
