package vn.sanka.lms.learning;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.security.Principal;
import java.util.Map;
import vn.sanka.lms.auth.Student;

@Controller
public class FlashcardController {
 private final LearningService learning;
 private final FlashcardCatalog catalog;
 public FlashcardController(LearningService learning,FlashcardCatalog catalog){this.learning=learning;this.catalog=catalog;}
 private String key(Student student,Long lesson){return "flashcards.v1."+student.getId()+"."+lesson;}
 private int cursor(HttpSession session,String key){var value=session.getAttribute(key);return value instanceof Integer i?i:0;}
 @GetMapping("/lessons/{id}") String lesson(Principal principal,@PathVariable Long id,Model model,HttpSession session){
  var student=learning.student(principal.getName());
  if(!student.isPlacementCompleted())return "redirect:/exams/PLACEMENT";
  var lesson=learning.lesson(student,id);
  var cards=catalog.cards(lesson);
  model.addAttribute("student",student);model.addAttribute("lesson",lesson);model.addAttribute("cards",cards);
  model.addAttribute("done",learning.completed(student).contains(id));
  model.addAttribute("studied",Math.min(cursor(session,key(student,id)),cards.size()));
  return "lesson";
 }
 @PostMapping("/lessons/{id}/cards/{index}/seen") @ResponseBody
 Map<String,Object> seen(Principal principal,@PathVariable Long id,@PathVariable int index,HttpSession session){
  var student=learning.student(principal.getName());
  var lesson=learning.lesson(student,id);var cards=catalog.cards(lesson);
  String key=key(student,id);int current=cursor(session,key);
  if(index<0 || index>=cards.size() || index>current)
   throw new ResponseStatusException(HttpStatus.CONFLICT,"Hãy học lần lượt từng thẻ.");
  int studied=Math.max(current,index+1);
  // Completion and XP are committed before acknowledging the last card.
  if(studied>=cards.size())learning.complete(principal.getName(),id);
  session.setAttribute(key,studied);
  return Map.of("studied",studied,"complete",studied>=cards.size());
 }
 @PostMapping("/lessons/{id}/complete") String complete(Principal principal,@PathVariable Long id,HttpSession session){
  var student=learning.student(principal.getName());var lesson=learning.lesson(student,id);
  if(cursor(session,key(student,id))<catalog.cards(lesson).size())
   throw new ResponseStatusException(HttpStatus.CONFLICT,"Hãy học hết các thẻ trước khi hoàn thành.");
  learning.complete(principal.getName(),id);return "redirect:/dashboard";
 }
}
