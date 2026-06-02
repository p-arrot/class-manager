-- ============================================
-- V9__phase6.sql - Phase 6: 考试 + 项目化学习
-- ============================================

-- 1. exam_papers
CREATE TABLE IF NOT EXISTS exam_papers (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    title        VARCHAR(200) NOT NULL,
    content      TEXT         NOT NULL,
    total_score  INT          NOT NULL DEFAULT 100,
    teacher_id   BIGINT       NOT NULL,
    created_at   TIMESTAMP    NOT NULL DEFAULT now(),
    updated_at   TIMESTAMP    NOT NULL DEFAULT now(),
    deleted      SMALLINT     NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_exam_papers_teacher ON exam_papers(teacher_id);
ALTER TABLE exam_papers ADD CONSTRAINT fk_exam_papers_teacher FOREIGN KEY (teacher_id) REFERENCES users(id);

-- 2. exams
CREATE TABLE IF NOT EXISTS exams (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name         VARCHAR(200)   NOT NULL,
    semester_id  BIGINT         NOT NULL,
    paper_id     BIGINT         NOT NULL,
    start_time   TIMESTAMP      NOT NULL,
    end_time     TIMESTAMP      NOT NULL,
    weight       DECIMAL(3,2)   NOT NULL DEFAULT 1.0,
    created_at   TIMESTAMP      NOT NULL DEFAULT now(),
    updated_at   TIMESTAMP      NOT NULL DEFAULT now(),
    deleted      SMALLINT       NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_exams_semester ON exams(semester_id);
ALTER TABLE exams ADD CONSTRAINT fk_exams_semester FOREIGN KEY (semester_id) REFERENCES semesters(id);
ALTER TABLE exams ADD CONSTRAINT fk_exams_paper    FOREIGN KEY (paper_id)    REFERENCES exam_papers(id);

-- 3. exam_classes
CREATE TABLE IF NOT EXISTS exam_classes (
    id       BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    exam_id  BIGINT NOT NULL,
    class_id BIGINT NOT NULL
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_exam_classes_uniq ON exam_classes(exam_id, class_id);
ALTER TABLE exam_classes ADD CONSTRAINT fk_ec_exam  FOREIGN KEY (exam_id)  REFERENCES exams(id);
ALTER TABLE exam_classes ADD CONSTRAINT fk_ec_class FOREIGN KEY (class_id) REFERENCES school_classes(id);

-- 4. exam_submissions
CREATE TABLE IF NOT EXISTS exam_submissions (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    exam_id      BIGINT       NOT NULL,
    student_id   BIGINT       NOT NULL,
    answers      TEXT,
    score        INT,
    status       VARCHAR(20)  NOT NULL DEFAULT 'submitted',
    submitted_at TIMESTAMP,
    created_at   TIMESTAMP    NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_exam_sub_uniq ON exam_submissions(exam_id, student_id);
ALTER TABLE exam_submissions ADD CONSTRAINT fk_es_exam    FOREIGN KEY (exam_id)    REFERENCES exams(id);
ALTER TABLE exam_submissions ADD CONSTRAINT fk_es_student FOREIGN KEY (student_id) REFERENCES users(id);

-- 5. projects
CREATE TABLE IF NOT EXISTS projects (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name          VARCHAR(200)   NOT NULL,
    description   TEXT,
    semester_id   BIGINT         NOT NULL,
    max_team_size INT            NOT NULL DEFAULT 1,
    deadline      TIMESTAMP,
    weight        DECIMAL(3,2)   NOT NULL DEFAULT 1.0,
    created_at    TIMESTAMP      NOT NULL DEFAULT now(),
    updated_at    TIMESTAMP      NOT NULL DEFAULT now(),
    deleted       SMALLINT       NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_projects_semester ON projects(semester_id);
ALTER TABLE projects ADD CONSTRAINT fk_projects_semester FOREIGN KEY (semester_id) REFERENCES semesters(id);

-- 6. project_teams
CREATE TABLE IF NOT EXISTS project_teams (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    project_id BIGINT       NOT NULL,
    name       VARCHAR(200),
    created_at TIMESTAMP    NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_pt_project ON project_teams(project_id);
ALTER TABLE project_teams ADD CONSTRAINT fk_pt_project FOREIGN KEY (project_id) REFERENCES projects(id);

-- 7. project_team_members
CREATE TABLE IF NOT EXISTS project_team_members (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    team_id    BIGINT    NOT NULL,
    student_id BIGINT    NOT NULL
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_ptm_uniq ON project_team_members(team_id, student_id);
ALTER TABLE project_team_members ADD CONSTRAINT fk_ptm_team    FOREIGN KEY (team_id)    REFERENCES project_teams(id);
ALTER TABLE project_team_members ADD CONSTRAINT fk_ptm_student FOREIGN KEY (student_id) REFERENCES users(id);

-- 8. project_submissions
CREATE TABLE IF NOT EXISTS project_submissions (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    project_id   BIGINT    NOT NULL,
    team_id      BIGINT,
    student_id   BIGINT    NOT NULL,
    content      TEXT,
    submitted_at TIMESTAMP,
    created_at   TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_ps_project ON project_submissions(project_id);
ALTER TABLE project_submissions ADD CONSTRAINT fk_ps_project FOREIGN KEY (project_id) REFERENCES projects(id);
ALTER TABLE project_submissions ADD CONSTRAINT fk_ps_student FOREIGN KEY (student_id) REFERENCES users(id);

-- 9. project_scores
CREATE TABLE IF NOT EXISTS project_scores (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    project_id BIGINT      NOT NULL,
    student_id BIGINT      NOT NULL,
    grade      VARCHAR(1)  NOT NULL CHECK (grade IN ('A','B','C','D','E','F')),
    is_special SMALLINT    NOT NULL DEFAULT 0,
    created_at TIMESTAMP   NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_pscores_project ON project_scores(project_id);
ALTER TABLE project_scores ADD CONSTRAINT fk_pscores_project FOREIGN KEY (project_id) REFERENCES projects(id);
ALTER TABLE project_scores ADD CONSTRAINT fk_pscores_student FOREIGN KEY (student_id) REFERENCES users(id);
