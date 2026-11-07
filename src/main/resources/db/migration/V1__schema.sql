CREATE TABLE students (
 id BIGINT AUTO_INCREMENT PRIMARY KEY, email VARCHAR(190) NOT NULL UNIQUE,
 password VARCHAR(255) NOT NULL, name VARCHAR(80) NOT NULL, major VARCHAR(32), level VARCHAR(32)
);
CREATE TABLE lessons (
 id BIGINT PRIMARY KEY, title VARCHAR(255) NOT NULL, content TEXT NOT NULL,
 level VARCHAR(32) NOT NULL, major VARCHAR(32), minutes INT NOT NULL
);
CREATE TABLE lesson_progress (
 id BIGINT AUTO_INCREMENT PRIMARY KEY, student_id BIGINT NOT NULL, lesson_id BIGINT NOT NULL,
 completed_at DATETIME(6) NOT NULL,
 CONSTRAINT uk_progress UNIQUE(student_id,lesson_id),
 CONSTRAINT fk_progress_student FOREIGN KEY(student_id) REFERENCES students(id),
 CONSTRAINT fk_progress_lesson FOREIGN KEY(lesson_id) REFERENCES lessons(id)
);
CREATE TABLE terms (
 id BIGINT PRIMARY KEY, japanese VARCHAR(255) NOT NULL, reading VARCHAR(255) NOT NULL,
 vietnamese VARCHAR(255) NOT NULL, example VARCHAR(255) NOT NULL, major VARCHAR(32) NOT NULL
);
