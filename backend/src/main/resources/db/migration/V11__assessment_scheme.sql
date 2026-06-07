-- Course semester assessment scheme
CREATE TABLE IF NOT EXISTS assessment_schemes (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    semester_id     BIGINT NOT NULL,
    process_percent INT    NOT NULL DEFAULT 50,
    exam_percent    INT    NOT NULL DEFAULT 50,
    project_percent INT    NOT NULL DEFAULT 0,
    created_at      TIMESTAMP NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP NOT NULL DEFAULT now(),
    deleted         SMALLINT  NOT NULL DEFAULT 0,
    CONSTRAINT uq_assessment_scheme_semester UNIQUE (semester_id),
    CONSTRAINT ck_assessment_scheme_range CHECK (
        process_percent >= 0 AND process_percent <= 100 AND
        exam_percent >= 0 AND exam_percent <= 100 AND
        project_percent >= 0 AND project_percent <= 100 AND
        process_percent + exam_percent + project_percent = 100
    )
);

ALTER TABLE assessment_schemes
    ADD CONSTRAINT fk_assessment_scheme_semester FOREIGN KEY (semester_id) REFERENCES semesters(id);
