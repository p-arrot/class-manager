-- ============================================
-- V8__phase5.sql - Phase 5: 四维度评价
-- ============================================

CREATE TABLE IF NOT EXISTS evaluations (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    student_id   BIGINT       NOT NULL,
    source_type  VARCHAR(20)  NOT NULL CHECK (source_type IN ('worksheet','artifact','project')),
    source_id    BIGINT       NOT NULL,
    dimension    VARCHAR(30)  NOT NULL CHECK (dimension IN ('AWARENESS','COMPUTING','DIGITAL_LEARNING','RESPONSIBILITY')),
    grade        VARCHAR(1)   NOT NULL CHECK (grade IN ('A','B','C','D','E','F')),
    is_special   SMALLINT     NOT NULL DEFAULT 0,
    created_at   TIMESTAMP    NOT NULL DEFAULT now()
);

COMMENT ON COLUMN evaluations.source_type IS '评分来源: worksheet/artifact/project';
COMMENT ON COLUMN evaluations.dimension   IS '四维度: AWARENESS/COMPUTING/DIGITAL_LEARNING/RESPONSIBILITY';
COMMENT ON COLUMN evaluations.grade        IS '等级: A=100/B=80/C=60/D=40/E=20/F=0';
COMMENT ON COLUMN evaluations.is_special   IS '特殊情况标记(不计入统计)';

CREATE INDEX IF NOT EXISTS idx_eval_student ON evaluations(student_id);
CREATE INDEX IF NOT EXISTS idx_eval_source  ON evaluations(source_type, source_id);
CREATE INDEX IF NOT EXISTS idx_eval_dim     ON evaluations(student_id, dimension);

ALTER TABLE evaluations
    ADD CONSTRAINT fk_eval_student FOREIGN KEY (student_id) REFERENCES users(id);
