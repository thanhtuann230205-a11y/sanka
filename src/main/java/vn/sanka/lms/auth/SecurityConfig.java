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
 @Bean SecurityFilterChain security(HttpSecurity http,org.springframework.beans.factory.ObjectProvider<org.springframework.security.oauth2.client.registration.ClientRegistrationRepository> clients,org.springframework.beans.factory.ObjectProvider<GoogleOidcUserService> googleUsers) throws Exception {
  http.authorizeHttpRequests(a -> a.requestMatchers("/","/login","/register","/register/**","/auth/google","/oauth2/**","/login/oauth2/**","/css/**","/js/**","/images/**","/fonts/**","/error").permitAll().requestMatchers("/instructor/**").hasRole("INSTRUCTOR").anyRequest().authenticated())
   .exceptionHandling(e->e.authenticationEntryPoint(chatEntryPoint()))
   .formLogin(f -> f.loginPage("/login").defaultSuccessUrl("/start",true).permitAll())
   .logout(l -> l.logoutSuccessUrl("/?logout"));
  if(clients.getIfAvailable()!=null)http.oauth2Login(o->o.loginPage("/login").clientRegistrationRepository(clients.getObject())
   .userInfoEndpoint(u->u.oidcUserService(googleUsers.getObject())).defaultSuccessUrl("/start",true).failureUrl("/login?googleError"));
  return http.build();
 }
}
