CREATE TABLE IF NOT EXISTS dimension_scores (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    student_id    BIGINT        NOT NULL,
    source_type   VARCHAR(20)   NOT NULL CHECK (source_type IN ('process','exam','project')),
    source_id     BIGINT        NOT NULL,
    question_id   VARCHAR(80),
    dimension     VARCHAR(30)   NOT NULL CHECK (dimension IN ('AWARENESS','COMPUTING','DIGITAL_LEARNING','RESPONSIBILITY')),
    earned_score  DECIMAL(8,2)  NOT NULL DEFAULT 0,
    max_score     DECIMAL(8,2)  NOT NULL DEFAULT 0,
    auto_graded   BOOLEAN       NOT NULL DEFAULT false,
    created_at    TIMESTAMP     NOT NULL DEFAULT now(),
    updated_at    TIMESTAMP     NOT NULL DEFAULT now(),
    deleted       SMALLINT      NOT NULL DEFAULT 0
);

COMMENT ON TABLE dimension_scores IS '按来源、题目和核心素养维度记录数值得分';
COMMENT ON COLUMN dimension_scores.source_type IS 'process=平时任务, exam=考试, project=项目';
COMMENT ON COLUMN dimension_scores.source_id IS '平时任务/考试/项目对应提交记录ID';
COMMENT ON COLUMN dimension_scores.question_id IS '题目ID或项目评分项ID';

CREATE INDEX IF NOT EXISTS idx_dimension_scores_student ON dimension_scores(student_id);
CREATE INDEX IF NOT EXISTS idx_dimension_scores_source ON dimension_scores(source_type, source_id);
CREATE INDEX IF NOT EXISTS idx_dimension_scores_student_dim ON dimension_scores(student_id, dimension);

ALTER TABLE dimension_scores
    ADD CONSTRAINT fk_dimension_scores_student FOREIGN KEY (student_id) REFERENCES users(id);
