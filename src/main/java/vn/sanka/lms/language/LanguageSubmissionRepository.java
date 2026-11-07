package vn.sanka.lms.language;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.sanka.lms.learning.*;
import java.util.*;
public interface LanguageSubmissionRepository extends JpaRepository<LanguageSubmission,Long> {
 List<LanguageSubmission> findByStudentIdOrderByCreatedAtDesc(Long studentId);
 List<LanguageSubmission> findByStatusOrderByCreatedAtAsc(String status);
 boolean existsByStudentIdAndLevelAndMajorAndStatus(Long studentId,Level level,Major major,String status);
}
