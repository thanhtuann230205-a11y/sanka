package vn.sanka.lms;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import vn.sanka.lms.auth.*;
import vn.sanka.lms.learning.*;
import vn.sanka.lms.assessment.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.assertj.core.api.Assertions.*;
@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test")
class LearningFlowTest {
 @Autowired MockMvc mvc; @Autowired StudentRepository students; @Autowired LearningService learning;
 @Autowired WorkflowService workflow; @Autowired LearningCatalog catalog;
 @Test void publicPagesAndSecurity() throws Exception {
  for(String page:new String[]{"/","/login","/register"})mvc.perform(get(page)).andExpect(status().isOk());
  mvc.perform(get("/images/japan-hero-v2.jpg")).andExpect(status().isOk()).andExpect(content().contentType("image/jpeg"));
  mvc.perform(get("/dashboard")).andExpect(status().is3xxRedirection());
  mvc.perform(post("/register")).andExpect(status().isForbidden());
 }
 @Test void registrationLearningAndIsolation() throws Exception {
  mvc.perform(post("/register").with(csrf()).param("name","An").param("email","an@example.com").param("password","password123")).andExpect(redirectedUrl("/login?registered"));
  var student=students.findByEmail("an@example.com").orElseThrow();assertThat(student.getPassword()).startsWith("$2a$");
  mvc.perform(post("/login").with(csrf()).param("username","an@example.com").param("password","password123")).andExpect(redirectedUrl("/start"));
  mvc.perform(post("/register").with(csrf()).param("name","An").param("email","an@example.com").param("password","password123")).andExpect(status().isOk()).andExpect(model().attributeHasFieldErrors("registration","email"));
  mvc.perform(post("/register").with(csrf()).param("name","An").param("email","unicode@example.com").param("password","あ".repeat(30))).andExpect(status().isOk()).andExpect(model().attributeHasFieldErrors("registration","password"));
  mvc.perform(get("/dashboard").with(user(student.getEmail()))).andExpect(redirectedUrl("/exams/PLACEMENT"));
  mvc.perform(get("/onboarding").with(user(student.getEmail()))).andExpect(redirectedUrl("/exams/PLACEMENT"));
  mvc.perform(post("/onboarding").with(user(student.getEmail())).with(csrf()).param("major","IT").param("level","N4")).andExpect(status().isForbidden());
  mvc.perform(get("/start").with(user(student.getEmail()))).andExpect(redirectedUrl("/exams/PLACEMENT"));
  assertThat(learning.student(student.getEmail()).getLevel()).isNull();
  var answers=new java.util.HashMap<String,String>();
  catalog.questions(Stage.PLACEMENT,Major.IT).forEach(q->answers.put("answer_"+q.id(),String.valueOf(q.id().startsWith("n3")||q.id().startsWith("n2")?(q.correct()+1)%q.options().size():q.correct())));
  workflow.submitExam(student.getEmail(),Stage.PLACEMENT,answers);
  learning.onboard(student.getEmail(),Major.IT,Level.ZERO);student=learning.student(student.getEmail());
  for(String page:new String[]{"/dashboard","/path","/lessons/9","/dictionary?q=不具合"})mvc.perform(get(page).with(user(student.getEmail()))).andExpect(status().isOk());
  mvc.perform(get("/lessons/5").with(user(student.getEmail()))).andExpect(status().isNotFound());
  for(int i=0;i<2;i++)mvc.perform(post("/lessons/9/complete").with(user(student.getEmail())).with(csrf())).andExpect(redirectedUrl("/dashboard"));
  assertThat(learning.completed(student)).containsExactly(9L);
  mvc.perform(post("/register").with(csrf()).param("name","Binh").param("email","binh@example.com").param("password","password123")).andExpect(status().is3xxRedirection());
  var other=students.findByEmail("binh@example.com").orElseThrow(); assertThat(learning.completed(other)).isEmpty();
  
  mvc.perform(get("/lessons/9").with(user(other.getEmail()))).andExpect(redirectedUrl("/exams/PLACEMENT"));
 }
}
