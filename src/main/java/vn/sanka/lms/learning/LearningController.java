package vn.sanka.lms.learning;
import java.security.Principal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
@Controller
public class LearningController {
 private final LearningService learning; private final TermRepository terms;
 private final vn.sanka.lms.gamification.GamificationService gamification;
 public LearningController(LearningService learning,TermRepository terms,vn.sanka.lms.gamification.GamificationService gamification){this.learning=learning;this.terms=terms;this.gamification=gamification;}
 @ModelAttribute void common(Principal p,Model m){if(p!=null)m.addAttribute("student",learning.student(p.getName()));m.addAttribute("majors",Major.values());m.addAttribute("levels",Level.values());}
 @GetMapping("/onboarding") String onboarding(){return "onboarding";}
 @GetMapping("/start") String start(Principal p){
  var s=learning.student(p.getName());
  if("INSTRUCTOR".equals(s.getRole()))return "redirect:/instructor/reviews";
  if(s.getMajor()==null)return "redirect:/onboarding";
  if(!s.isPlacementCompleted())return "redirect:/exams/PLACEMENT";
  return "redirect:/dashboard";
 }
 @PostMapping("/onboarding") String onboard(Principal p,@RequestParam Major major){learning.onboard(p.getName(),major,Level.ZERO);return "redirect:/start";}
 @GetMapping({"/dashboard","/path"}) String dashboard(Principal p,Model m,jakarta.servlet.http.HttpServletRequest request){
  var s=learning.student(p.getName());if(s.getMajor()==null)return "redirect:/onboarding";
  if(!s.isPlacementCompleted())return "redirect:/exams/PLACEMENT";
  var lessons=learning.currentCourse(s);var done=learning.completed(s);
  m.addAttribute("courseTitle",learning.courseTitle(s));
  m.addAttribute("courseJourney",learning.courseJourney(s));
  m.addAttribute("courseNextUrl",s.getLevel()==Level.ZERO?"/exams/N5":s.getLevel()==Level.N5?"/exams/N4":"/specialized");
  m.addAttribute("courseNextLabel",s.getLevel()==Level.ZERO?"Kiểm tra đạt N5 →":s.getLevel()==Level.N5?"Kiểm tra đạt N4 →":"Học phần chuyên ngành tiếp theo →");
  long count=lessons.stream().filter(l->done.contains(l.getId())).count();
  m.addAttribute("lessons",lessons);m.addAttribute("completed",done);m.addAttribute("completedCount",count);
  var motivation=gamification.overview(s.getId());m.addAttribute("motivation",motivation);
  m.addAttribute("percent",lessons.isEmpty()?0:count*100/lessons.size());m.addAttribute("xp",motivation.totalXp());
  m.addAttribute("nextLesson",lessons.stream().filter(l->!done.contains(l.getId())).findFirst().orElse(null));
  return request.getRequestURI().equals("/path")?"path":"dashboard";
 }
 @GetMapping("/lessons/{id}") String lesson(Principal p,@PathVariable Long id,Model m){var s=learning.student(p.getName());if(s.getMajor()==null)return "redirect:/onboarding";m.addAttribute("lesson",learning.lesson(s,id));m.addAttribute("done",learning.completed(s).contains(id));return "lesson";}
 @PostMapping("/lessons/{id}/complete") String complete(Principal p,@PathVariable Long id){learning.complete(p.getName(),id);return "redirect:/dashboard";}
 @GetMapping("/dictionary") String dictionary(@RequestParam(defaultValue="") String q,@RequestParam(required=false) Major major,Model m){
  String query=q.strip().toLowerCase(java.util.Locale.ROOT);
  m.addAttribute("terms",terms.findAll().stream().filter(t->major==null||t.getMajor()==major).filter(t->(t.getJapanese()+t.getReading()+t.getVietnamese()).toLowerCase(java.util.Locale.ROOT).contains(query)).toList());
  m.addAttribute("q",q);m.addAttribute("selectedMajor",major);return "dictionary";
 }
}
