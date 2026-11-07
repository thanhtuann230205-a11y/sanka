package vn.sanka.lms.gamification;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity @Table(name="earned_badges",uniqueConstraints=@UniqueConstraint(columnNames={"student_id","code"}))
public class EarnedBadge {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(nullable=false) private Long studentId;
    @Column(nullable=false,length=40) private String code;
    @Column(nullable=false) private LocalDateTime earnedAt;
    protected EarnedBadge(){}
    public EarnedBadge(Long studentId,String code,LocalDateTime time){this.studentId=studentId;this.code=code;this.earnedAt=time;}
    public String getCode(){return code;}
    public LocalDateTime getEarnedAt(){return earnedAt;}
}
