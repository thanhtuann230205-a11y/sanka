package vn.sanka.lms.auth;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import jakarta.servlet.http.Cookie;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;

@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test")
class EmailRegistrationTest {
 @Autowired MockMvc mvc;
 @Autowired StudentRepository students;
 @Autowired EmailRegistrationRepository pending;
 @Autowired EmailRegistrationService service;
 @Autowired PasswordEncoder encoder;
 @MockitoBean VerificationMailer mailer;
 final AtomicReference<String> delivered=new AtomicReference<>();
 @BeforeEach void capture(){doAnswer(inv->{delivered.set(inv.getArgument(1));return null;}).when(mailer).send(anyString(),anyString());}
 private Cookie begin(String email) throws Exception {
  var response=mvc.perform(post("/register").with(csrf()).param("name","New learner").param("email",email).param("password","password123"))
   .andExpect(redirectedUrl("/register/verify")).andReturn().getResponse();
  assertThat(students.findByEmail(email)).isEmpty();assertThat(delivered.get()).matches("[0-9]{6}");
  assertThat(pending.findById(email).orElseThrow().codeHash).isNotEqualTo(delivered.get());
  var cookie=response.getCookie("SANKA_SESSION");assertThat(cookie).isNotNull();return cookie;
 }
 @Test void onlyCorrectCodeCreatesAccountAndCodeCannotBeReused() throws Exception {
  String email="verified-"+UUID.randomUUID()+"@gmail.com";var cookie=begin(email);
  mvc.perform(get("/register/verify").cookie(cookie)).andExpect(status().isOk()).andExpect(model().attribute("email",email));
  String wrong=delivered.get().equals("000000")?"111111":"000000";
  mvc.perform(post("/register/verify").cookie(cookie).with(csrf()).param("code",wrong))
   .andExpect(redirectedUrl("/register/verify")).andExpect(flash().attributeExists("error"));
  assertThat(students.findByEmail(email)).isEmpty();assertThat(pending.findById(email).orElseThrow().attempts).isEqualTo(1);
  mvc.perform(post("/register/verify").cookie(cookie).with(csrf()).param("code",delivered.get())).andExpect(redirectedUrl("/login?registered"));
  assertThat(encoder.matches("password123",students.findByEmail(email).orElseThrow().getPassword())).isTrue();
  assertThat(pending.findById(email)).isEmpty();
  mvc.perform(post("/register/verify").cookie(cookie).with(csrf()).param("code",delivered.get())).andExpect(redirectedUrl("/register"));
 }
 @Test void expiresLocksAndResendCooldownIsEnforced() throws Exception {
  String email="locked-"+UUID.randomUUID()+"@gmail.com";var cookie=begin(email);
  mvc.perform(post("/register/resend").cookie(cookie).with(csrf())).andExpect(redirectedUrl("/register/verify"))
   .andExpect(flash().attributeExists("error"));verify(mailer,times(1)).send(eq(email),anyString());
  var entry=pending.findById(email).orElseThrow();String token=entry.token;
  String wrong=delivered.get().equals("000000")?"111111":"000000";
  for(int i=0;i<5;i++)service.verify(token,wrong);
  assertThat(service.verify(token,delivered.get())).isEqualTo(EmailRegistrationService.Result.LOCKED);
  entry=pending.findById(email).orElseThrow();entry.sentAt=LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")).minusMinutes(11);entry.expiresAt=entry.sentAt.plusMinutes(10);pending.saveAndFlush(entry);
  assertThat(service.verify(token,delivered.get())).isEqualTo(EmailRegistrationService.Result.EXPIRED);
  service.resend(token);assertThat(service.verify(token,delivered.get())).isEqualTo(EmailRegistrationService.Result.SUCCESS);
 }
 @Test void sendingFailureCreatesNeitherAccountNorPendingRequest() throws Exception {
  doThrow(new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE,"Không gửi được mã"))
   .when(mailer).send(anyString(),anyString());
  String email="send-failure-"+UUID.randomUUID()+"@gmail.com";
  mvc.perform(post("/register").with(csrf()).param("name","Learner").param("email",email).param("password","password123"))
   .andExpect(status().isOk()).andExpect(model().attributeHasErrors("registration"));
  assertThat(students.findByEmail(email)).isEmpty();assertThat(pending.findById(email)).isEmpty();
 }
 @Test void cannotVerifyWithoutSessionOrCsrf() throws Exception {
  mvc.perform(post("/register/verify").param("code","123456")).andExpect(status().isForbidden());
  mvc.perform(post("/register/verify").with(csrf()).param("code","123456")).andExpect(redirectedUrl("/register"));
 }
}
