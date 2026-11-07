CREATE TABLE xp_events (
 id BIGINT AUTO_INCREMENT PRIMARY KEY, student_id BIGINT NOT NULL,
 event_key VARCHAR(190) NOT NULL, kind VARCHAR(40) NOT NULL,
 points INT NOT NULL, activity_date DATE NOT NULL, occurred_at DATETIME(6) NOT NULL,
 CONSTRAINT uk_xp_event UNIQUE(student_id,event_key),
 CONSTRAINT fk_xp_student FOREIGN KEY(student_id) REFERENCES students(id)
);
CREATE INDEX idx_xp_calendar ON xp_events(student_id,activity_date);
CREATE TABLE motivation_profiles (
 id BIGINT AUTO_INCREMENT PRIMARY KEY, student_id BIGINT NOT NULL UNIQUE,
 target_xp INT NOT NULL DEFAULT 40, leaderboard_enabled BOOLEAN NOT NULL DEFAULT FALSE,
 nickname VARCHAR(30) NOT NULL,
 CONSTRAINT fk_motivation_student FOREIGN KEY(student_id) REFERENCES students(id)
);
CREATE TABLE daily_goals (
 id BIGINT AUTO_INCREMENT PRIMARY KEY, student_id BIGINT NOT NULL,
 activity_date DATE NOT NULL, target_xp INT NOT NULL,
 CONSTRAINT uk_daily_goal UNIQUE(student_id,activity_date),
 CONSTRAINT fk_goal_student FOREIGN KEY(student_id) REFERENCES students(id)
);
CREATE TABLE earned_badges (
 id BIGINT AUTO_INCREMENT PRIMARY KEY, student_id BIGINT NOT NULL, code VARCHAR(40) NOT NULL,
 earned_at DATETIME(6) NOT NULL,
 CONSTRAINT uk_earned_badge UNIQUE(student_id,code),
 CONSTRAINT fk_badge_student FOREIGN KEY(student_id) REFERENCES students(id)
);
-- Preserve the previously displayed 20 XP for each completed lesson, on its original date.
INSERT INTO xp_events(student_id,event_key,kind,points,activity_date,occurred_at)
SELECT student_id,CONCAT('lesson:',lesson_id),'LESSON',20,CAST(completed_at AS DATE),completed_at FROM lesson_progress;
INSERT INTO daily_goals(student_id,activity_date,target_xp)
SELECT student_id,activity_date,40 FROM xp_events GROUP BY student_id,activity_date;
