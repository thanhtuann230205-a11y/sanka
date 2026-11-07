package vn.sanka.lms.learning;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
public interface ProgressRepository extends JpaRepository<LessonProgress,Long>{
 List<LessonProgress> findByStudentId(Long studentId);
 boolean existsByStudentIdAndLessonId(Long studentId,Long lessonId);
}