package vn.sanka.lms;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import vn.sanka.lms.auth.*;
import vn.sanka.lms.learning.*;
import vn.sanka.lms.assessment.*;
import java.util.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test")
class AssessmentWorkflowTest {
    @Autowired MockMvc mvc;
    @Autowired StudentRepository students;
    @Autowired LearningService learning;
    @Autowired WorkflowService workflow;
    @Autowired LearningCatalog catalog;
    @Autowired PasswordEncoder encoder;
    @Autowired DraftRepository drafts;
    @Autowired SpecializationService specialization;

    Student student(String name,Major major) {
        var s=students.save(new Student(name,name+"@test.example",encoder.encode("password123")));
        learning.onboard(s.getEmail(),major,Level.N4);
        return learning.student(s.getEmail());
    }
    Map<String,String> answers(Stage stage, Major major, boolean right) {
        var answers=new HashMap<String,String>();
        catalog.questions(stage,major).forEach(q->{boolean correct=right && !(stage==Stage.PLACEMENT && (q.id().startsWith("n3")||q.id().startsWith("n2")));
            answers.put("answer_"+q.id(),String.valueOf(correct?q.correct():(q.correct()+1)%q.options().size()));});
        return answers;
    }
    void pass(String email,Stage stage) throws Exception {
        var request=post("/exams/"+stage).with(user(email)).with(csrf());
        answers(stage,learning.student(email).getMajor(),true).forEach(request::param);
        mvc.perform(get("/exams/"+stage).with(user(email))).andExpect(status().isOk());
        var result=mvc.perform(request).andExpect(status().is3xxRedirection()).andReturn();
        mvc.perform(get(result.getResponse().getRedirectedUrl()).with(user(email))).andExpect(status().isOk());
    }
    @ParameterizedTest @EnumSource(Major.class)
    void completeAllStagesForEachMajor(Major major) throws Exception {
        var s=student("full_"+major,major);
        mvc.perform(get("/journey").with(user(s.getEmail()))).andExpect(status().isOk());
        mvc.perform(get("/specialized").with(user(s.getEmail()))).andExpect(status().isOk()).andExpect(model().attribute("entered",false));
        mvc.perform(get("/specialized/TERMS").with(user(s.getEmail()))).andExpect(status().isForbidden());
        mvc.perform(post("/exams/TECHNICAL").with(user(s.getEmail())).with(csrf())).andExpect(status().isForbidden());
        pass(s.getEmail(),Stage.PLACEMENT);
        s=learning.student(s.getEmail());assertThat(s.getLevel()).isEqualTo(Level.N4);
        mvc.perform(get("/specialized").with(user(s.getEmail()))).andExpect(status().isOk()).andExpect(model().attribute("entered",true));
        mvc.perform(get("/specialized/TERMS").with(user(s.getEmail()))).andExpect(status().isOk()).andExpect(model().attribute("examAllowed",false));
        var term=specialization.terms(s,false).getFirst();
        assertThat(specialization.saved(s)).isEmpty();
        mvc.perform(post("/specialized/terms/"+term.getId()+"/bookmark").with(user(s.getEmail())).param("saved","true"))
            .andExpect(status().isForbidden());
        for(int i=0;i<2;i++)mvc.perform(post("/specialized/terms/"+term.getId()+"/bookmark").with(user(s.getEmail())).with(csrf()).param("saved","true"))
            .andExpect(redirectedUrl("/specialized/TERMS"));
        assertThat(specialization.saved(s)).containsExactly(term.getId());
        mvc.perform(get("/specialized/TERMS?savedOnly=true").with(user(s.getEmail()))).andExpect(status().isOk());
        long wrongTerm=major==Major.IT?1L:9L;
        mvc.perform(post("/specialized/terms/"+wrongTerm+"/bookmark").with(user(s.getEmail())).with(csrf()).param("saved","true"))
            .andExpect(status().isNotFound());
        var majorLesson=learning.available(s).stream().filter(l->l.getMajor()==major).findFirst().orElseThrow();
        learning.complete(s.getEmail(),majorLesson.getId());
        pass(s.getEmail(),Stage.TECHNICAL);
        mvc.perform(get("/specialized/READING").with(user(s.getEmail()))).andExpect(status().isOk());
        pass(s.getEmail(),Stage.READING);
        mvc.perform(get("/specialized/SITUATION").with(user(s.getEmail()))).andExpect(status().isOk());
        pass(s.getEmail(),Stage.SITUATION);
        mvc.perform(get("/exams/APPLICATION").with(user(s.getEmail()))).andExpect(status().isForbidden());
        var teacher=students.save(new Student("teacher_"+major,"teacher_"+major+"@test.example",encoder.encode("password123")));
        teacher.setRole("INSTRUCTOR");students.save(teacher);
        for(var kind:List.of(PracticeKind.SITUATION,PracticeKind.EMAIL,PracticeKind.REPORT)) {
            submitAndReview(s,teacher,kind);
        }
        mvc.perform(get("/specialized/WRITING").with(user(s.getEmail()))).andExpect(status().isOk());
        mvc.perform(get("/specialized/APPLICATION").with(user(s.getEmail()))).andExpect(status().isOk());
        pass(s.getEmail(),Stage.APPLICATION);pass(s.getEmail(),Stage.ADVANCED);
        mvc.perform(get("/specialized/ADVANCED").with(user(s.getEmail()))).andExpect(status().isOk());
        submitAndReview(s,teacher,PracticeKind.ADVANCED);
        mvc.perform(get("/journey").with(user(s.getEmail()))).andExpect(status().isOk()).andExpect(model().attribute("allDone",true));
        mvc.perform(get("/specialized").with(user(s.getEmail()))).andExpect(status().isOk()).andExpect(model().attribute("completedCount",6L));
        mvc.perform(get("/competency").with(user(s.getEmail()))).andExpect(status().isOk());
        learning.onboard(s.getEmail(),major==Major.IT?Major.MECHANICAL:Major.IT,Level.ZERO);
        var changed=learning.student(s.getEmail());assertThat(changed.getLevel()).isEqualTo(Level.N4);
        assertThat(workflow.passed(changed,Stage.READING)).isFalse();
        assertThat(specialization.terms(changed,true)).isEmpty();
        mvc.perform(get("/specialized/READING").with(user(s.getEmail()))).andExpect(status().isForbidden());
        mvc.perform(get("/practice/EMAIL").with(user(s.getEmail()))).andExpect(status().isForbidden());
    }
    void submitAndReview(Student s,Student teacher,PracticeKind kind) throws Exception {
        mvc.perform(get("/practice/"+kind).with(user(s.getEmail()))).andExpect(status().isOk());
        String work="担当者様。お疲れ様です。確認した現象と発生時刻を報告いたします。原因はまだ確認できていません。記録を添付し、対応方法について確認をお願いいたします。確認でき次第、改めてご連絡いたします。よろしくお願いいたします。";
        mvc.perform(post("/practice/"+kind+"/draft").with(user(s.getEmail())).with(csrf()).param("response","メモ"))
            .andExpect(redirectedUrl("/practice/"+kind));
        mvc.perform(get("/practice/"+kind).with(user(s.getEmail()))).andExpect(status().isOk()).andExpect(model().attribute("responseText","メモ"));
        mvc.perform(post("/practice/"+kind+"/draft").with(user(s.getEmail())).with(csrf()).param("response","a".repeat(10001)))
            .andExpect(status().isBadRequest());
        mvc.perform(post("/practice/"+kind).with(user(s.getEmail())).with(csrf()).param("response",work)).andExpect(redirectedUrl("/competency"));
        assertThat(drafts.findByStudentIdAndMajorAndKind(s.getId(),s.getMajor(),kind)).isEmpty();
        mvc.perform(get("/practice/"+kind).with(user(s.getEmail()))).andExpect(status().isOk());
        mvc.perform(post("/practice/"+kind).with(user(s.getEmail())).with(csrf()).param("response",work)).andExpect(status().isConflict());
        var submission=workflow.submissions(s).getFirst();
        mvc.perform(get("/instructor/reviews").with(user(s.getEmail()))).andExpect(status().isForbidden());
        mvc.perform(get("/instructor/reviews").with(user(teacher.getEmail()).roles("INSTRUCTOR"))).andExpect(status().isOk());
        mvc.perform(post("/instructor/reviews/"+submission.getId()).with(user(teacher.getEmail()).roles("INSTRUCTOR")).with(csrf())
            .param("language","4").param("technical","4").param("communication","4").param("task","4")
            .param("feedback","Thông tin rõ ràng, dùng thuật ngữ đúng. Tiếp tục luyện mẫu câu yêu cầu xác nhận.")).andExpect(redirectedUrl("/instructor/reviews"));
        assertThat(workflow.reviewed(s,kind)).isTrue();
    }
    @Test void zeroN5N4ProgressionAndNoBypass() throws Exception {
        var s=student("zero_route",Major.IT);
        var first=workflow.submitExam(s.getEmail(),Stage.PLACEMENT,answers(Stage.PLACEMENT,Major.IT,false));
        s=learning.student(s.getEmail());assertThat(s.getLevel()).isEqualTo(Level.ZERO);
        mvc.perform(get("/exams/PLACEMENT").with(user(s.getEmail()))).andExpect(status().isForbidden());
        mvc.perform(get("/exams/N4").with(user(s.getEmail()))).andExpect(status().isForbidden());
        assertThat(learning.available(s)).noneMatch(l->l.getLevel()==Level.N4);
        for(var l:learning.available(s))learning.complete(s.getEmail(),l.getId());
        var fail=workflow.submitExam(s.getEmail(),Stage.N5,answers(Stage.N5,Major.IT,false));
        assertThat(fail.isPassed()).isFalse();assertThat(learning.student(s.getEmail()).getLevel()).isEqualTo(Level.ZERO);
        pass(s.getEmail(),Stage.N5);
        s=learning.student(s.getEmail());assertThat(s.getLevel()).isEqualTo(Level.N5);
        for(var l:learning.available(s))learning.complete(s.getEmail(),l.getId());
        pass(s.getEmail(),Stage.N4);
        assertThat(learning.student(s.getEmail()).getLevel()).isEqualTo(Level.N4);
        var other=student("isolation_route",Major.IT);
        mvc.perform(get("/results/"+first.getId()).with(user(other.getEmail()))).andExpect(status().isNotFound());
        mvc.perform(post("/exams/PLACEMENT").with(user(other.getEmail())).with(csrf()).param("score","100")).andExpect(status().isBadRequest());
        mvc.perform(post("/exams/PLACEMENT").with(user(other.getEmail()))).andExpect(status().isForbidden());
        var mixed=answers(Stage.PLACEMENT,Major.IT,false);
        answers(Stage.N5,Major.IT,true).forEach(mixed::put);
        workflow.submitExam(other.getEmail(),Stage.PLACEMENT,mixed);
        assertThat(learning.student(other.getEmail()).getLevel()).isEqualTo(Level.N5);
    }

