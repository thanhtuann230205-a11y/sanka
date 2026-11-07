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
 @GetMapping("/onboarding") String onboarding(Principal p){var s=learning.student(p.getName());if(!s.isPlacementCompleted())return "redirect:/exams/PLACEMENT";return learning.canChooseMajor(s)?"onboarding":"redirect:/dashboard";}
 @GetMapping("/start") String start(Principal p){
  var s=learning.student(p.getName());
  if("INSTRUCTOR".equals(s.getRole()))return "redirect:/instructor/reviews";
  if(!s.isPlacementCompleted())return "redirect:/exams/PLACEMENT";
  if(learning.canChooseMajor(s) && s.getMajor()==null)return "redirect:/onboarding";
  return "redirect:/dashboard";
 }
 @PostMapping("/onboarding") String onboard(Principal p,@RequestParam Major major){learning.onboard(p.getName(),major,Level.ZERO);return "redirect:/start";}
 @GetMapping({"/dashboard","/path"}) String dashboard(Principal p,Model m,jakarta.servlet.http.HttpServletRequest request){
  var s=learning.student(p.getName());
  if(!s.isPlacementCompleted())return "redirect:/exams/PLACEMENT";
  if(learning.canChooseMajor(s) && s.getMajor()==null)return "redirect:/onboarding";
  var lessons=learning.currentCourse(s);var done=learning.completed(s);
  m.addAttribute("courseTitle",learning.courseTitle(s));
  m.addAttribute("courseJourney",learning.courseJourney(s));
  m.addAttribute("foundation",learning.needsFoundation(s));
  m.addAttribute("foundationCompleted",learning.foundationCompleted(s));
  m.addAttribute("courseNextUrl",learning.needsFoundation(s)?(learning.s4Foundation(s)?"/exams/N4":"/exams/N5"):"/specialized");
  m.addAttribute("courseNextLabel",learning.needsFoundation(s)?(learning.s4Foundation(s)?"Kiểm tra đạt S4 →":"Kiểm tra đạt S5 →"):"Học phần chuyên ngành tiếp theo →");
  long count=lessons.stream().filter(l->done.contains(l.getId())).count();
  m.addAttribute("lessons",lessons);m.addAttribute("completed",done);m.addAttribute("completedCount",count);
  var motivation=gamification.overview(s.getId());m.addAttribute("motivation",motivation);
  m.addAttribute("percent",lessons.isEmpty()?0:count*100/lessons.size());m.addAttribute("xp",motivation.totalXp());
  m.addAttribute("nextLesson",lessons.stream().filter(l->!done.contains(l.getId())).findFirst().orElse(null));
  return request.getRequestURI().equals("/path")?"path":"dashboard";
 }
 @GetMapping("/lessons/{id}") String lesson(Principal p,@PathVariable Long id,Model m){var s=learning.student(p.getName());if(!s.isPlacementCompleted())return "redirect:/exams/PLACEMENT";m.addAttribute("lesson",learning.lesson(s,id));m.addAttribute("done",learning.completed(s).contains(id));return "lesson";}
 @PostMapping("/lessons/{id}/complete") String complete(Principal p,@PathVariable Long id){learning.complete(p.getName(),id);return "redirect:/dashboard";}
 @GetMapping("/dictionary") String dictionary(@RequestParam(defaultValue="") String q,@RequestParam(required=false) Major major,Model m){
  String query=q.strip().toLowerCase(java.util.Locale.ROOT);
  m.addAttribute("terms",terms.findAll().stream().filter(t->major==null||t.getMajor()==major).filter(t->(t.getJapanese()+t.getReading()+t.getVietnamese()).toLowerCase(java.util.Locale.ROOT).contains(query)).toList());
  m.addAttribute("q",q);m.addAttribute("selectedMajor",major);return "dictionary";
 }
}
