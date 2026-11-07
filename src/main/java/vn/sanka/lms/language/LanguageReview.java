package vn.sanka.lms.language;
import jakarta.persistence.*;
import vn.sanka.lms.learning.Level;
import java.time.LocalDateTime;
@Entity @Table(name="language_reviews")
public class LanguageReview {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(nullable=false) private Long studentId;
 @Enumerated(EnumType.STRING) @Column(nullable=false) private Level level;
 @Column(nullable=false) private int score;
 @Column(nullable=false,columnDefinition="TEXT") private String evidence;
 @Column(nullable=false) private LocalDateTime createdAt;
 protected LanguageReview(){}
 public LanguageReview(Long studentId,Level level,int score,String evidence,LocalDateTime now){this.studentId=studentId;this.level=level;this.score=score;this.evidence=evidence;this.createdAt=now;}
 public Long getId(){return id;} public Long getStudentId(){return studentId;} public Level getLevel(){return level;}
 public int getScore(){return score;} public String getEvidence(){return evidence;} public LocalDateTime getCreatedAt(){return createdAt;}
}
