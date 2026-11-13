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
 private org.springframework.security.web.AuthenticationEntryPoint chatEntryPoint(){
  var entries=new java.util.LinkedHashMap<org.springframework.security.web.util.matcher.RequestMatcher,org.springframework.security.web.AuthenticationEntryPoint>();
  entries.put(new org.springframework.security.web.util.matcher.AntPathRequestMatcher("/api/chat/**"),new org.springframework.security.web.authentication.HttpStatusEntryPoint(org.springframework.http.HttpStatus.UNAUTHORIZED));
  var entry=new org.springframework.security.web.authentication.DelegatingAuthenticationEntryPoint(entries);
  entry.setDefaultEntryPoint(new org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint("/login"));
  return entry;
 }
 @Bean SecurityFilterChain security(HttpSecurity http) throws Exception {
  return http.authorizeHttpRequests(a -> a.requestMatchers("/","/login","/register","/register/**","/css/**","/js/**","/images/**","/fonts/**","/error").permitAll().requestMatchers("/instructor/**").hasRole("INSTRUCTOR").anyRequest().authenticated())
   .exceptionHandling(e->e.authenticationEntryPoint(chatEntryPoint()))
   .formLogin(f -> f.loginPage("/login").defaultSuccessUrl("/start",true).permitAll())
   .logout(l -> l.logoutSuccessUrl("/?logout")).build();
 }
}
