package vn.sanka.lms.language;
import jakarta.persistence.*;
import vn.sanka.lms.learning.Level;
import java.time.LocalDate;
@Entity @Table(name="language_enrollments",uniqueConstraints=@UniqueConstraint(columnNames={"student_id","level"}))
public class LanguageEnrollment {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(nullable=false) private Long studentId;
 @Enumerated(EnumType.STRING) @Column(nullable=false) private Level level;
 @Column(nullable=false) private LocalDate enrolledAt;
 @Column(nullable=false) private LocalDate nextReviewAt;
 protected LanguageEnrollment(){}
 public LanguageEnrollment(Long studentId,Level level,LocalDate today){this.studentId=studentId;this.level=level;this.enrolledAt=today;this.nextReviewAt=today.plusDays(7);}
 public Level getLevel(){return level;} public LocalDate getEnrolledAt(){return enrolledAt;} public LocalDate getNextReviewAt(){return nextReviewAt;}
 public void reviewed(LocalDate today){this.nextReviewAt=today.plusDays(7);}
}
