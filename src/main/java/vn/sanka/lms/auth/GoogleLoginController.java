package vn.sanka.lms.auth;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.core.env.Environment;
@Controller
public class GoogleLoginController {
 private final Environment environment;
 public GoogleLoginController(Environment environment){this.environment=environment;}
 @GetMapping("/auth/google") String google(){return GoogleOAuthConfiguration.enabled(environment)?"redirect:/oauth2/authorization/google":"redirect:/login?googleUnavailable";}
}
