package vn.sanka.lms.language;
import jakarta.persistence.*;
import vn.sanka.lms.learning.*;
import java.time.LocalDateTime;
@Entity @Table(name="language_submissions")
public class LanguageSubmission {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(nullable=false) private Long studentId;
 @Enumerated(EnumType.STRING) @Column(nullable=false) private Level level;
 @Enumerated(EnumType.STRING) @Column(nullable=false) private Major major;
 @Column(nullable=false,columnDefinition="TEXT") private String response;
 @Column(nullable=false,length=20) private String status="PENDING";
 private Integer languageScore,technicalScore,communicationScore,taskScore;
 @Column(columnDefinition="TEXT") private String feedback;
 private Long reviewerId;
 @Column(nullable=false) private LocalDateTime createdAt;
 private LocalDateTime reviewedAt;
 protected LanguageSubmission(){}
 public LanguageSubmission(Long studentId,Level level,Major major,String response,LocalDateTime now){this.studentId=studentId;this.level=level;this.major=major;this.response=response;this.createdAt=now;}
 public void review(Long reviewer,int language,int technical,int communication,int task,String feedback,LocalDateTime now){
  this.reviewerId=reviewer;this.languageScore=language;this.technicalScore=technical;this.communicationScore=communication;this.taskScore=task;this.feedback=feedback;this.reviewedAt=now;
  this.status=language>=3&&technical>=3&&communication>=3&&task>=3&&language+technical+communication+task>=14?"PASSED":"REVISION";
 }
 public Long getId(){return id;} public Long getStudentId(){return studentId;} public Level getLevel(){return level;} public Major getMajor(){return major;}
 public String getResponse(){return response;} public String getStatus(){return status;} public String getFeedback(){return feedback;}
 public Integer getLanguageScore(){return languageScore;} public Integer getTechnicalScore(){return technicalScore;}
 public Integer getCommunicationScore(){return communicationScore;} public Integer getTaskScore(){return taskScore;}
 public LocalDateTime getCreatedAt(){return createdAt;} public LocalDateTime getReviewedAt(){return reviewedAt;}
}
