package vn.sanka.lms.gamification;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface BadgeRepository extends JpaRepository<EarnedBadge,Long> {
    List<EarnedBadge> findByStudentId(Long studentId);
}
