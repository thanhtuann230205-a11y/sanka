package vn.sanka.lms.assessment;

import jakarta.persistence.*;
import vn.sanka.lms.learning.Major;
import java.time.LocalDateTime;

@Entity @Table(name="practice_submissions")
public class PracticeSubmission {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(nullable=false) private Long studentId;
    @Enumerated(EnumType.STRING) @Column(nullable=false) private Major major;
    @Enumerated(EnumType.STRING) @Column(nullable=false) private PracticeKind kind;
    @Column(nullable=false,columnDefinition="TEXT") private String response;
    @Column(nullable=false,length=20) private String status="PENDING";
    private Integer languageScore;
    private Integer technicalScore;
    private Integer communicationScore;
    private Integer taskScore;
    @Column(columnDefinition="TEXT") private String feedback;
    private Long reviewerId;
    @Column(nullable=false) private LocalDateTime createdAt;
    private LocalDateTime reviewedAt;
    protected PracticeSubmission() {}
    public PracticeSubmission(Long studentId, Major major, PracticeKind kind, String response) {
        this(studentId,major,kind,response,LocalDateTime.now());
    }
    public PracticeSubmission(Long studentId, Major major, PracticeKind kind, String response,LocalDateTime now) {
        this.studentId=studentId; this.major=major; this.kind=kind;
        this.response=response; this.createdAt=now;
    }
    public void review(Long reviewer, int language, int technical, int communication, int task, String feedback) {
        this.reviewerId=reviewer; this.languageScore=language; this.technicalScore=technical;
        this.communicationScore=communication; this.taskScore=task; this.feedback=feedback;
        this.status=language>=3 && technical>=3 && communication>=3 && task>=3
            && language+technical+communication+task>=14 ? "PASSED" : "REVISION";
        this.reviewedAt=LocalDateTime.now();
    }
    public Long getId(){return id;}
    public Long getStudentId(){return studentId;}
    public Major getMajor(){return major;}
    public PracticeKind getKind(){return kind;}
    public String getResponse(){return response;}
    public String getStatus(){return status;}
    public Integer getLanguageScore(){return languageScore;}
    public Integer getTechnicalScore(){return technicalScore;}
    public Integer getCommunicationScore(){return communicationScore;}
    public Integer getTaskScore(){return taskScore;}
    public String getFeedback(){return feedback;}
    public Long getReviewerId(){return reviewerId;}
    public LocalDateTime getCreatedAt(){return createdAt;}
    public LocalDateTime getReviewedAt(){return reviewedAt;}
}
