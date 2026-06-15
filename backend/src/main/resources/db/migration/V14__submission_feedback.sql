CREATE TABLE IF NOT EXISTS submission_feedback (
    submission_id     BIGINT PRIMARY KEY,
    teacher_id        BIGINT,
    teacher_comment   TEXT,
    question_feedback TEXT,
    graded_at         TIMESTAMP,
    created_at        TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE submission_feedback IS '任务提交批改反馈';
COMMENT ON COLUMN submission_feedback.teacher_comment IS '整份任务总评语';
COMMENT ON COLUMN submission_feedback.question_feedback IS '逐题反馈 JSON 数组';
COMMENT ON COLUMN submission_feedback.graded_at IS '批改完成时间';

CREATE INDEX IF NOT EXISTS idx_submission_feedback_teacher ON submission_feedback(teacher_id);
CREATE INDEX IF NOT EXISTS idx_submission_feedback_graded_at ON submission_feedback(graded_at);

ALTER TABLE submission_feedback
    ADD CONSTRAINT fk_submission_feedback_submission FOREIGN KEY (submission_id) REFERENCES submissions(id);

ALTER TABLE submission_feedback
    ADD CONSTRAINT fk_submission_feedback_teacher FOREIGN KEY (teacher_id) REFERENCES users(id);
