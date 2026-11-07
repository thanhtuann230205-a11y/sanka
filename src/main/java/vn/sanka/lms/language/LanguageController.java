package vn.sanka.lms.language;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.sanka.lms.learning.*;
import vn.sanka.lms.assessment.*;
import java.security.Principal;
import java.util.*;

@Controller
public class LanguageController {
 private final LanguageService language;
 private final LanguageCatalog content;
 private final LearningService learning;
 private final WorkflowService workflow;
 public LanguageController(LanguageService language,LanguageCatalog content,LearningService learning,WorkflowService workflow){this.language=language;this.content=content;this.learning=learning;this.workflow=workflow;}
 @ModelAttribute void common(Principal p,Model m){if(p!=null)m.addAttribute("student",learning.student(p.getName()));}
 @GetMapping("/language") String courses(Principal p,Model m){return "redirect:/start";}
 @GetMapping("/language/{level}") String course(Principal p,@PathVariable Level level,Model m){
  var s=learning.student(p.getName());language.require(s,level);
  m.addAttribute("course",content.course(level));m.addAttribute("lessons",language.lessons(s,level));m.addAttribute("completed",learning.completed(s));
  var enrollment=language.enrollment(s,level);m.addAttribute("enrollment",enrollment);m.addAttribute("due",language.due(enrollment));
  m.addAttribute("minimum",content.minimum(level));
  Stage exam=level==Level.ZERO?null:Stage.valueOf(level.name());m.addAttribute("exam",exam);m.addAttribute("examAllowed",exam!=null&&workflow.allowed(s,exam));
  var work=language.work(s).stream().filter(w->w.getLevel()==level&&w.getMajor()==s.getMajor()).toList();
  m.addAttribute("work",work);m.addAttribute("pending",work.stream().anyMatch(w->"PENDING".equals(w.getStatus())));
  if(!work.isEmpty() && "REVISION".equals(work.getFirst().getStatus()))m.addAttribute("responseText",work.getFirst().getResponse());
  return "language-course";
 }
 @PostMapping("/language/{level}/enroll") String enroll(Principal p,@PathVariable Level level){language.enroll(p.getName(),level);return "redirect:/language/"+level;}
 @GetMapping("/language/{level}/review") String review(Principal p,@PathVariable Level level,Model m){
  var s=learning.student(p.getName());m.addAttribute("questions",language.questions(s,level));m.addAttribute("level",level);return "language-review";
 }
 @PostMapping("/language/{level}/review") String review(Principal p,@PathVariable Level level,@RequestParam Map<String,String> answers){return "redirect:/language/results/"+language.review(p.getName(),level,answers).getId();}
 @GetMapping("/language/results/{id}") String result(Principal p,@PathVariable Long id,Model m){m.addAttribute("result",language.result(learning.student(p.getName()),id));return "language-result";}
 @GetMapping("/language/history") String history(Principal p,Model m){var s=learning.student(p.getName());m.addAttribute("reviews",language.history(s));m.addAttribute("work",language.work(s));m.addAttribute("courses",language.courses(s));return "language-history";}
 @PostMapping("/language/{level}/application") String application(Principal p,@PathVariable Level level,@RequestParam String response,RedirectAttributes flash,Model m,jakarta.servlet.http.HttpServletResponse http){
  try{language.submit(p.getName(),level,response);}catch(org.springframework.web.server.ResponseStatusException error){
   if(error.getStatusCode().value()!=400)throw error;course(p,level,m);m.addAttribute("responseText",response);m.addAttribute("error",error.getReason());http.setStatus(400);return "language-course";
  }
  flash.addFlashAttribute("message","Đã nộp bài ứng dụng. Giảng viên sẽ chấm và phản hồi trong lịch sử học tiếng Nhật.");return "redirect:/language/history";
 }
 @GetMapping("/instructor/language-reviews") String pending(Model m){m.addAttribute("work",language.pending());return "language-instructor";}
 @PostMapping("/instructor/language-reviews/{id}") String grade(Principal p,@PathVariable Long id,@RequestParam int language,@RequestParam int technical,@RequestParam int communication,@RequestParam int task,@RequestParam String feedback,RedirectAttributes flash){
  this.language.grade(p.getName(),id,language,technical,communication,task,feedback);flash.addFlashAttribute("message","Đã lưu điểm và phản hồi.");return "redirect:/instructor/language-reviews";
 }
}
