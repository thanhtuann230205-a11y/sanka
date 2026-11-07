package vn.sanka.lms.gamification;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface MotivationProfileRepository extends JpaRepository<MotivationProfile,Long> {
    Optional<MotivationProfile> findByStudentId(Long studentId);
}
