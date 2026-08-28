ALTER TABLE submissions
    ADD COLUMN IF NOT EXISTS return_reason VARCHAR(500),
    ADD COLUMN IF NOT EXISTS returned_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS revision_count INT NOT NULL DEFAULT 0;

ALTER TABLE exam_submissions
    ADD COLUMN IF NOT EXISTS return_reason VARCHAR(500),
    ADD COLUMN IF NOT EXISTS returned_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS started_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP NOT NULL DEFAULT now(),
    ADD COLUMN IF NOT EXISTS revision_count INT NOT NULL DEFAULT 0;

ALTER TABLE project_submissions
    ADD COLUMN IF NOT EXISTS status VARCHAR(20) NOT NULL DEFAULT 'submitted',
    ADD COLUMN IF NOT EXISTS return_reason VARCHAR(500),
    ADD COLUMN IF NOT EXISTS returned_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP NOT NULL DEFAULT now(),
    ADD COLUMN IF NOT EXISTS revision_count INT NOT NULL DEFAULT 0;

UPDATE project_submissions submission
SET status = CASE
    WHEN EXISTS (
        SELECT 1 FROM dimension_scores score
        WHERE score.source_type = 'project'
          AND score.source_id = submission.id
          AND score.deleted = 0
    ) THEN 'graded'
    ELSE 'submitted'
END;

ALTER TABLE project_submissions DROP COLUMN IF EXISTS team_id;
DROP TABLE IF EXISTS project_team_members;
DROP TABLE IF EXISTS project_teams;
ALTER TABLE projects DROP COLUMN IF EXISTS max_team_size;
DROP TABLE IF EXISTS project_scores;

CREATE INDEX IF NOT EXISTS idx_exam_submissions_status ON exam_submissions(status);
CREATE INDEX IF NOT EXISTS idx_project_submissions_status ON project_submissions(status);
