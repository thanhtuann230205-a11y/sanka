CREATE TABLE practice_drafts (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,
 student_id BIGINT NOT NULL,
 major VARCHAR(32) NOT NULL,
 kind VARCHAR(32) NOT NULL,
 content TEXT NOT NULL,
 updated_at DATETIME(6) NOT NULL,
 CONSTRAINT uk_draft UNIQUE(student_id, major, kind),
 CONSTRAINT fk_draft_student FOREIGN KEY(student_id) REFERENCES students(id)
);
CREATE TABLE term_bookmarks (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,
 student_id BIGINT NOT NULL,
 term_id BIGINT NOT NULL,
 CONSTRAINT uk_term_bookmark UNIQUE(student_id, term_id),
 CONSTRAINT fk_bookmark_student FOREIGN KEY(student_id) REFERENCES students(id),
 CONSTRAINT fk_bookmark_term FOREIGN KEY(term_id) REFERENCES terms(id)
);
