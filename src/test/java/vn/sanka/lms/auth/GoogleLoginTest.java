package vn.sanka.lms.auth;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.core.oidc.*;
import org.springframework.security.oauth2.core.oidc.user.*;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import java.time.Instant;
import java.util.*;
import java.io.*;
import vn.sanka.lms.learning.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;

@SpringBootTest(properties={"sanka.google.client-id=test-client","sanka.google.client-secret=test-secret"})
@AutoConfigureMockMvc @ActiveProfiles("test")
class GoogleLoginTest {
 @Autowired MockMvc mvc;
 @Autowired GoogleAccountService accounts;
 @Autowired StudentRepository students;
 @Autowired PasswordEncoder encoder;
 @Test void buttonsAndAuthorizationRedirectWorkWithoutCallingGoogle() throws Exception {
  for(String page:List.of("/login","/register"))mvc.perform(get(page)).andExpect(status().isOk())
   .andExpect(content().string(org.hamcrest.Matchers.containsString("Tiếp tục với Google")));
  mvc.perform(get("/auth/google")).andExpect(redirectedUrl("/oauth2/authorization/google"));
  mvc.perform(get("/oauth2/authorization/google"))
   .andExpect(status().is3xxRedirection()).andExpect(header().string("Location",org.hamcrest.Matchers.startsWith("https://accounts.google.com/")));
  // A forged callback state must fail before any token exchange.
  mvc.perform(get("/login/oauth2/code/google").param("code","fake-code").param("state","forged"))
   .andExpect(redirectedUrl("/login?googleError"));
 }
 @Test void newGoogleAccountCanFollowExistingLearningFlowAndSerializePrincipal() throws Exception {
  String subject=UUID.randomUUID().toString(),email="google-"+UUID.randomUUID()+"@gmail.com";
  var student=accounts.resolve(subject,email,true,null,"Google Learner");
  assertThat(student.getRole()).isEqualTo("STUDENT");assertThat(student.getGoogleSubject()).isEqualTo(subject);
  assertThat(accounts.resolve(subject,"changed@gmail.com",true,null,"New name").getId()).isEqualTo(student.getId());
  var token=new OidcIdToken("test-id-token",Instant.now(),Instant.now().plusSeconds(3600),Map.of("sub",subject,"email",email,"email_verified",true));
  var google=new DefaultOidcUser(List.of(new SimpleGrantedAuthority("ROLE_INSTRUCTOR")),token);
  var principal=new GoogleOidcUserService.SankaGoogleUser(google,student.getEmail(),student.getRole());
  assertThat(principal.getName()).isEqualTo(email);
  assertThat(principal.getAuthorities()).extracting("authority").containsExactly("ROLE_STUDENT");
  mvc.perform(get("/start").with(oidcLogin().oidcUser(principal))).andExpect(redirectedUrl("/exams/PLACEMENT"));
  var bytes=new ByteArrayOutputStream();try(var out=new ObjectOutputStream(bytes)){out.writeObject(principal);}
  try(var in=new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))){assertThat(((OidcUser)in.readObject()).getName()).isEqualTo(email);}
 }
 @Test void linksVerifiedExistingAccountWithoutChangingPasswordProgressOrRole(){
  String email="google-link-"+UUID.randomUUID()+"@gmail.com",hash=encoder.encode("password123");
  var local=new Student("Existing learner",email,hash);local.assess(Level.N4);local.onboard(Major.IT,Level.N4);local.setRole("INSTRUCTOR");students.saveAndFlush(local);
  var linked=accounts.resolve(UUID.randomUUID().toString(),email,true,null,"Changed name");
  assertThat(linked.getId()).isEqualTo(local.getId());assertThat(linked.getRole()).isEqualTo("INSTRUCTOR");
  assertThat(linked.getMajor()).isEqualTo(Major.IT);assertThat(linked.getLevel()).isEqualTo(Level.N4);assertThat(linked.getPassword()).isEqualTo(hash);
  assertThatThrownBy(()->accounts.resolve("different-subject",email,true,null,"Other" )).isInstanceOf(OAuth2AuthenticationException.class);
 }
 @Test void rejectsUnverifiedAndThirdPartyEmailButAcceptsWorkspace(){
  String suffix=UUID.randomUUID().toString();
  assertThatThrownBy(()->accounts.resolve("unverified-"+suffix,"x@gmail.com",false,null,"X")).isInstanceOf(OAuth2AuthenticationException.class);
  assertThatThrownBy(()->accounts.resolve("external-"+suffix,"x@example.test",true,null,"X")).isInstanceOf(OAuth2AuthenticationException.class);
  assertThat(accounts.resolve("workspace-"+suffix,"employee-"+suffix+"@example.test",true,"example.test","Employee").getRole()).isEqualTo("STUDENT");
 }
}
