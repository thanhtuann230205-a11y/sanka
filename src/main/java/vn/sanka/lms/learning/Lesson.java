package vn.sanka.lms.learning;
import jakarta.persistence.*;
@Entity @Table(name="lessons")
public class Lesson {
 @Id private Long id;
 @Column(nullable=false) private String title;
 @Column(nullable=false,columnDefinition="TEXT") private String content;
 @Enumerated(EnumType.STRING) @Column(nullable=false) private Level level;
 @Enumerated(EnumType.STRING) private Major major;
 @Column(nullable=false) private int minutes;
 protected Lesson(){}
 public Long getId(){return id;} public String getTitle(){return title;} public String getContent(){return content;}
 public Level getLevel(){return level;} public Major getMajor(){return major;} public int getMinutes(){return minutes;}
}