package vn.sanka.lms;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import vn.sanka.lms.learning.*;
import vn.sanka.lms.auth.*;
import vn.sanka.lms.assessment.*;
import vn.sanka.lms.language.*;
import java.time.*;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;

@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test")
class LanguageLearningTest {
 @Autowired MockMvc mvc;
 @Autowired StudentRepository students;
 @Autowired LearningService learning;
 @Autowired WorkflowService workflow;
 @Autowired LearningCatalog exams;
 @Autowired LanguageService language;
 @Autowired LanguageCatalog content;
 @Autowired LanguageEnrollmentRepository enrollments;
 @Autowired PasswordEncoder encoder;
 @Autowired Clock clock;
 Map<String,String> answers(List<LearningCatalog.Question> questions,boolean correct){
  var map=new HashMap<String,String>();questions.forEach(q->map.put("answer_"+q.id(),String.valueOf(correct?q.correct():(q.correct()+1)%q.options().size())));return map;
 }
 Map<String,String> placement(Level target){
  var map=new HashMap<String,String>();
  exams.questions(Stage.PLACEMENT,Major.IT).forEach(q->{var group=Level.valueOf(q.id().substring(0,2).toUpperCase(Locale.ROOT));
   map.put("answer_"+q.id(),String.valueOf(group.ordinal()<=target.ordinal()?q.correct():(q.correct()+1)%q.options().size()));});return map;
 }
 Student student(String name,Level level){
  var s=students.save(new Student(name,name+"@language.example",encoder.encode("password123")));
  learning.onboard(s.getEmail(),Major.IT,Level.ZERO);workflow.submitExam(s.getEmail(),Stage.PLACEMENT,placement(level));return learning.student(s.getEmail());
 }
 @ParameterizedTest @EnumSource(Level.class)
 void eachLevelHasCompleteLearningAndPeriodicFlow(Level level) throws Exception {
  var s=student("language_"+level,level);assertThat(s.getLevel()).isEqualTo(level);
  assertThat(content.course(level).words()).isNotEmpty();assertThat(content.course(level).grammar()).isNotEmpty();
  assertThat(language.lessons(s,level)).isNotEmpty();
  mvc.perform(get("/language").with(user(s.getEmail()))).andExpect(status().isOk());
  mvc.perform(get("/language/"+level).with(user(s.getEmail()))).andExpect(status().isOk());
  mvc.perform(get("/language/"+level+"/review").with(user(s.getEmail()))).andExpect(status().isForbidden());
  for(int i=0;i<2;i++)mvc.perform(post("/language/"+level+"/enroll").with(user(s.getEmail())).with(csrf())).andExpect(redirectedUrl("/language/"+level));
  assertThat(enrollments.findByStudentId(s.getId())).hasSize(1);
  var enrollment=language.enrollment(s,level);assertThat(enrollment.getNextReviewAt()).isEqualTo(LocalDate.now(clock).plusDays(7));
  enrollment.reviewed(LocalDate.now(clock).minusDays(8));enrollments.save(enrollment);
  mvc.perform(get("/language/"+level).with(user(s.getEmail()))).andExpect(status().isOk()).andExpect(model().attribute("due",true));
  mvc.perform(get("/language/"+level+"/review").with(user(s.getEmail()))).andExpect(status().isOk());
  var request=post("/language/"+level+"/review").with(user(s.getEmail())).with(csrf());
  answers(exams.questionsForLevel(level,Major.IT),true).forEach(request::param);
  var result=mvc.perform(request).andExpect(status().is3xxRedirection()).andReturn();
  mvc.perform(get(result.getResponse().getRedirectedUrl()).with(user(s.getEmail()))).andExpect(status().isOk());
  assertThat(language.history(s)).hasSize(1);assertThat(language.history(s).getFirst().getScore()).isEqualTo(100);
  assertThat(language.enrollment(s,level).getNextReviewAt()).isEqualTo(LocalDate.now(clock).plusDays(7));
  assertThat(learning.student(s.getEmail()).getLevel()).isEqualTo(level);
  String work="確認した情報に基づいて、担当者に状況と次の行動を報告します。".repeat(8);
  mvc.perform(post("/language/"+level+"/application").with(user(s.getEmail())).with(csrf()).param("response",work)).andExpect(redirectedUrl("/language/history"));
  mvc.perform(post("/language/"+level+"/application").with(user(s.getEmail())).with(csrf()).param("response",work)).andExpect(status().isConflict());
  mvc.perform(get("/language/"+level).with(user(s.getEmail()))).andExpect(status().isOk()).andExpect(model().attribute("pending",true));
  mvc.perform(get("/language/history").with(user(s.getEmail()))).andExpect(status().isOk());
  var clip=content.course(level).audioId();
  var audio=mvc.perform(get("/audio/"+clip+".wav").with(user(s.getEmail()))).andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray();
  assertThat(audio.length).isGreaterThan(10000);assertThat(new String(audio,0,4,java.nio.charset.StandardCharsets.US_ASCII)).isEqualTo("RIFF");
 }
 @Test void advanceFromN4ThroughN3AndCombinedUpperGroup() throws Exception {
  var s=student("language_promotion",Level.N4);
  mvc.perform(get("/language/N2").with(user(s.getEmail()))).andExpect(status().isForbidden());
  mvc.perform(get("/exams/N3").with(user(s.getEmail()))).andExpect(status().isForbidden());
  for(var lesson:language.lessons(s,Level.N3))learning.complete(s.getEmail(),lesson.getId());
  mvc.perform(get("/exams/N3").with(user(s.getEmail()))).andExpect(status().isOk());
  workflow.submitExam(s.getEmail(),Stage.N3,answers(exams.questions(Stage.N3,Major.IT),true));
  s=learning.student(s.getEmail());assertThat(s.getLevel()).isEqualTo(Level.N3);
  for(var lesson:language.lessons(s,Level.N2))learning.complete(s.getEmail(),lesson.getId());
  workflow.submitExam(s.getEmail(),Stage.N2,answers(exams.questions(Stage.N2,Major.IT),true));
  s=learning.student(s.getEmail());assertThat(s.getLevel()).isEqualTo(Level.N2);
  workflow.submitExam(s.getEmail(),Stage.N3,answers(exams.questions(Stage.N3,Major.IT),false));
  assertThat(learning.student(s.getEmail()).getLevel()).isEqualTo(Level.N2);
 }
 @Test void isolatedReviewResultsAndManualApplicationAssessment() throws Exception {
  var s=student("language_owner",Level.N3);var other=student("language_other",Level.ZERO);
  language.enroll(s.getEmail(),Level.N3);var result=language.review(s.getEmail(),Level.N3,answers(exams.questionsForLevel(Level.N3,Major.IT),true));
  mvc.perform(get("/language/results/"+result.getId()).with(user(other.getEmail()))).andExpect(status().isNotFound());
  mvc.perform(post("/language/N3/enroll").with(user(other.getEmail())).with(csrf())).andExpect(status().isForbidden());
  mvc.perform(post("/language/ZERO/enroll").with(user(other.getEmail()))).andExpect(status().isForbidden());
  mvc.perform(get("/audio/n3.wav")).andExpect(status().is3xxRedirection());
  String work="変更内容、確認結果と期限を報告いたします。未確認の部分については引き続き調査します。".repeat(5);
  language.submit(s.getEmail(),Level.N3,work);var submission=language.work(s).getFirst();
  var teacher=students.save(new Student("Language Teacher","language_teacher@language.example",encoder.encode("password123")));
  teacher.setRole("INSTRUCTOR");students.save(teacher);
  mvc.perform(get("/instructor/language-reviews").with(user(s.getEmail()))).andExpect(status().isForbidden());
  mvc.perform(get("/instructor/language-reviews").with(user(teacher.getEmail()).roles("INSTRUCTOR"))).andExpect(status().isOk());
  mvc.perform(post("/instructor/language-reviews/"+submission.getId()).with(user(teacher.getEmail()).roles("INSTRUCTOR")).with(csrf())
   .param("language","4").param("technical","2").param("communication","4").param("task","4").param("feedback","Bổ sung dữ kiện và tách giả định khỏi thông tin đã xác nhận.")).andExpect(redirectedUrl("/instructor/language-reviews"));
  assertThat(language.work(s).getFirst().getStatus()).isEqualTo("REVISION");
  mvc.perform(get("/language/N3").with(user(s.getEmail()))).andExpect(status().isOk()).andExpect(model().attribute("responseText",work));
  mvc.perform(get("/language/history").with(user(s.getEmail()))).andExpect(status().isOk());
  language.submit(s.getEmail(),Level.N3,work+"確認した事実を追加しました。");
  language.grade(teacher.getEmail(),language.work(s).getFirst().getId(),4,4,4,4,"Đã bổ sung đúng dữ kiện. Tiếp tục luyện cách trình bày thời hạn và trách nhiệm.");
  assertThat(language.work(s).getFirst().getStatus()).isEqualTo("PASSED");
  assertThat(language.work(other)).isEmpty();assertThat(learning.student(s.getEmail()).getLevel()).isEqualTo(Level.N3);
 }
 @Test void placementNeedsConsecutiveGroupsAndContainsAllSkills() {
  var bank=exams.questions(Stage.PLACEMENT,Major.IT);assertThat(bank).hasSize(20);
  assertThat(bank.stream().map(LearningCatalog.Question::skill).distinct()).hasSize(5);
  assertThat(bank.stream().filter(q->q.audioId()!=null)).hasSize(4);
  var s=student("placement_no_skip",Level.ZERO);
  // A separate new account gets advanced questions right but fails N5: no level skipping.
  var newStudent=students.save(new Student("No skip","no_skip@language.example",encoder.encode("password123")));
  learning.onboard(newStudent.getEmail(),Major.IT,Level.ZERO);
  var choices=answers(bank,true);answers(exams.questions(Stage.N5,Major.IT),false).forEach(choices::put);
  workflow.submitExam(newStudent.getEmail(),Stage.PLACEMENT,choices);
  assertThat(learning.student(newStudent.getEmail()).getLevel()).isEqualTo(Level.ZERO);
 }
}
