package vn.sanka.lms.learning;
import jakarta.persistence.*;
@Entity @Table(name="terms")
public class Term {
 @Id private Long id;
 @Column(nullable=false) private String japanese;
 @Column(nullable=false) private String reading;
 @Column(nullable=false) private String vietnamese;
 @Column(nullable=false) private String example;
 @Enumerated(EnumType.STRING) @Column(nullable=false) private Major major;
 protected Term(){}
 public Long getId(){return id;}
 public String getJapanese(){return japanese;} public String getReading(){return reading;} public String getVietnamese(){return vietnamese;}
 public String getExample(){return example;} public Major getMajor(){return major;}
}
