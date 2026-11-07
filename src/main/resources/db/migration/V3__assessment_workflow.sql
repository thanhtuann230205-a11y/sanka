ALTER TABLE students ADD placement_completed BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE students ADD role VARCHAR(20) NOT NULL DEFAULT 'STUDENT';
-- Previous levels were self-declared, so do not treat them as assessment evidence.
UPDATE students SET level = 'ZERO';
CREATE TABLE exam_attempts (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,
 student_id BIGINT NOT NULL,
 major VARCHAR(32) NOT NULL,
 stage VARCHAR(32) NOT NULL,
 score INT NOT NULL,
 passed BOOLEAN NOT NULL,
 evidence TEXT NOT NULL,
 created_at DATETIME(6) NOT NULL,
 CONSTRAINT fk_attempt_student FOREIGN KEY (student_id) REFERENCES students(id)
);
CREATE INDEX idx_attempt_student ON exam_attempts(student_id, major, stage);
CREATE TABLE practice_submissions (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,
 student_id BIGINT NOT NULL,
 major VARCHAR(32) NOT NULL,
 kind VARCHAR(32) NOT NULL,
 response TEXT NOT NULL,
 status VARCHAR(20) NOT NULL,
 language_score INT,
 technical_score INT,
 communication_score INT,
 task_score INT,
 feedback TEXT,
 reviewer_id BIGINT,
 created_at DATETIME(6) NOT NULL,
 reviewed_at DATETIME(6),
 CONSTRAINT fk_submission_student FOREIGN KEY (student_id) REFERENCES students(id),
 CONSTRAINT fk_submission_reviewer FOREIGN KEY (reviewer_id) REFERENCES students(id)
);
CREATE INDEX idx_submission_student ON practice_submissions(student_id, major, kind);
