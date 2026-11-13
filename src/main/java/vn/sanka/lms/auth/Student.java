package vn.sanka.lms.auth;
import jakarta.persistence.*;
import vn.sanka.lms.learning.*;
@Entity @Table(name="students")
public class Student {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(nullable=false,unique=true,length=190) private String email;
 @Column(nullable=false) private String password;
 @Column(name="google_subject",unique=true,length=255) private String googleSubject;
 @Column(nullable=false,length=80) private String name;
 @Enumerated(EnumType.STRING) private Major major;
 @Enumerated(EnumType.STRING) private Level level;
 @Column(nullable=false) private boolean placementCompleted;
 @Column(nullable=false,length=20) private String role="STUDENT";
 protected Student(){}
 public Student(String name,String email,String password){this.name=name;this.email=email;this.password=password;}
 public String getGoogleSubject(){return googleSubject;}
 void linkGoogle(String subject){this.googleSubject=subject;}
 public Long getId(){return id;} public String getEmail(){return email;} public String getPassword(){return password;} public String getName(){return name;}
 public Major getMajor(){return major;} public Level getLevel(){return level;}
 public boolean isPlacementCompleted(){return placementCompleted;}
 public String getRole(){return role;}
 public void setRole(String role){this.role=role;}
 public void onboard(Major major,Level ignored){this.major=major;if(this.level==null)this.level=Level.ZERO;}
 public void assess(Level level){this.level=level;this.placementCompleted=true;}
}
