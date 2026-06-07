-- Keep this migration idempotent. V11/V12 may already have created these
-- constraints; this guards unusual restored or manually repaired databases
-- without changing the checksum of previously applied migrations.
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_assessment_scheme_semester'
    ) THEN
        ALTER TABLE assessment_schemes
            ADD CONSTRAINT fk_assessment_scheme_semester
            FOREIGN KEY (semester_id) REFERENCES semesters(id);
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_dimension_scores_student'
    ) THEN
        ALTER TABLE dimension_scores
            ADD CONSTRAINT fk_dimension_scores_student
            FOREIGN KEY (student_id) REFERENCES users(id);
    END IF;
END $$;
