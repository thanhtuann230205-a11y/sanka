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
 public LearningService(StudentRepository students,LessonRepository lessons,ProgressRepository progress,vn.sanka.lms.gamification.GamificationService gamification){this.students=students;this.lessons=lessons;this.progress=progress;this.gamification=gamification;}
 public Student student(String email){return students.findByEmail(email).orElseThrow(()->new ResponseStatusException(HttpStatus.UNAUTHORIZED));}
 public List<Lesson> available(Student s){
  if(!s.isPlacementCompleted() || s.getMajor()==null)return List.of();
  return lessons.findAll().stream().filter(l -> l.getMajor()==null
   ? l.getLevel().ordinal()<=Math.max(Level.N5.ordinal(),s.getLevel().ordinal()+1)
   : l.getMajor()==s.getMajor() && s.getLevel().ordinal()>=Level.N4.ordinal())
   .sorted(Comparator.comparing(Lesson::getLevel).thenComparing(l->l.getMajor()!=null).thenComparing(Lesson::getId)).toList();
 }
 public Set<Long> completed(Student s){return new HashSet<>(progress.findByStudentId(s.getId()).stream().map(LessonProgress::getLessonId).toList());}
 public List<Lesson> currentCourse(Student s){
  return available(s).stream().filter(l -> switch(s.getLevel()){
   case ZERO -> l.getMajor()==null && (l.getLevel()==Level.ZERO || l.getLevel()==Level.N5);
   case N5 -> l.getMajor()==null && l.getLevel()==Level.N4;
   default -> l.getMajor()==s.getMajor();
  }).toList();
 }
 public String courseTitle(Student s){
  return switch(s.getLevel()){
   case ZERO -> "Tiếng Nhật nền tảng N5";
   case N5 -> "Tiếng Nhật N4";
   default -> "Tiếng Nhật chuyên ngành · " + s.getMajor().getLabel();
  };
 }
 public String courseJourney(Student s){
  return switch(s.getLevel()){
   case ZERO -> "N5 → N4 → Chuyên ngành";
   case N5 -> "N4 → Chuyên ngành";
   default -> "Chuyên ngành → Thực hành → Đánh giá ứng dụng";
  };
 }
 public Lesson lesson(Student s,Long id){return available(s).stream().filter(l -> l.getId().equals(id)).findFirst().orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));}
 @Transactional public void onboard(String email,Major major,Level level){Student s=student(email);em.lock(s,jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);em.refresh(s);s.onboard(major,level);students.save(s);}
 @Transactional public void complete(String email,Long id){
  Student s=students.findById(student(email).getId()).orElseThrow();
  // Lock the student row to serialize repeated completion requests.
  completeLocked(s,id);
 }
 @jakarta.persistence.PersistenceContext private jakarta.persistence.EntityManager em;
 private void completeLocked(Student s,Long id){em.lock(s,jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);em.refresh(s);lesson(s,id);if(!progress.existsByStudentIdAndLessonId(s.getId(),id)){progress.save(new LessonProgress(s.getId(),id,gamification.now()));gamification.lesson(s.getId(),id);}}
}
