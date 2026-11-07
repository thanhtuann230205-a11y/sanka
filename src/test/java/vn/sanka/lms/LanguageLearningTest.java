package vn.sanka.lms;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import vn.sanka.lms.auth.*;
import vn.sanka.lms.learning.*;
import vn.sanka.lms.assessment.*;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.hamcrest.Matchers.containsString;

@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test")
class LanguageLearningTest {
 @Autowired MockMvc mvc;
 @Autowired StudentRepository students;
 @Autowired LearningService learning;
 @Autowired WorkflowService workflow;
 @Autowired LearningCatalog catalog;
 @Autowired AttemptRepository attempts;

 Map<String,String> answers(Stage stage,Level target){
  Map<String,String> map=new HashMap<>();
  catalog.questions(stage,null).forEach(q->{
   Level group=Level.valueOf(q.id().substring(0,2).toUpperCase(Locale.ROOT));
   map.put("answer_"+q.id(),""+(group.ordinal()<=target.ordinal()?q.correct():(q.correct()+1)%q.options().size()));
  });return map;
 }
 Student student(String name){return students.save(new Student(name,name+"@sflow.example","unused"));}

 @ParameterizedTest @EnumSource(Level.class)
 void placementBeforeMajorRoutesEveryLevel(Level level) throws Exception {
  var s=student("s_route_"+level);
  mvc.perform(get("/start").with(user(s.getEmail()))).andExpect(redirectedUrl("/exams/PLACEMENT"));
  mvc.perform(get("/onboarding").with(user(s.getEmail()))).andExpect(redirectedUrl("/exams/PLACEMENT"));
  mvc.perform(post("/onboarding").with(user(s.getEmail())).with(csrf()).param("major","IT")).andExpect(status().isForbidden());
  mvc.perform(get("/exams/PLACEMENT").with(user(s.getEmail()))).andExpect(status().isOk());
  var attempt=workflow.submitExam(s.getEmail(),Stage.PLACEMENT,answers(Stage.PLACEMENT,level));
  assertThat(attempt.getMajor()).isNull();assertThat(attempt.getAssignedLevel()).isEqualTo(level);
  mvc.perform(get("/results/"+attempt.getId()).with(user(s.getEmail()))).andExpect(status().isOk());
  mvc.perform(get("/results/"+attempt.getId()+"/review").with(user(s.getEmail()))).andExpect(status().isOk());
  mvc.perform(get("/competency").with(user(s.getEmail()))).andExpect(status().isOk());
  s=learning.student(s.getEmail());assertThat(s.getMajor()).isNull();
  if(level.ordinal()>=Level.N4.ordinal()){
   mvc.perform(get("/start").with(user(s.getEmail()))).andExpect(redirectedUrl("/onboarding"));
   mvc.perform(get("/onboarding").with(user(s.getEmail()))).andExpect(status().isOk());
   mvc.perform(post("/onboarding").with(user(s.getEmail())).with(csrf()).param("major","IT")).andExpect(redirectedUrl("/start"));
   mvc.perform(get("/specialized/TERMS").with(user(s.getEmail()))).andExpect(status().isOk());
  }else{
   mvc.perform(get("/start").with(user(s.getEmail()))).andExpect(redirectedUrl("/dashboard"));
   mvc.perform(get("/onboarding").with(user(s.getEmail()))).andExpect(redirectedUrl("/dashboard"));
   mvc.perform(post("/onboarding").with(user(s.getEmail())).with(csrf()).param("major","IT")).andExpect(status().isForbidden());
   mvc.perform(get("/specialized/TERMS").with(user(s.getEmail()))).andExpect(status().isForbidden());
   assertThat(learning.currentCourse(s)).isNotEmpty().allMatch(l->l.getMajor()==null && l.getLevel().ordinal()<=Level.N5.ordinal());
  }
  mvc.perform(get("/dashboard").with(user(s.getEmail()))).andExpect(status().isOk());
  mvc.perform(get("/journey").with(user(s.getEmail()))).andExpect(status().isOk());
 }

 @ParameterizedTest @EnumSource(value=Level.class,names={"ZERO","N5"})
 void foundationRequiresS5ThenS4BeforeChoosingMajor(Level initial) throws Exception {
  var s=student("foundation_"+initial);
  workflow.submitExam(s.getEmail(),Stage.PLACEMENT,answers(Stage.PLACEMENT,initial));
  s=learning.student(s.getEmail());
  mvc.perform(get("/exams/N5").with(user(s.getEmail()))).andExpect(status().isForbidden());
  mvc.perform(get("/exams/N4").with(user(s.getEmail()))).andExpect(status().isForbidden());
  for(var lesson:learning.currentCourse(s))learning.complete(s.getEmail(),lesson.getId());
  var failed=workflow.submitExam(s.getEmail(),Stage.N5,answers(Stage.N5,Level.ZERO));
  assertThat(failed.isPassed()).isFalse();assertThat(learning.s4Foundation(learning.student(s.getEmail()))).isFalse();
  var s5Result=workflow.submitExam(s.getEmail(),Stage.N5,answers(Stage.N5,Level.N5));
  mvc.perform(get("/results/"+s5Result.getId()).with(user(s.getEmail()))).andExpect(status().isOk());
  s=learning.student(s.getEmail());assertThat(learning.s4Foundation(s)).isTrue();
  assertThat(learning.currentCourse(s)).isNotEmpty().allMatch(l->l.getMajor()==null&&l.getLevel()==Level.N4);
  mvc.perform(post("/onboarding").with(user(s.getEmail())).with(csrf()).param("major","IT")).andExpect(status().isForbidden());
  for(var lesson:learning.currentCourse(s))learning.complete(s.getEmail(),lesson.getId());
  var s4Result=workflow.submitExam(s.getEmail(),Stage.N4,answers(Stage.N4,Level.N4));
  mvc.perform(get("/results/"+s4Result.getId()).with(user(s.getEmail()))).andExpect(status().isOk());
  mvc.perform(get("/start").with(user(s.getEmail()))).andExpect(redirectedUrl("/onboarding"));
  mvc.perform(post("/onboarding").with(user(s.getEmail())).with(csrf()).param("major","IT")).andExpect(redirectedUrl("/start"));
  mvc.perform(get("/dashboard").with(user(s.getEmail()))).andExpect(status().isOk());
  assertThat(attempts.findByStudentIdOrderByCreatedAtDesc(s.getId())).allMatch(a->a.getMajor()==null);
 }

 @Test void retiredLevelModuleCannotBeUsedViaDirectUrls() throws Exception {
  var s=student("retired_module");workflow.submitExam(s.getEmail(),Stage.PLACEMENT,answers(Stage.PLACEMENT,Level.N4));
  learning.onboard(s.getEmail(),Major.IT,Level.ZERO);
  mvc.perform(get("/language").with(user(s.getEmail()))).andExpect(redirectedUrl("/start"));
  for(Level level:Level.values()){
   mvc.perform(get("/language/"+level).with(user(s.getEmail()))).andExpect(status().isForbidden());
   mvc.perform(post("/language/"+level+"/enroll").with(user(s.getEmail())).with(csrf())).andExpect(status().isForbidden());
  }
  for(Stage stage:List.of(Stage.N3,Stage.N2))mvc.perform(get("/exams/"+stage).with(user(s.getEmail()))).andExpect(status().isForbidden());
  var page=mvc.perform(get("/dashboard").with(user(s.getEmail()))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
  assertThat(page).doesNotContain("Tiếng Nhật theo cấp", "Mở các cấp học", "href=\"/language\"");
 }
}
