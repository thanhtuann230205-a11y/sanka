package vn.sanka.lms.language;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface LanguageReviewRepository extends JpaRepository<LanguageReview,Long> {
 List<LanguageReview> findByStudentIdOrderByCreatedAtDesc(Long studentId);
}
