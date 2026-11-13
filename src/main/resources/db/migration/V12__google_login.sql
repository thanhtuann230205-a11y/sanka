ALTER TABLE students ADD COLUMN google_subject VARCHAR(255) NULL;
CREATE UNIQUE INDEX idx_students_google_subject ON students(google_subject);
