package vn.sanka.lms.learning;
import vn.sanka.lms.auth.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
@Service
public class LearningService {
 private final StudentRepository students; private final LessonRepository lessons; private final ProgressRepository progress;
 private final vn.sanka.lms.gamification.GamificationService gamification;
 private final vn.sanka.lms.assessment.AttemptRepository attempts;
 public LearningService(StudentRepository students,LessonRepository lessons,ProgressRepository progress,vn.sanka.lms.gamification.GamificationService gamification,vn.sanka.lms.assessment.AttemptRepository attempts){this.students=students;this.lessons=lessons;this.progress=progress;this.gamification=gamification;this.attempts=attempts;}
 public Student student(String email){return students.findByEmail(email).orElseThrow(()->new ResponseStatusException(HttpStatus.UNAUTHORIZED));}
 public List<Lesson> available(Student s){
  if(!s.isPlacementCompleted())return List.of();
  boolean foundation=needsFoundation(s),s4=foundation && s4Foundation(s);
  return lessons.findAll().stream().filter(l -> foundation
   ? l.getMajor()==null && (s4?l.getLevel()==Level.N4:(l.getLevel()==Level.ZERO || l.getLevel()==Level.N5))
   : l.getMajor()!=null && l.getMajor()==s.getMajor())
   .sorted(Comparator.comparing(Lesson::getLevel).thenComparing(l->l.getMajor()!=null).thenComparing(Lesson::getId)).toList();
 }
 public Set<Long> completed(Student s){return new HashSet<>(progress.findByStudentId(s.getId()).stream().map(LessonProgress::getLessonId).toList());}
 public List<Lesson> currentCourse(Student s){
  return available(s);
 }
 public boolean needsFoundation(Student s){return s.isPlacementCompleted() && s.getLevel().ordinal()<Level.N4.ordinal();}
 public boolean canChooseMajor(Student s){return s.isPlacementCompleted() && s.getLevel().ordinal()>=Level.N4.ordinal();}
 public boolean s4Foundation(Student s){return needsFoundation(s) && attempts.existsByStudentIdAndStageAndPassedTrue(s.getId(),vn.sanka.lms.assessment.Stage.N5);}
 public boolean foundationCompleted(Student s){
  var course=currentCourse(s);var done=completed(s);
  return needsFoundation(s) && !course.isEmpty() && course.stream().allMatch(l->done.contains(l.getId()));
 }
 public String courseTitle(Student s){
  return switch(s.getLevel()){
   case ZERO,N5 -> s4Foundation(s)?"Tiếng Nhật cơ bản S4 · tương đương N4":"Tiếng Nhật cơ bản S5 · tương đương N5";
   default -> s.getMajor()==null?"Chọn chuyên ngành":"Tiếng Nhật chuyên ngành · " + s.getMajor().getLabel();
  };
 }
 public String courseJourney(Student s){
  return switch(s.getLevel()){
   case ZERO,N5 -> s4Foundation(s)?"Cơ bản S4 → Kiểm tra đạt S4 → Chọn chuyên ngành":"Cơ bản S5 → Cơ bản S4 → Đạt S4 → Chọn chuyên ngành";
   default -> "Chuyên ngành → Thực hành → Đánh giá ứng dụng";
  };
 }
 public Lesson lesson(Student s,Long id){return available(s).stream().filter(l -> l.getId().equals(id)).findFirst().orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));}
 @Transactional public void onboard(String email,Major major,Level level){Student s=student(email);em.lock(s,jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);em.refresh(s);if(!canChooseMajor(s))throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Cần đạt S4 trở lên trước khi chọn chuyên ngành");s.onboard(major,level);students.save(s);}
 @Transactional public void complete(String email,Long id){
  Student s=students.findById(student(email).getId()).orElseThrow();
  // Lock the student row to serialize repeated completion requests.
  completeLocked(s,id);
 }
 @jakarta.persistence.PersistenceContext private jakarta.persistence.EntityManager em;
 private void completeLocked(Student s,Long id){em.lock(s,jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);em.refresh(s);lesson(s,id);if(!progress.existsByStudentIdAndLessonId(s.getId(),id)){progress.save(new LessonProgress(s.getId(),id,gamification.now()));gamification.lesson(s.getId(),id);}}
}
