package vn.sanka.lms.learning;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import vn.sanka.lms.auth.Level;
import vn.sanka.lms.auth.Major;
public interface LessonRepository extends JpaRepository<Lesson,Long>{
 List<Lesson> findByMajor(Major major);
 List<Lesson> findByMajorIsNullAndLevelIn(List<Level> levels);
}