    @Test void rubricRevisionAndResubmission() throws Exception {
        var s=student("revision_route",Major.IT);
        pass(s.getEmail(),Stage.PLACEMENT);
        learning.complete(s.getEmail(),9L);
        pass(s.getEmail(),Stage.TECHNICAL);pass(s.getEmail(),Stage.READING);pass(s.getEmail(),Stage.SITUATION);
        mvc.perform(post("/practice/SITUATION").with(user(s.getEmail())).with(csrf()).param("response","short"))
            .andExpect(status().isBadRequest());
        String work="作業中の問題について報告します。".repeat(10);
        var submission=workflow.submitPractice(s.getEmail(),PracticeKind.SITUATION,work);
        var teacher=students.save(new Student("Reviewer","revision_teacher@test.example",encoder.encode("password123")));
        teacher.setRole("INSTRUCTOR");students.save(teacher);
        mvc.perform(post("/instructor/reviews/"+submission.getId()).with(user(s.getEmail())).with(csrf())
            .param("language","5").param("technical","5").param("communication","5").param("task","5")
            .param("feedback","Tự chấm bài không được phép.")).andExpect(status().isForbidden());
        // Even a total over 14 cannot pass if a criterion is below 3.
        workflow.review(teacher.getEmail(),submission.getId(),5,2,5,5,"Cần sửa thông tin kỹ thuật theo tài liệu trước khi nộp lại.");
        assertThat(workflow.reviewed(s,PracticeKind.SITUATION)).isFalse();
        mvc.perform(get("/practice/SITUATION").with(user(s.getEmail()))).andExpect(status().isOk()).andExpect(model().attribute("responseText",work));
        mvc.perform(get("/practice/EMAIL").with(user(s.getEmail()))).andExpect(status().isForbidden());
        mvc.perform(get("/competency").with(user(s.getEmail()))).andExpect(status().isOk());
        var revised=workflow.submitPractice(s.getEmail(),PracticeKind.SITUATION,work+"修正しました。");
        workflow.review(teacher.getEmail(),revised.getId(),4,4,4,4,"Đã sửa đúng theo tài liệu, hãy tiếp tục luyện email công việc.");
        assertThat(workflow.reviewed(s,PracticeKind.SITUATION)).isTrue();
        assertThat(workflow.submissions(s)).hasSize(2);
        mvc.perform(get("/practice/EMAIL").with(user(s.getEmail()))).andExpect(status().isOk());
    }

