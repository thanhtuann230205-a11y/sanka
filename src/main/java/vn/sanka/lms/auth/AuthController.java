package vn.sanka.lms.auth;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.dao.DataIntegrityViolationException;
@Controller
public class AuthController {
 private static final org.slf4j.Logger log=org.slf4j.LoggerFactory.getLogger(AuthController.class);
 private final StudentRepository students; private final PasswordEncoder encoder; private final EmailRegistrationService verification;
 public AuthController(StudentRepository students,PasswordEncoder encoder,EmailRegistrationService verification){this.students=students;this.encoder=encoder;this.verification=verification;}
 public record Registration(@NotBlank @Size(max=80) String name,@NotBlank @Email @Size(max=190) String email,@NotBlank @Size(min=8,max=72) String password){}
 @GetMapping("/") String home(){return "index";}
 @GetMapping("/login") String login(){return "login";}
 @GetMapping("/register") String register(Model model){model.addAttribute("registration",new Registration("","",""));return "register";}
 @PostMapping("/register") String register(@Valid @ModelAttribute("registration") Registration form,BindingResult errors,jakarta.servlet.http.HttpSession session){
  if(form.password()!=null && form.password().getBytes(java.nio.charset.StandardCharsets.UTF_8).length>72)
   errors.rejectValue("password","length","Mật khẩu tối đa 72 byte UTF-8; hãy dùng mật khẩu ngắn hơn.");
  if(errors.hasErrors())return "register";
  String email=form.email().strip().toLowerCase(java.util.Locale.ROOT);
  if(students.findByEmail(email).isPresent()){errors.rejectValue("email","duplicate","Email đã được đăng ký");return "register";}
  try{session.setAttribute("registration.token",verification.start(form.name(),email,form.password()));}
  catch(org.springframework.web.server.ResponseStatusException e){errors.reject("registration.email",e.getReason());return "register";}
  catch(DataIntegrityViolationException e){
   // A constraint failure can concern another column or the generated ID, not just email.
   if(students.findByEmail(email).isPresent()){
    errors.rejectValue("email","duplicate","Email đã được đăng ký");
   }else{
    log.error("Registration failed while saving a new student",e);
    errors.reject("registration.failed","Chưa thể tạo tài khoản do lỗi lưu dữ liệu. Vui lòng thử lại sau.");
   }
   return "register";
  }
  return "redirect:/register/verify";
 }

 @GetMapping("/register/verify") String verifyPage(jakarta.servlet.http.HttpSession session,Model model){
  String token=(String)session.getAttribute("registration.token");String email=token==null?null:verification.email(token);
  if(email==null)return "redirect:/register";model.addAttribute("email",email);return "verify-email";
 }
 @PostMapping("/register/verify") String verify(@RequestParam(defaultValue="") String code,jakarta.servlet.http.HttpSession session,org.springframework.web.servlet.mvc.support.RedirectAttributes flash){
  String token=(String)session.getAttribute("registration.token");if(token==null)return "redirect:/register";
  var result=verification.verify(token,code);
  if(result==EmailRegistrationService.Result.SUCCESS){session.removeAttribute("registration.token");return "redirect:/login?registered";}
  flash.addFlashAttribute("error",switch(result){
   case INVALID -> "Mã xác nhận chưa đúng. Vui lòng thử lại.";
   case LOCKED -> "Đã nhập sai 5 lần. Hãy yêu cầu mã mới.";
   case DUPLICATE -> "Email đã được đăng ký. Hãy đăng nhập.";
   default -> "Mã đã hết hạn. Hãy gửi lại mã hoặc đăng ký lại.";
  });return "redirect:/register/verify";
 }
 @PostMapping("/register/resend") String resend(jakarta.servlet.http.HttpSession session,org.springframework.web.servlet.mvc.support.RedirectAttributes flash){
  String token=(String)session.getAttribute("registration.token");if(token==null)return "redirect:/register";
  try{verification.resend(token);flash.addFlashAttribute("message","Đã gửi mã mới. Hãy kiểm tra hộp thư và thư rác.");}
  catch(org.springframework.web.server.ResponseStatusException e){flash.addFlashAttribute("error",e.getReason());}
  return "redirect:/register/verify";
 }
}
