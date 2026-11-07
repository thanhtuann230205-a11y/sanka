CREATE TABLE language_enrollments (
 id BIGINT AUTO_INCREMENT PRIMARY KEY, student_id BIGINT NOT NULL, level VARCHAR(32) NOT NULL,
 enrolled_at DATE NOT NULL, next_review_at DATE NOT NULL,
 CONSTRAINT uk_language_enrollment UNIQUE(student_id,level),
 CONSTRAINT fk_language_enrollment_student FOREIGN KEY(student_id) REFERENCES students(id)
);
CREATE TABLE language_reviews (
 id BIGINT AUTO_INCREMENT PRIMARY KEY, student_id BIGINT NOT NULL, level VARCHAR(32) NOT NULL,
 score INT NOT NULL, evidence TEXT NOT NULL, created_at DATETIME(6) NOT NULL,
 CONSTRAINT fk_language_review_student FOREIGN KEY(student_id) REFERENCES students(id)
);
CREATE TABLE language_submissions (
 id BIGINT AUTO_INCREMENT PRIMARY KEY, student_id BIGINT NOT NULL, level VARCHAR(32) NOT NULL,
 major VARCHAR(32) NOT NULL, response TEXT NOT NULL, status VARCHAR(20) NOT NULL,
 language_score INT, technical_score INT, communication_score INT, task_score INT,
 feedback TEXT, reviewer_id BIGINT, created_at DATETIME(6) NOT NULL, reviewed_at DATETIME(6),
 CONSTRAINT fk_language_submission_student FOREIGN KEY(student_id) REFERENCES students(id),
 CONSTRAINT fk_language_submission_reviewer FOREIGN KEY(reviewer_id) REFERENCES students(id)
);
CREATE INDEX idx_language_submission ON language_submissions(student_id,level,major);
INSERT INTO lessons(id,title,content,level,major,minutes) VALUES
(30,'N3 · Báo cáo thay đổi','変更点を確認次第、担当者に報告してください。
Hãy báo cho người phụ trách ngay khi xác nhận xong thay đổi.
次第 diễn tả thực hiện ngay sau khi một việc hoàn tất.
練習：viết yêu cầu xác nhận thông số và báo lại cho trưởng nhóm.','N3',NULL,25),
(31,'N3 · Đọc thông báo và thời hạn','検証結果は、午後3時までに送ってください。
Hãy gửi kết quả kiểm chứng trước 3 giờ chiều.
までに diễn tả hạn chót; まで diễn tả sự tiếp diễn đến một mốc.
練習：viết thông báo hạn nộp báo cáo và những dữ liệu cần kèm theo.','N3',NULL,25),
(32,'N3 · Điều kiện vận hành và giao tiếp','確認が済むまで、変更を反映しないでください。
Không áp dụng thay đổi cho đến khi xác nhận xong.
原因はまだ特定されていません。Chưa xác định được nguyên nhân.
練習：tách dữ kiện đã xác nhận và phần cần điều tra trong một bản ghi.','N3',NULL,30),
(40,'N2–N1 · Đánh giá hai mặt của đề xuất','効果が認められる一方、運用負荷の増大も懸念される。
Mặc dù có hiệu quả, cũng có lo ngại gánh nặng vận hành tăng.
一方 đối chiếu hai mặt; 懸念される nêu lo ngại cần cân nhắc.
練習：viết ưu điểm, hạn chế và điều kiện áp dụng một đề xuất.','N2',NULL,35),
(41,'N2–N1 · Quyết định dựa trên kiểm chứng','品質を維持するうえで、十分な検証が不可欠です。
Để duy trì chất lượng, kiểm chứng đầy đủ là không thể thiếu.
うえで dùng nêu điều kiện hoặc yếu tố quan trọng trong một hoạt động.
練習：giải thích vì sao một thay đổi cần được kiểm chứng trước khi áp dụng.','N2',NULL,35),
(42,'N2–N1 · Đọc và tổng hợp báo cáo kỹ thuật','一部の検証が完了していないため、全体への適用は見合わせます。
Do một phần kiểm chứng chưa hoàn tất, tạm hoãn áp dụng toàn bộ.
影響範囲を確認したうえで、改めて判断します。
Sau khi xác nhận phạm vi ảnh hưởng sẽ đánh giá lại.
練習：viết một bản tóm tắt có căn cứ, giới hạn dữ liệu và đề nghị bước tiếp theo.','N2',NULL,40);
