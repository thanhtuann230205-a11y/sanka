package vn.sanka.lms.chat;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import java.util.Map;

@RestController
@RequestMapping("/api/chat")
public class ChatController {
 private final GeminiChatService chat;
 public ChatController(GeminiChatService chat){this.chat=chat;}
 @GetMapping("/status") public ResponseEntity<?> status(CsrfToken token){
  return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(Map.of("enabled",chat.enabled(),"csrfToken",token.getToken(),"csrfHeader",token.getHeaderName()));
 }
 @PostMapping public Map<String,String> ask(@Valid @RequestBody GeminiChatService.Question question,HttpSession session){
  long minute=System.currentTimeMillis()/60000;
  synchronized(session){
   Long last=(Long)session.getAttribute("chat.minute");
   Integer count=(Integer)session.getAttribute("chat.count");
   int used=last!=null&&last==minute&&count!=null?count:0;
   if(used>=8)throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,"Bạn gửi hơi nhanh. Hãy chờ một phút rồi thử lại.");
   session.setAttribute("chat.minute",minute);session.setAttribute("chat.count",used+1);
  }
  return Map.of("reply",chat.answer(question));
 }
 @ExceptionHandler(ResponseStatusException.class) ResponseEntity<?> error(ResponseStatusException error){
  return ResponseEntity.status(error.getStatusCode()).body(Map.of("message",error.getReason()==null?"Không gửi được tin nhắn.":error.getReason()));
 }
 @ExceptionHandler(MethodArgumentNotValidException.class) ResponseEntity<?> invalid(){
  return ResponseEntity.badRequest().body(Map.of("message","Câu hỏi tối đa 1500 ký tự, lịch sử hội thoại không hợp lệ."));
 }
}
