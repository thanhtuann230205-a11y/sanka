package vn.sanka.lms.learning;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface BookmarkRepository extends JpaRepository<TermBookmark,Long> {
    List<TermBookmark> findByStudentId(Long studentId);
    Optional<TermBookmark> findByStudentIdAndTermId(Long studentId,Long termId);
}