    @ParameterizedTest @EnumSource(value=Level.class,names={"ZERO","N5","N4"})
    @SuppressWarnings("unchecked")
    void loginRoutesToTheCorrectCurrentCourse(Level level) throws Exception {
        var s=students.save(new Student("branch_"+level,"branch_"+level+"@test.example",encoder.encode("password123")));
        mvc.perform(get("/start").with(user(s.getEmail()))).andExpect(redirectedUrl("/onboarding"));
        mvc.perform(post("/onboarding").with(user(s.getEmail())).with(csrf()).param("major","IT"))
            .andExpect(redirectedUrl("/start"));
        mvc.perform(get("/start").with(user(s.getEmail()))).andExpect(redirectedUrl("/exams/PLACEMENT"));
        var placement=answers(Stage.PLACEMENT,Major.IT,level==Level.N4);
        if(level==Level.N5)answers(Stage.N5,Major.IT,true).forEach(placement::put);
        workflow.submitExam(s.getEmail(),Stage.PLACEMENT,placement);
        mvc.perform(get("/start").with(user(s.getEmail()))).andExpect(redirectedUrl("/dashboard"));
        for(String page:List.of("/dashboard","/path")) {
            var result=mvc.perform(get(page).with(user(s.getEmail()))).andExpect(status().isOk()).andReturn();
            var model=result.getModelAndView().getModel();
            var lessons=(List<Lesson>)model.get("lessons");
            assertThat(lessons).isNotEmpty();
            if(level==Level.ZERO)assertThat(lessons).allMatch(l->l.getMajor()==null && (l.getLevel()==Level.ZERO||l.getLevel()==Level.N5));
            else if(level==Level.N5)assertThat(lessons).allMatch(l->l.getMajor()==null && l.getLevel()==Level.N4);
            else assertThat(lessons).allMatch(l->l.getMajor()==Major.IT);
            assertThat((Lesson)model.get("nextLesson")).isEqualTo(lessons.getFirst());
        }
    }
}
