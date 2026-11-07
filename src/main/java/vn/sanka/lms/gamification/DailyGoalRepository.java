package vn.sanka.lms.gamification;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.*;
public interface DailyGoalRepository extends JpaRepository<DailyGoal,Long> {
    Optional<DailyGoal> findByStudentIdAndActivityDate(Long studentId,LocalDate date);
    List<DailyGoal> findByStudentId(Long studentId);
}
