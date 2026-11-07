-- Placement and foundation assessments happen before a major is selected.
ALTER TABLE exam_attempts MODIFY COLUMN major VARCHAR(32) NULL;
