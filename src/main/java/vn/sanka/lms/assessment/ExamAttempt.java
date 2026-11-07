package vn.sanka.lms.assessment;

import jakarta.persistence.*;
import vn.sanka.lms.learning.Major;
import vn.sanka.lms.learning.Level;
import java.time.LocalDateTime;

@Entity @Table(name="exam_attempts")
public class ExamAttempt {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(nullable=false) private Long studentId;
    @Enumerated(EnumType.STRING) private Major major;
    @Enumerated(EnumType.STRING) @Column(nullable=false) private Stage stage;
    @Column(nullable=false) private int score;
    @Column(nullable=false) private boolean passed;
    @Column(nullable=false,columnDefinition="TEXT") private String evidence;
    @Column(nullable=false) private LocalDateTime createdAt;
    @Enumerated(EnumType.STRING) private Level assignedLevel;
    protected ExamAttempt() {}
    public ExamAttempt(Long studentId, Major major, Stage stage, int score, boolean passed, String evidence) {
        this.studentId=studentId; this.major=major; this.stage=stage; this.score=score;
        this.passed=passed; this.evidence=evidence; this.createdAt=LocalDateTime.now();
    }
    public Long getId(){return id;}
    public Long getStudentId(){return studentId;}
    public Major getMajor(){return major;}
    public Stage getStage(){return stage;}
    public int getScore(){return score;}
    public boolean isPassed(){return passed;}
    public String getEvidence(){return evidence;}
    public LocalDateTime getCreatedAt(){return createdAt;}
    public Level getAssignedLevel(){return assignedLevel;}
    public void assignLevel(Level level){this.assignedLevel=level;}
}
