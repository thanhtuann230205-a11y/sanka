package vn.sanka.lms.auth;
import jakarta.persistence.*;
import java.time.LocalDateTime;
@Entity @Table(name="email_registrations")
public class EmailRegistration {
 @Id @Column(length=190) String email;
 @Column(name="request_token",nullable=false,length=36) String token;
 @Column(name="student_name",nullable=false,length=80) String name;
 @Column(name="password_hash",nullable=false) String passwordHash;
 @Column(name="code_hash",nullable=false) String codeHash;
 @Column(name="expires_at",nullable=false) LocalDateTime expiresAt;
 @Column(name="sent_at",nullable=false) LocalDateTime sentAt;
 @Column(name="window_start",nullable=false) LocalDateTime windowStart;
 @Column(nullable=false) int attempts;
 @Column(name="send_count",nullable=false) int sendCount;
 protected EmailRegistration(){}
 EmailRegistration(String email){this.email=email;}
}
