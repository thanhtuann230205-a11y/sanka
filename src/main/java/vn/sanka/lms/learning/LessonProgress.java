package vn.sanka.lms.learning;
import jakarta.persistence.*;
import java.time.LocalDateTime;
@Entity @Table(name="lesson_progress",uniqueConstraints=@UniqueConstraint(columnNames={"student_id","lesson_id"}))
public class LessonProgress {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(name="student_id",nullable=false) private Long studentId;
 @Column(name="lesson_id",nullable=false) private Long lessonId;
 @Column(nullable=false) private LocalDateTime completedAt;
 protected LessonProgress(){}
 public LessonProgress(Long studentId,Long lessonId){this(studentId,lessonId,LocalDateTime.now());}
 public LessonProgress(Long studentId,Long lessonId,LocalDateTime completedAt){this.studentId=studentId;this.lessonId=lessonId;this.completedAt=completedAt;}
 public Long getLessonId(){return lessonId;}
}
