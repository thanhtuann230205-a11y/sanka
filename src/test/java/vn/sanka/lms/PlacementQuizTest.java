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
import static org.hamcrest.Matchers.*;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test")
class PlacementQuizTest {
    @Autowired MockMvc mvc;
    @Autowired StudentRepository students;
    @Autowired LearningService learning;
    @Autowired LearningCatalog catalog;
    @Autowired AttemptRepository attempts;
    @Autowired ExamAnswerRepository snapshots;
    @Autowired ExamResultService results;
    @Autowired vn.sanka.lms.gamification.GamificationService gamification;

    @Test void placementHasItsOwnQuizAndServerPreservesInvalidSubmission() throws Exception {
        var s=students.save(new Student("Quiz learner","quiz_view@example.com","unused-password"));
        
        mvc.perform(get("/exams/PLACEMENT").with(user(s.getEmail())))
            .andExpect(status().isOk()).andExpect(view().name("placement-quiz"))
            .andExpect(model().attribute("questions",hasSize(20)))
            .andExpect(content().string(containsString("/css/placement-quiz.css")))
            .andExpect(content().string(containsString("/js/placement-quiz.js")))
            .andExpect(content().string(containsString("id=\"quiz-review\"")))
            .andExpect(content().string(containsString("/audio/n5.wav")));
        mvc.perform(post("/exams/PLACEMENT").with(user(s.getEmail())).with(csrf()).param("answer_n5a","1"))
            .andExpect(status().isBadRequest()).andExpect(view().name("placement-quiz"))
            .andExpect(model().attribute("submittedAnswers",hasEntry("answer_n5a","1")));
        assertThat(learning.student(s.getEmail()).isPlacementCompleted()).isFalse();
        var request=post("/exams/PLACEMENT").with(user(s.getEmail())).with(csrf());
        catalog.questions(Stage.PLACEMENT,Major.IT).forEach(q->request.param("answer_"+q.id(),String.valueOf(
            q.id().startsWith("n3")||q.id().startsWith("n2")?(q.correct()+1)%q.options().size():q.correct())));
        var result=mvc.perform(request).andExpect(status().is3xxRedirection()).andReturn();
        assertThat(learning.student(s.getEmail()).getLevel()).isEqualTo(Level.N4);
        var id=Long.parseLong(result.getResponse().getRedirectedUrl().replace("/results/",""));
        var attempt=attempts.findById(id).orElseThrow();
        assertThat(attempt.getAssignedLevel()).isEqualTo(Level.N4);
        assertThat(snapshots.findByAttemptIdOrderByQuestionNumberAsc(id)).hasSize(20);
        var summary=results.summary(attempt);
        assertThat(summary.correctCount()).isEqualTo(10);assertThat(summary.totalQuestions()).isEqualTo(20);
        assertThat(summary.questions().get(0).options()).hasSize(3);
        assertThat(summary.questions().get(0).correct()).isTrue();assertThat(summary.questions().get(10).correct()).isFalse();
        assertThat(summary.questions().get(3).audioId()).isEqualTo("n5");
        var promoted=learning.student(s.getEmail());promoted.assess(Level.N3);students.save(promoted);
        assertThat(results.summary(attempts.findById(id).orElseThrow()).level()).isEqualTo(Level.N4);
        long xpBefore=gamification.overview(s.getId()).totalXp();
        mvc.perform(get(result.getResponse().getRedirectedUrl()).with(user(s.getEmail())))
            .andExpect(status().isOk()).andExpect(view().name("placement-result"))
            .andExpect(content().string(containsString("data-placement-complete")))
            .andExpect(content().string(containsString("Xem lại kết quả")))
            .andExpect(content().string(not(containsString("id=\"result-question-panels\""))));
        mvc.perform(get("/results/"+id+"/review").with(user(s.getEmail())))
            .andExpect(status().isOk()).andExpect(view().name("placement-review"))
            .andExpect(content().string(containsString("Bạn đã trả lời đúng")))
            .andExpect(content().string(containsString("Câu này chưa đúng")))
            .andExpect(content().string(containsString("Đáp án đúng")));
        assertThat(gamification.overview(s.getId()).totalXp()).isEqualTo(xpBefore);
        var other=students.save(new Student("Other quiz","quiz_other@example.com","unused-password"));
        mvc.perform(get("/results/"+id+"/review").with(user(other.getEmail()))).andExpect(status().isNotFound());
        learning.onboard(s.getEmail(),Major.IT,Level.ZERO);learning.complete(s.getEmail(),9L);
        mvc.perform(get("/exams/TECHNICAL").with(user(s.getEmail()))).andExpect(status().isOk()).andExpect(view().name("exam"));
    }
    @Test void oldAttemptsCanBeReviewedWithoutInventingMissingOptions() throws Exception {
        var s=students.save(new Student("Legacy quiz","quiz_legacy@example.com","unused-password"));
        var attempt=attempts.save(new ExamAttempt(s.getId(),Major.IT,Stage.PLACEMENT,0,true,
            "Ngân hàng cũ\nCâu hỏi cũ?\nĐã chọn: Lựa chọn B ✗\nĐáp án: Lựa chọn A\nGiải thích: A phù hợp với câu hỏi.\n\nLộ trình được xếp: N5."));
        var summary=results.summary(attempt);
        assertThat(summary.legacy()).isTrue();assertThat(summary.level()).isEqualTo(Level.N5);
        assertThat(summary.questions()).hasSize(1);assertThat(summary.questions().getFirst().options()).hasSize(2);
        assertThat(summary.questions().getFirst().correct()).isFalse();
        mvc.perform(get("/results/"+attempt.getId()+"/review").with(user(s.getEmail())))
            .andExpect(status().isOk()).andExpect(content().string(containsString("Câu hỏi cũ?")));
    }
}
