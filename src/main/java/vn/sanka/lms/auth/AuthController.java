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
 private final StudentRepository students; private final PasswordEncoder encoder;
 public AuthController(StudentRepository students,PasswordEncoder encoder){this.students=students;this.encoder=encoder;}
 public record Registration(@NotBlank @Size(max=80) String name,@NotBlank @Email @Size(max=190) String email,@NotBlank @Size(min=8,max=72) String password){}
 @GetMapping("/") String home(){return "index";}
 @GetMapping("/login") String login(){return "login";}
 @GetMapping("/register") String register(Model model){model.addAttribute("registration",new Registration("","",""));return "register";}
 @PostMapping("/register") String register(@Valid @ModelAttribute("registration") Registration form,BindingResult errors){
  if(form.password()!=null && form.password().getBytes(java.nio.charset.StandardCharsets.UTF_8).length>72)
   errors.rejectValue("password","length","Mật khẩu tối đa 72 byte UTF-8; hãy dùng mật khẩu ngắn hơn.");
  if(errors.hasErrors())return "register";
  String email=form.email().strip().toLowerCase(java.util.Locale.ROOT);
  if(students.findByEmail(email).isPresent()){errors.rejectValue("email","duplicate","Email đã được đăng ký");return "register";}
  try{students.saveAndFlush(new Student(form.name().strip(),email,encoder.encode(form.password())));}catch(DataIntegrityViolationException e){errors.rejectValue("email","duplicate","Email đã được đăng ký");return "register";}
  return "redirect:/login?registered";
 }
}
