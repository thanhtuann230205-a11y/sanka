package vn.sanka.lms.assessment;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.sanka.lms.learning.Major;
import java.util.Optional;

public interface DraftRepository extends JpaRepository<PracticeDraft,Long> {
    Optional<PracticeDraft> findByStudentIdAndMajorAndKind(Long studentId,Major major,PracticeKind kind);
    void deleteByStudentIdAndMajorAndKind(Long studentId,Major major,PracticeKind kind);
}
