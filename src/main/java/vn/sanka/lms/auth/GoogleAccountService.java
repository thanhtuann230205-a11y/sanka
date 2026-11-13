package vn.sanka.lms.auth;

import jakarta.persistence.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.*;
import java.util.*;

@Service
public class GoogleAccountService {
 private final StudentRepository students;
 private final PasswordEncoder encoder;
 @PersistenceContext private EntityManager em;
 public GoogleAccountService(StudentRepository students,PasswordEncoder encoder){this.students=students;this.encoder=encoder;}
 @Transactional public Student resolve(String subject,String email,boolean verified,String hostedDomain,String name){
  if(subject==null||subject.isBlank()||subject.length()>255||email==null||!verified)throw denied();
  email=email.strip().toLowerCase(Locale.ROOT);
  if(email.length()>190||!email.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+"))throw denied();
  var linked=students.findByGoogleSubject(subject);
  if(linked.isPresent())return linked.get();
  // Google is authoritative for Gmail / verified Google Workspace addresses.
  // Other Google-account emails must use the email-code registration flow.
  if(!email.endsWith("@gmail.com")&&(hostedDomain==null||hostedDomain.isBlank()))throw denied();
  var existing=students.findByEmail(email);
  Student student;
  if(existing.isPresent()){
   student=existing.get();em.lock(student,LockModeType.PESSIMISTIC_WRITE);em.refresh(student);
   if(student.getGoogleSubject()!=null&&!subject.equals(student.getGoogleSubject()))throw denied();
  }else{
   String display=name==null||name.isBlank()?email.substring(0,email.indexOf('@')):name.strip();
   if(display.length()>80)display=display.substring(0,80);
   student=new Student(display,email,encoder.encode(UUID.randomUUID().toString()));
  }
  student.linkGoogle(subject);return students.saveAndFlush(student);
 }
 private OAuth2AuthenticationException denied(){return new OAuth2AuthenticationException(new OAuth2Error("google_account_unverified"),"Không thể xác minh tài khoản Google. Hãy dùng Gmail hoặc đăng ký bằng mã email.");}
}
