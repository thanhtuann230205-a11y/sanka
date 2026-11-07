package vn.sanka.lms.language;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.sanka.lms.learning.Level;
import java.util.*;
public interface LanguageEnrollmentRepository extends JpaRepository<LanguageEnrollment,Long> {
 Optional<LanguageEnrollment> findByStudentIdAndLevel(Long studentId,Level level);
 List<LanguageEnrollment> findByStudentId(Long studentId);
}
