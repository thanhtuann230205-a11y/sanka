package vn.sanka.lms;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.*;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.context.annotation.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import vn.sanka.lms.auth.*;
import vn.sanka.lms.learning.*;
import vn.sanka.lms.assessment.*;
import vn.sanka.lms.language.*;
import vn.sanka.lms.gamification.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;

@SpringBootTest(properties="spring.datasource.url=jdbc:h2:mem:motivation;MODE=MySQL;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc @ActiveProfiles("test") @Import(GamificationTest.ClockConfiguration.class)
class GamificationTest {
    static class TestClock extends Clock {
        private volatile Instant instant=Instant.parse("2026-11-07T03:00:00Z");
        void local(String dateTime){instant=LocalDateTime.parse(dateTime).atZone(ZoneId.of("Asia/Ho_Chi_Minh")).toInstant();}
        void utc(String dateTime){instant=Instant.parse(dateTime);}
        @Override public ZoneId getZone(){return ZoneOffset.UTC;}
        @Override public Clock withZone(ZoneId zone){return new Clock(){public ZoneId getZone(){return zone;}public Clock withZone(ZoneId z){return TestClock.this.withZone(z);}public Instant instant(){return TestClock.this.instant();}};}
        @Override public Instant instant(){return instant;}
    }
    @TestConfiguration static class ClockConfiguration {@Bean @Primary TestClock controlledClock(){return new TestClock();}}
    @Autowired TestClock clock;
    @Autowired StudentRepository students;
    @Autowired GamificationService gamification;
    @Autowired DailyGoalRepository goals;
    @Autowired BadgeRepository badges;
    @Autowired LearningService learning;
    @Autowired LearningCatalog catalog;
    @Autowired WorkflowService workflow;
    @Autowired LanguageService language;
    @Autowired MockMvc mvc;
    @BeforeEach void resetClock(){clock.local("2026-11-07T10:00:00");}
    Student student(String name){return students.save(new Student(name,name+"@motivation.example","unused-test-password"));}
    Map<String,String> answers(Stage stage,boolean right){
        var result=new HashMap<String,String>();catalog.questions(stage,Major.IT).forEach(q->{boolean correct=right&&!(stage==Stage.PLACEMENT&&(q.id().startsWith("n3")||q.id().startsWith("n2")));
            result.put("answer_"+q.id(),String.valueOf(correct?q.correct():(q.correct()+1)%q.options().size()));});return result;
    }
    boolean earned(Student s,String code){return gamification.badgeViews(s.getId()).stream().anyMatch(b->b.code().equals(code)&&b.earned());}

    @Test void awardsAreIdempotentAndBadgesPersist(){
        var s=student("reward_idempotence");
        for(int i=0;i<3;i++)gamification.lesson(s.getId(),1L);
        assertThat(gamification.overview(s.getId()).totalXp()).isEqualTo(20);
        assertThat(earned(s,"FIRST_LESSON")).isTrue();
        gamification.exam(s.getId(),Stage.N5,Major.IT,true);gamification.exam(s.getId(),Stage.N5,Major.MECHANICAL,true);
        gamification.periodic(s.getId(),Level.N5);gamification.periodic(s.getId(),Level.N5);
        gamification.practicePassed(s.getId(),PracticeKind.EMAIL,Major.IT,gamification.now());
        gamification.practicePassed(s.getId(),PracticeKind.EMAIL,Major.IT,gamification.now());
        gamification.languagePassed(s.getId(),Level.N4,Major.IT,gamification.now());
        assertThat(gamification.overview(s.getId()).totalXp()).isEqualTo(165);
        assertThat(earned(s,"XP_100")).isTrue();assertThat(earned(s,"FIRST_EXAM")).isTrue();
        assertThat(earned(s,"FIRST_PRACTICE")).isTrue();assertThat(earned(s,"FIRST_REVIEW")).isTrue();
        gamification.reconcileExisting(s.getId());gamification.reconcileExisting(s.getId());
        assertThat(badges.findByStudentId(s.getId())).extracting(EarnedBadge::getCode).doesNotHaveDuplicates();
    }
    @Test void streakCrossesVietnamMidnightAndSurvivesUntilTheEndOfToday(){
        var s=student("timezone_streak");
        clock.utc("2026-11-06T16:59:00Z");gamification.lesson(s.getId(),1L);
        assertThat(gamification.today()).isEqualTo(LocalDate.of(2026,11,6));
        clock.utc("2026-11-06T17:01:00Z");
        assertThat(gamification.today()).isEqualTo(LocalDate.of(2026,11,7));
        assertThat(gamification.overview(s.getId()).currentStreak()).isEqualTo(1);
        gamification.lesson(s.getId(),2L);
        clock.local("2026-11-08T10:00:00");gamification.lesson(s.getId(),3L);
        assertThat(gamification.overview(s.getId()).currentStreak()).isEqualTo(3);assertThat(earned(s,"STREAK_3")).isTrue();
        clock.local("2026-11-09T23:59:00");assertThat(gamification.overview(s.getId()).currentStreak()).isEqualTo(3);
        clock.local("2026-11-10T00:01:00");assertThat(gamification.overview(s.getId()).currentStreak()).isZero();
        gamification.lesson(s.getId(),4L);
        assertThat(gamification.overview(s.getId()).currentStreak()).isEqualTo(1);
        assertThat(gamification.overview(s.getId()).bestStreak()).isEqualTo(3);assertThat(earned(s,"STREAK_3")).isTrue();
    }
    @Test void dailyTargetsAreSnapshottedAndCalendarMatchesActivity() throws Exception {
        var s=student("daily_target");
        gamification.lesson(s.getId(),1L);gamification.lesson(s.getId(),2L);
        assertThat(gamification.overview(s.getId()).goalReached()).isTrue();assertThat(earned(s,"FIRST_GOAL")).isTrue();
        gamification.setGoal(s.getId(),60);
        clock.local("2026-11-08T10:00:00");gamification.lesson(s.getId(),3L);gamification.setGoal(s.getId(),80);
        assertThat(goals.findByStudentIdAndActivityDate(s.getId(),LocalDate.of(2026,11,7)).orElseThrow().getTargetXp()).isEqualTo(60);
        assertThat(gamification.overview(s.getId()).targetXp()).isEqualTo(80);
        var calendar=gamification.calendar(s.getId(),YearMonth.of(2026,11));assertThat(calendar).hasSize(42);
        var old=calendar.stream().filter(d->d.date().equals(LocalDate.of(2026,11,7))).findFirst().orElseThrow();
        assertThat(old.xp()).isEqualTo(40);assertThat(old.activities()).isEqualTo(2);assertThat(old.goalReached()).isFalse();
        mvc.perform(get("/motivation?month=2026-11&day=2026-11-07").with(user(s.getEmail()))).andExpect(status().isOk());
        mvc.perform(post("/motivation/goal").with(user(s.getEmail())).with(csrf()).param("targetXp","0")).andExpect(status().isBadRequest());
        mvc.perform(post("/motivation/goal").with(user(s.getEmail())).param("targetXp","100")).andExpect(status().isForbidden());
        mvc.perform(get("/motivation?month=bad-month").with(user(s.getEmail()))).andExpect(status().isBadRequest());
    }
    @Test void leaderboardRequiresConsentUsesAliasesAndSupportsWithdrawal() throws Exception {
        var a=student("ranking_a");var b=student("ranking_b");var hidden=student("ranking_hidden");
        a.onboard(Major.IT,Level.ZERO);b.onboard(Major.IT,Level.ZERO);students.save(a);students.save(b);
        gamification.lesson(a.getId(),1L);gamification.lesson(b.getId(),1L);
        gamification.lesson(hidden.getId(),1L);gamification.lesson(hidden.getId(),2L);
        assertThat(gamification.leaderboard(a.getId(),"all",null,null)).isEmpty();
        gamification.rankingPreference(a.getId(),true,"Sakura");gamification.rankingPreference(b.getId(),true,"Kaizen");
        var ranking=gamification.leaderboard(a.getId(),"all",Major.IT,Level.ZERO);
        assertThat(ranking).hasSize(2);assertThat(ranking).extracting(GamificationService.Rank::rank).containsOnly(1);
        assertThat(ranking).extracting(GamificationService.Rank::nickname).containsExactlyInAnyOrder("Sakura","Kaizen");
        mvc.perform(get("/leaderboard?period=all&major=IT&level=ZERO").with(user(a.getEmail()))).andExpect(status().isOk())
            .andExpect(content().string(not(containsString(b.getEmail())))).andExpect(content().string(not(containsString("ranking_hidden"))));
        mvc.perform(post("/leaderboard/preferences").with(user(a.getEmail())).with(csrf()).param("enabled","false"))
            .andExpect(redirectedUrl("/leaderboard"));
        assertThat(gamification.leaderboard(a.getId(),"all",null,null)).extracting(GamificationService.Rank::nickname).containsExactly("Kaizen");
        assertThat(gamification.overview(a.getId()).totalXp()).isEqualTo(20);
        assertThat(gamification.leaderboard(a.getId(),"all",Major.MECHANICAL,null)).isEmpty();
        mvc.perform(post("/leaderboard/preferences").with(user(a.getEmail())).with(csrf()).param("enabled","true").param("nickname","a\n\nb"))
            .andExpect(status().isBadRequest());
        mvc.perform(get("/leaderboard?period=unknown").with(user(a.getEmail()))).andExpect(status().isBadRequest());
        mvc.perform(get("/motivation?studentId="+hidden.getId()).with(user(a.getEmail()))).andExpect(model().attribute("motivation",gamification.overview(a.getId())));
    }
    @Test void weeklyXpDoesNotRepeatAndZeroXpSubmissionsStillCountForStreak(){
        var s=student("weekly_review");clock.local("2026-11-02T10:00:00");
        gamification.periodic(s.getId(),Level.N4);clock.local("2026-11-03T10:00:00");gamification.periodic(s.getId(),Level.N4);
        assertThat(gamification.overview(s.getId()).totalXp()).isEqualTo(15);
        gamification.submitted(s.getId(),"specialized:EMAIL:IT");gamification.submitted(s.getId(),"specialized:EMAIL:IT");
        assertThat(gamification.overview(s.getId()).currentStreak()).isEqualTo(2);assertThat(gamification.overview(s.getId()).todayXp()).isZero();
        clock.local("2026-11-09T10:00:00");gamification.periodic(s.getId(),Level.N4);
        gamification.rankingPreference(s.getId(),true,"Weekly learner");
        assertThat(gamification.leaderboard(s.getId(),"week",null,null).stream().filter(r->r.own()).findFirst().orElseThrow().xp()).isEqualTo(15);
        assertThat(gamification.overview(s.getId()).totalXp()).isEqualTo(30);
    }
    @Test void teacherRewardsBelongToSubmissionDateNotGradingDate(){
        var s=student("review_activity_date");clock.local("2026-11-01T10:00:00");var submitted=gamification.now();
        gamification.submitted(s.getId(),"specialized:EMAIL:IT");clock.local("2026-11-04T10:00:00");
        gamification.practicePassed(s.getId(),PracticeKind.EMAIL,Major.IT,submitted);
        assertThat(gamification.overview(s.getId()).totalXp()).isEqualTo(50);
        assertThat(gamification.overview(s.getId()).todayXp()).isZero();assertThat(gamification.overview(s.getId()).currentStreak()).isZero();
        assertThat(gamification.history(s.getId())).allMatch(e->e.getActivityDate().equals(LocalDate.of(2026,11,1)));
        var cell=gamification.calendar(s.getId(),YearMonth.of(2026,11)).stream().filter(d->d.date().equals(LocalDate.of(2026,11,1))).findFirst().orElseThrow();
        assertThat(cell.goalReached()).isTrue();assertThat(earned(s,"FIRST_PRACTICE")).isTrue();
    }
    @Test void sevenDayBadgeAndConcurrentAwardsStayUnique() throws Exception {
        var s=student("seven_day");
        for(int i=1;i<=7;i++){clock.local("2026-11-0"+i+"T10:00:00");gamification.lesson(s.getId(),(long)i);}
        assertThat(earned(s,"STREAK_7")).isTrue();
        var concurrent=student("concurrent_award");var pool=Executors.newFixedThreadPool(4);
        try{var futures=new ArrayList<CompletableFuture<Void>>();for(int i=0;i<4;i++)futures.add(CompletableFuture.runAsync(()->gamification.lesson(concurrent.getId(),1L),pool));
            CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).get(20,TimeUnit.SECONDS);
        }finally{pool.shutdownNow();}
        assertThat(gamification.history(concurrent.getId())).hasSize(1);assertThat(gamification.overview(concurrent.getId()).totalXp()).isEqualTo(20);
    }
    @Test void actualLearningServicesGenerateTheLedger() throws Exception {
        var s=student("actual_rewards");
        workflow.submitExam(s.getEmail(),Stage.PLACEMENT,answers(Stage.PLACEMENT,true));learning.onboard(s.getEmail(),Major.IT,Level.ZERO);
        learning.complete(s.getEmail(),9L);learning.complete(s.getEmail(),9L);
        workflow.submitExam(s.getEmail(),Stage.TECHNICAL,answers(Stage.TECHNICAL,true));workflow.submitExam(s.getEmail(),Stage.TECHNICAL,answers(Stage.TECHNICAL,true));
        assertThat(gamification.overview(s.getId()).totalXp()).isEqualTo(80);
        workflow.submitExam(s.getEmail(),Stage.READING,answers(Stage.READING,true));workflow.submitExam(s.getEmail(),Stage.SITUATION,answers(Stage.SITUATION,true));
        String text="確認した情報に基づいて、担当者に状況と次の行動を報告いたします。".repeat(8);
        var work=workflow.submitPractice(s.getEmail(),PracticeKind.SITUATION,text);
        var teacher=student("reward_teacher");teacher.setRole("INSTRUCTOR");students.save(teacher);
        workflow.review(teacher.getEmail(),work.getId(),4,4,4,4,"状況を正確に説明できています。次は確認の表現を練習してください。");
        assertThat(gamification.overview(s.getId()).totalXp()).isEqualTo(210);
        mvc.perform(get("/motivation").with(user(s.getEmail()))).andExpect(status().isOk());
        mvc.perform(get("/leaderboard").with(user(s.getEmail()))).andExpect(status().isOk());
        mvc.perform(get("/dashboard").with(user(s.getEmail()))).andExpect(status().isOk()).andExpect(model().attribute("xp",210L));
        mvc.perform(get("/motivation")).andExpect(status().is3xxRedirection());
        mvc.perform(post("/leaderboard/preferences").with(user(teacher.getEmail()).roles("INSTRUCTOR")).with(csrf()).param("enabled","true").param("nickname","Teacher"))
            .andExpect(status().isForbidden());
    }
    @Test void migrationPreservesExistingLessonXpAndOriginalDates() throws Exception {
        String url="jdbc:h2:mem:legacy_motivation;MODE=MySQL;DB_CLOSE_DELAY=-1";
        org.flywaydb.core.Flyway.configure().dataSource(url,"sa","").target("6").load().migrate();
        try(var connection=java.sql.DriverManager.getConnection(url,"sa","");var statement=connection.createStatement()){
            statement.executeUpdate("INSERT INTO students(id,email,password,name) VALUES (999,'legacy@example.com','unused','Legacy'),(998,'legacy2@example.com','unused','Legacy 2')");
            statement.executeUpdate("INSERT INTO lesson_progress(student_id,lesson_id,completed_at) VALUES (999,1,'2026-11-05 10:00:00'),(999,2,'2026-11-05 11:00:00'),(998,1,'2026-11-04 09:00:00')");
        }
        org.flywaydb.core.Flyway.configure().dataSource(url,"sa","").load().migrate();
        try(var connection=java.sql.DriverManager.getConnection(url,"sa","");var statement=connection.createStatement()){
            try(var result=statement.executeQuery("SELECT COUNT(*),SUM(points),MIN(points),MAX(points) FROM xp_events")){
                assertThat(result.next()).isTrue();assertThat(result.getInt(1)).isEqualTo(3);assertThat(result.getInt(2)).isEqualTo(60);
                assertThat(result.getInt(3)).isEqualTo(20);assertThat(result.getInt(4)).isEqualTo(20);
            }
            try(var result=statement.executeQuery("SELECT activity_date,COUNT(*) FROM xp_events WHERE student_id=999 GROUP BY activity_date")){
                assertThat(result.next()).isTrue();assertThat(result.getDate(1).toLocalDate()).isEqualTo(LocalDate.of(2026,11,5));assertThat(result.getInt(2)).isEqualTo(2);
            }
            try(var result=statement.executeQuery("SELECT COUNT(*) FROM daily_goals")){assertThat(result.next()).isTrue();assertThat(result.getInt(1)).isEqualTo(2);}
            try(var result=statement.executeQuery("SELECT COUNT(*) FROM lesson_progress")){assertThat(result.next()).isTrue();assertThat(result.getInt(1)).isEqualTo(3);}
        }
    }
}
