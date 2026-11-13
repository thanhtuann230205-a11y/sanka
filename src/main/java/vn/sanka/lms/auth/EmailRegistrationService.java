package vn.sanka.lms.auth;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.time.*;
import java.security.SecureRandom;
import java.util.*;
@Service
public class EmailRegistrationService {
 private final EmailRegistrationRepository pending;
 private final StudentRepository students;
 private final PasswordEncoder encoder;
 private final VerificationMailer mailer;
 @jakarta.persistence.PersistenceContext private jakarta.persistence.EntityManager em;
 private final SecureRandom random=new SecureRandom();
 public EmailRegistrationService(EmailRegistrationRepository pending,StudentRepository students,PasswordEncoder encoder,VerificationMailer mailer){this.pending=pending;this.students=students;this.encoder=encoder;this.mailer=mailer;}
 private LocalDateTime now(){return LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"));}
 @Transactional public String start(String name,String email,String password){
  if(students.findByEmail(email).isPresent())throw new ResponseStatusException(HttpStatus.CONFLICT,"Email đã được đăng ký");
  var entry=pending.lockEmail(email).orElseGet(()->new EmailRegistration(email));
  prepare(entry);
  entry.token=UUID.randomUUID().toString();entry.name=name.strip();entry.passwordHash=encoder.encode(password);
  issue(entry);return entry.token;
 }
 private void prepare(EmailRegistration entry){
  var time=now();
  if(entry.sentAt!=null&&time.isBefore(entry.sentAt.plusSeconds(60)))throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,"Vui lòng chờ 60 giây trước khi gửi lại mã.");
  if(entry.windowStart==null||!time.isBefore(entry.windowStart.plusHours(1))){entry.windowStart=time;entry.sendCount=0;}
  if(entry.sendCount>=5)throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,"Đã gửi tối đa 5 mã trong một giờ. Vui lòng thử lại sau.");
 }
 private void issue(EmailRegistration entry){
  String code=String.format(Locale.ROOT,"%06d",random.nextInt(1000000));
  entry.codeHash=encoder.encode(code);entry.sentAt=now();entry.expiresAt=entry.sentAt.plusMinutes(10);entry.attempts=0;entry.sendCount++;
  pending.saveAndFlush(entry);mailer.send(entry.email,code);
 }
 public String email(String token){return pending.findByToken(token).map(e->e.email).orElse(null);}
 @Transactional public void resend(String token){
  var found=pending.findByToken(token).orElseThrow(()->new ResponseStatusException(HttpStatus.BAD_REQUEST,"Phiên xác nhận đã hết. Hãy đăng ký lại."));
  var entry=pending.lockEmail(found.email).orElseThrow();em.refresh(entry);
  if(!entry.token.equals(token))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Phiên xác nhận đã thay đổi. Hãy đăng ký lại.");
  prepare(entry);issue(entry);
 }
 public enum Result{SUCCESS,INVALID,EXPIRED,LOCKED,DUPLICATE}
 @Transactional public Result verify(String token,String code){
  var found=pending.findByToken(token).orElse(null);if(found==null)return Result.EXPIRED;
  var entry=pending.lockEmail(found.email).orElse(null);if(entry!=null)em.refresh(entry);
  if(entry==null||!entry.token.equals(token)||!now().isBefore(entry.expiresAt))return Result.EXPIRED;
  if(entry.attempts>=5)return Result.LOCKED;
  entry.attempts++;
  if(code==null||!code.matches("[0-9]{6}")||!encoder.matches(code,entry.codeHash))return entry.attempts>=5?Result.LOCKED:Result.INVALID;
  if(students.findByEmail(entry.email).isPresent())return Result.DUPLICATE;
  students.saveAndFlush(new Student(entry.name,entry.email,entry.passwordHash));
  pending.delete(entry);return Result.SUCCESS;
 }
}
