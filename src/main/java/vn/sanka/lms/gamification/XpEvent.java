package vn.sanka.lms.gamification;

import jakarta.persistence.*;
import java.time.*;

@Entity @Table(name="xp_events",uniqueConstraints=@UniqueConstraint(columnNames={"student_id","event_key"}))
public class XpEvent {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(nullable=false) private Long studentId;
    @Column(nullable=false,length=190) private String eventKey;
    @Enumerated(EnumType.STRING) @Column(nullable=false,length=40) private ActivityKind kind;
    @Column(nullable=false) private int points;
    @Column(nullable=false) private LocalDate activityDate;
    @Column(nullable=false) private LocalDateTime occurredAt;
    protected XpEvent(){}
    public XpEvent(Long studentId,String key,ActivityKind kind,int points,LocalDateTime now){
        this.studentId=studentId;this.eventKey=key;this.kind=kind;this.points=points;this.occurredAt=now;this.activityDate=now.toLocalDate();
    }
    public Long getId(){return id;}
    public ActivityKind getKind(){return kind;}
    public int getPoints(){return points;}
    public LocalDate getActivityDate(){return activityDate;}
    public LocalDateTime getOccurredAt(){return occurredAt;}
}
