package vn.sanka.lms.assessment;

import jakarta.persistence.*;
import vn.sanka.lms.learning.Major;
import java.time.LocalDateTime;

@Entity @Table(name="practice_drafts",uniqueConstraints=@UniqueConstraint(columnNames={"student_id","major","kind"}))
public class PracticeDraft {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(nullable=false) private Long studentId;
    @Enumerated(EnumType.STRING) @Column(nullable=false) private Major major;
    @Enumerated(EnumType.STRING) @Column(nullable=false) private PracticeKind kind;
    @Column(nullable=false,columnDefinition="TEXT") private String content;
    @Column(nullable=false) private LocalDateTime updatedAt;
    protected PracticeDraft() {}
    public PracticeDraft(Long studentId,Major major,PracticeKind kind){this.studentId=studentId;this.major=major;this.kind=kind;}
    public void update(String content){this.content=content;this.updatedAt=LocalDateTime.now();}
    public String getContent(){return content;}
    public LocalDateTime getUpdatedAt(){return updatedAt;}
}
