package vn.sanka.lms.gamification;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity @Table(name="daily_goals",uniqueConstraints=@UniqueConstraint(columnNames={"student_id","activity_date"}))
public class DailyGoal {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(nullable=false) private Long studentId;
    @Column(nullable=false) private LocalDate activityDate;
    @Column(nullable=false) private int targetXp;
    protected DailyGoal(){}
    public DailyGoal(Long studentId,LocalDate day,int targetXp){this.studentId=studentId;this.activityDate=day;this.targetXp=targetXp;}
    public LocalDate getActivityDate(){return activityDate;}
    public int getTargetXp(){return targetXp;}
    public void target(int value){this.targetXp=value;}
}
