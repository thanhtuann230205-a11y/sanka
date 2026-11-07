ALTER TABLE exam_attempts ADD assigned_level VARCHAR(32);
UPDATE exam_attempts SET assigned_level = CASE
 WHEN evidence LIKE '%Lộ trình được xếp: N2%' THEN 'N2'
 WHEN evidence LIKE '%Lộ trình được xếp: N3%' THEN 'N3'
 WHEN evidence LIKE '%Lộ trình được xếp: N4%' THEN 'N4'
 WHEN evidence LIKE '%Lộ trình được xếp: N5%' THEN 'N5'
 WHEN evidence LIKE '%Lộ trình được xếp: ZERO%' THEN 'ZERO'
 ELSE NULL END WHERE stage='PLACEMENT';
CREATE TABLE exam_answer_snapshots (
 id BIGINT AUTO_INCREMENT PRIMARY KEY, attempt_id BIGINT NOT NULL,
 question_number INT NOT NULL, question_id VARCHAR(80) NOT NULL,
 prompt TEXT NOT NULL, skill VARCHAR(80) NOT NULL, audio_id VARCHAR(80),
 options_json TEXT NOT NULL, chosen_index INT NOT NULL, correct_index INT NOT NULL,
 explanation TEXT NOT NULL,
 CONSTRAINT uk_exam_answer UNIQUE(attempt_id,question_number),
 CONSTRAINT fk_exam_answer_attempt FOREIGN KEY(attempt_id) REFERENCES exam_attempts(id)
);
