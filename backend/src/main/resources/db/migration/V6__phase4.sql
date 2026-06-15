-- ============================================
-- V6__phase4.sql - Phase 4: 课堂任务 + 学生提交
-- ============================================

-- 1. tasks 表
CREATE TABLE IF NOT EXISTS tasks (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    title        VARCHAR(200) NOT NULL,
    type         VARCHAR(20)  NOT NULL CHECK (type IN ('worksheet','artifact')),
    lesson_id    BIGINT       NOT NULL,
    form_schema  TEXT,
    description  TEXT,
    deadline     TIMESTAMP,
    created_at   TIMESTAMP    NOT NULL DEFAULT now(),
    updated_at   TIMESTAMP    NOT NULL DEFAULT now(),
    deleted      SMALLINT     NOT NULL DEFAULT 0
);

COMMENT ON COLUMN tasks.type        IS '任务类型: worksheet=学习单, artifact=课堂作品';
COMMENT ON COLUMN tasks.form_schema IS '学习单 JSON Schema（仅 worksheet 类型）';
COMMENT ON COLUMN tasks.deadline    IS '截止时间，null=无截止';

CREATE INDEX IF NOT EXISTS idx_tasks_lesson ON tasks(lesson_id);
CREATE INDEX IF NOT EXISTS idx_tasks_type   ON tasks(type);

ALTER TABLE tasks
    ADD CONSTRAINT fk_tasks_lesson FOREIGN KEY (lesson_id) REFERENCES lessons(id);

-- 2. submissions 表
CREATE TABLE IF NOT EXISTS submissions (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    task_id      BIGINT       NOT NULL,
    student_id   BIGINT       NOT NULL,
    status       VARCHAR(20)  NOT NULL DEFAULT 'submitted',
    content      TEXT,
    submitted_at TIMESTAMP,
    created_at   TIMESTAMP    NOT NULL DEFAULT now(),
    updated_at   TIMESTAMP    NOT NULL DEFAULT now()
);

COMMENT ON COLUMN submissions.status       IS 'submitted=已提交, graded=已评分, special=特殊情况';
COMMENT ON COLUMN submissions.content      IS '学习单答案 JSON / 作品文件列表 JSON';
COMMENT ON COLUMN submissions.submitted_at IS '实际提交时间';

CREATE UNIQUE INDEX IF NOT EXISTS idx_submissions_task_student
    ON submissions(task_id, student_id);
CREATE INDEX IF NOT EXISTS idx_submissions_task    ON submissions(task_id);
CREATE INDEX IF NOT EXISTS idx_submissions_student ON submissions(student_id);
CREATE INDEX IF NOT EXISTS idx_submissions_status  ON submissions(status);

ALTER TABLE submissions
    ADD CONSTRAINT fk_submissions_task    FOREIGN KEY (task_id)    REFERENCES tasks(id),
    ADD CONSTRAINT fk_submissions_student FOREIGN KEY (student_id) REFERENCES users(id);
