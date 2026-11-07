package vn.sanka.lms.assessment;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.sanka.lms.learning.Major;
import java.util.List;

public interface SubmissionRepository extends JpaRepository<PracticeSubmission,Long> {
    boolean existsByStudentIdAndMajorAndKindAndStatus(Long studentId, Major major, PracticeKind kind, String status);
    List<PracticeSubmission> findByStudentIdOrderByCreatedAtDesc(Long studentId);
    List<PracticeSubmission> findByStatusOrderByCreatedAtAsc(String status);
}
