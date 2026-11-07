package vn.sanka.lms.language;

import jakarta.persistence.*;
import org.springframework.context.annotation.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import vn.sanka.lms.auth.Student;
import vn.sanka.lms.learning.*;
import vn.sanka.lms.assessment.*;
import java.time.*;
import java.util.*;

@Service
public class LanguageService {
 private final LearningService learning;
 private final LearningCatalog exams;
 private final LanguageCatalog content;
 private final WorkflowService workflow;
 private final LanguageEnrollmentRepository enrollments;
 private final LanguageReviewRepository reviews;
 private final LanguageSubmissionRepository submissions;
 private final Clock clock;
 private final vn.sanka.lms.gamification.GamificationService gamification;
 @PersistenceContext private EntityManager em;
 public LanguageService(LearningService learning,LearningCatalog exams,LanguageCatalog content,WorkflowService workflow,
     LanguageEnrollmentRepository enrollments,LanguageReviewRepository reviews,LanguageSubmissionRepository submissions,Clock clock,vn.sanka.lms.gamification.GamificationService gamification){
  this.learning=learning;this.exams=exams;this.content=content;this.workflow=workflow;
  this.enrollments=enrollments;this.reviews=reviews;this.submissions=submissions;this.clock=clock;
  this.gamification=gamification;
 }
 // The former level-course module is retired. Preserve records, deny new activity.
 public boolean open(Student s,Level level){return false;}
 public void require(Student s,Level level){workflow.require(open(s,level));}
 public List<Lesson> lessons(Student s,Level level){require(s,level);return learning.available(s).stream().filter(l->l.getMajor()==null && l.getLevel()==level).toList();}
 public LanguageEnrollment enrollment(Student s,Level level){return enrollments.findByStudentIdAndLevel(s.getId(),level).orElse(null);}
 public boolean due(LanguageEnrollment e){return e!=null && !e.getNextReviewAt().isAfter(LocalDate.now(clock));}
 @Transactional public void enroll(String email,Level level){
  var s=locked(email);require(s,level);
  if(enrollment(s,level)==null)enrollments.save(new LanguageEnrollment(s.getId(),level,LocalDate.now(clock)));
 }
 private Student locked(String email){var s=learning.student(email);em.lock(s,LockModeType.PESSIMISTIC_WRITE);em.refresh(s);return s;}
 public record CourseCard(Level level,String theme,boolean open,LanguageEnrollment enrollment,boolean due){}
 public List<CourseCard> courses(Student s){return Arrays.stream(Level.values()).map(l->{var e=enrollment(s,l);return new CourseCard(l,content.course(l).theme(),open(s,l),e,due(e));}).toList();}
 public List<LearningCatalog.QuestionView> questions(Student s,Level level){
  require(s,level);workflow.require(enrollment(s,level)!=null);
  return exams.questionsForLevel(level,s.getMajor()).stream().map(LearningCatalog.Question::view).toList();
 }
 @Transactional public LanguageReview review(String email,Level level,Map<String,String> answers){
  var s=locked(email);require(s,level);var enrollment=enrollment(s,level);workflow.require(enrollment!=null);
  var questions=exams.questionsForLevel(level,s.getMajor());
  int correct=0;var evidence=new StringBuilder("Kiểm tra ôn tập định kỳ · không thay đổi trình độ\n");
  for(var q:questions){
   int chosen;
   try{chosen=Integer.parseInt(answers.get("answer_"+q.id()));}catch(Exception e){throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Hãy trả lời đầy đủ câu hỏi");}
   if(chosen<0||chosen>=q.options().size())throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Lựa chọn không hợp lệ");
   boolean right=chosen==q.correct();if(right)correct++;
   evidence.append("[").append(q.skill()).append("] ").append(q.prompt()).append("\nĐã chọn: ").append(q.options().get(chosen))
    .append(right?" ✓\n":" ✗\n").append("Đáp án: ").append(q.options().get(q.correct())).append("\n").append(q.explanation()).append("\n\n");
  }
  enrollment.reviewed(LocalDate.now(clock));
  var result=reviews.save(new LanguageReview(s.getId(),level,correct*100/questions.size(),evidence.toString(),LocalDateTime.now(clock)));
  gamification.periodic(s.getId(),level);return result;
 }
 public LanguageReview result(Student s,Long id){return reviews.findById(id).filter(r->r.getStudentId().equals(s.getId())).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));}
 public List<LanguageReview> history(Student s){return reviews.findByStudentIdOrderByCreatedAtDesc(s.getId());}
 public List<LanguageSubmission> work(Student s){return submissions.findByStudentIdOrderByCreatedAtDesc(s.getId());}
 @Transactional public void submit(String email,Level level,String response){
  var s=locked(email);require(s,level);
  if(response==null||response.strip().length()<content.minimum(level)||response.length()>10000)
   throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Bài làm cần từ "+content.minimum(level)+" đến 10.000 ký tự");
  if(submissions.existsByStudentIdAndLevelAndMajorAndStatus(s.getId(),level,s.getMajor(),"PENDING"))
   throw new ResponseStatusException(HttpStatus.CONFLICT,"Bạn đã có bài cùng cấp độ/ngành đang chờ chấm");
  submissions.save(new LanguageSubmission(s.getId(),level,s.getMajor(),response.strip(),LocalDateTime.now(clock)));
  gamification.submitted(s.getId(),"language:"+level+":"+s.getMajor());
 }
 public List<LanguageSubmission> pending(){return submissions.findByStatusOrderByCreatedAtAsc("PENDING");}
 @Transactional public void grade(String email,Long id,int language,int technical,int communication,int task,String feedback){
  var teacher=learning.student(email);workflow.require("INSTRUCTOR".equals(teacher.getRole()));
  var submission=submissions.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));
  em.lock(submission,LockModeType.PESSIMISTIC_WRITE);em.refresh(submission);
  workflow.require(!submission.getStudentId().equals(teacher.getId()));
  if(!"PENDING".equals(submission.getStatus()))throw new ResponseStatusException(HttpStatus.CONFLICT,"Bài đã được chấm");
  if(java.util.stream.IntStream.of(language,technical,communication,task).anyMatch(x->x<0||x>5)||feedback==null||feedback.strip().length()<10||feedback.length()>4000)
   throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Điểm cần từ 0–5 và phản hồi từ 10–4.000 ký tự");
  submission.review(teacher.getId(),language,technical,communication,task,feedback.strip(),LocalDateTime.now(clock));
  if("PASSED".equals(submission.getStatus()))gamification.languagePassed(submission.getStudentId(),submission.getLevel(),submission.getMajor(),submission.getCreatedAt());
 }
 @Configuration static class TimeConfiguration {
  @Bean Clock languageClock(){return Clock.system(ZoneId.of("Asia/Ho_Chi_Minh"));}
 }
}
