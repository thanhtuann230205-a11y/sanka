package vn.sanka.lms.assessment;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.sanka.lms.learning.Major;
import java.util.List;

public interface AttemptRepository extends JpaRepository<ExamAttempt,Long> {
    boolean existsByStudentIdAndStageAndPassedTrue(Long studentId, Stage stage);
    boolean existsByStudentIdAndMajorAndStageAndPassedTrue(Long studentId, Major major, Stage stage);
    List<ExamAttempt> findByStudentIdOrderByCreatedAtDesc(Long studentId);
}
