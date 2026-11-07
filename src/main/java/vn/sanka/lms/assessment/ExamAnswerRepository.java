package vn.sanka.lms.assessment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface ExamAnswerRepository extends JpaRepository<ExamAnswerSnapshot,Long> {
    List<ExamAnswerSnapshot> findByAttemptIdOrderByQuestionNumberAsc(Long attemptId);
}
