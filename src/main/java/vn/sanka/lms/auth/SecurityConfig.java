package vn.sanka.lms.auth;
import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
@Configuration
public class SecurityConfig {
 @Bean PasswordEncoder passwordEncoder(){return new BCryptPasswordEncoder();}
 @Bean UserDetailsService users(StudentRepository students){return email -> students.findByEmail(email.toLowerCase(java.util.Locale.ROOT)).map(s -> User.withUsername(s.getEmail()).password(s.getPassword()).roles(s.getRole()).build()).orElseThrow(() -> new UsernameNotFoundException("Không tìm thấy tài khoản"));}
 @Bean SecurityFilterChain security(HttpSecurity http) throws Exception {
  return http.authorizeHttpRequests(a -> a.requestMatchers("/","/login","/register","/css/**","/js/**","/images/**","/error").permitAll().requestMatchers("/instructor/**").hasRole("INSTRUCTOR").anyRequest().authenticated())
   .formLogin(f -> f.loginPage("/login").defaultSuccessUrl("/start",true).permitAll())
   .logout(l -> l.logoutSuccessUrl("/?logout")).build();
 }
}
