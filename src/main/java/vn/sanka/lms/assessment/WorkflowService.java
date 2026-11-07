package vn.sanka.lms.assessment;

import jakarta.persistence.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import vn.sanka.lms.auth.Student;
import vn.sanka.lms.learning.*;
import java.util.*;

@Service
public class WorkflowService {
    private final LearningService learning;
    private final LearningCatalog catalog;
    private final AttemptRepository attempts;
    private final SubmissionRepository submissions;
    private final DraftRepository drafts;
    private final vn.sanka.lms.gamification.GamificationService gamification;
    private final ExamAnswerRepository answerSnapshots;
    @PersistenceContext private EntityManager em;

    public WorkflowService(LearningService learning, LearningCatalog catalog,
                           AttemptRepository attempts, SubmissionRepository submissions,DraftRepository drafts,vn.sanka.lms.gamification.GamificationService gamification,ExamAnswerRepository answerSnapshots) {
        this.learning=learning; this.catalog=catalog; this.attempts=attempts; this.submissions=submissions;this.drafts=drafts;this.gamification=gamification;
        this.answerSnapshots=answerSnapshots;
    }

    public boolean passed(Student s, Stage stage) {
        return s.getMajor()!=null && attempts.existsByStudentIdAndMajorAndStageAndPassedTrue(s.getId(),s.getMajor(),stage);
    }
    public boolean reviewed(Student s, PracticeKind kind) {
        return s.getMajor()!=null && submissions.existsByStudentIdAndMajorAndKindAndStatus(s.getId(),s.getMajor(),kind,"PASSED");
    }
    private boolean completedLevel(Student s, Level level) {
        var lessons=learning.available(s).stream().filter(l->l.getLevel()==level && l.getMajor()==null).toList();
        var done=learning.completed(s);
        return !lessons.isEmpty() && lessons.stream().allMatch(l->done.contains(l.getId()));
    }
    private boolean specializedLessonDone(Student s) {
        var lessons=learning.available(s).stream().filter(l->l.getMajor()==s.getMajor()).toList();
        var done=learning.completed(s);
        return !lessons.isEmpty() && lessons.stream().allMatch(l->done.contains(l.getId()));
    }
    public boolean allowed(Student s, Stage stage) {
        if(s.getMajor()==null)return false;
        if(stage==Stage.PLACEMENT)return !s.isPlacementCompleted();
        if(!s.isPlacementCompleted())return false;
        return switch(stage) {
            case N5 -> completedLevel(s,Level.ZERO) && completedLevel(s,Level.N5);
            case N4 -> s.getLevel().ordinal()>=Level.N5.ordinal() && completedLevel(s,Level.N4);
            case N3 -> s.getLevel().ordinal()>=Level.N4.ordinal() && completedLevel(s,Level.N3);
            case N2 -> s.getLevel().ordinal()>=Level.N3.ordinal() && completedLevel(s,Level.N2);
            case TECHNICAL -> s.getLevel().ordinal()>=Level.N4.ordinal() && specializedLessonDone(s);
            case READING -> passed(s,Stage.TECHNICAL);
            case SITUATION -> passed(s,Stage.READING);
            case APPLICATION -> passed(s,Stage.SITUATION) && reviewed(s,PracticeKind.SITUATION)
                && reviewed(s,PracticeKind.EMAIL) && reviewed(s,PracticeKind.REPORT);
            case ADVANCED -> passed(s,Stage.APPLICATION);
            default -> false;
        };
    }
    public boolean allowed(Student s, PracticeKind kind) {
        return s.isPlacementCompleted() && switch(kind) {
            case SITUATION -> passed(s,Stage.SITUATION);
            case EMAIL -> reviewed(s,PracticeKind.SITUATION);
            case REPORT -> reviewed(s,PracticeKind.EMAIL);
            case ADVANCED -> passed(s,Stage.ADVANCED);
        };
    }
    public String prerequisite(Stage stage) {
        return switch(stage) {
            case PLACEMENT -> "Chọn chuyên ngành. Bài đầu vào được làm một lần; sau đó tiến cấp bằng bài thi N5/N4.";
            case N5 -> "Hoàn thành bài Zero và N5.";
            case N4 -> "Đạt N5 và hoàn thành bài N4.";
            case N3 -> "Đạt N4 và hoàn thành bài N3; học song song với chuyên ngành.";
            case N2 -> "Đạt N3 và hoàn thành bài nhóm N2–N1.";
            case TECHNICAL -> "Đạt N4 và hoàn thành bài chuyên ngành hiện tại.";
            case READING -> "Đạt kiểm tra thuật ngữ chuyên ngành.";
            case SITUATION -> "Đạt kiểm tra đọc hiểu tài liệu.";
            case APPLICATION -> "Đạt kiểm tra tình huống; hội thoại, email và báo cáo được giảng viên chấm đạt.";
            case ADVANCED -> "Đạt bài đánh giá kiến thức ứng dụng.";
        };
    }
    public record Step(Stage stage, boolean allowed, boolean passed, String prerequisite) {}
    public List<Step> steps(Student s) {
        return Arrays.stream(Stage.values()).map(stage->new Step(stage,allowed(s,stage),
            switch(stage) {
                case PLACEMENT -> s.isPlacementCompleted();
                case N5 -> s.isPlacementCompleted() && s.getLevel().ordinal()>=Level.N5.ordinal();
                case N4 -> s.isPlacementCompleted() && s.getLevel().ordinal()>=Level.N4.ordinal();
                case N3 -> s.isPlacementCompleted() && s.getLevel().ordinal()>=Level.N3.ordinal();
                case N2 -> s.isPlacementCompleted() && s.getLevel().ordinal()>=Level.N2.ordinal();
                default -> passed(s,stage);
            },prerequisite(stage))).toList();
    }
    public void require(boolean condition) {
        if(!condition)throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Chưa đủ điều kiện mở học phần");
    }
    public List<LearningCatalog.QuestionView> questions(Student s, Stage stage) {
        require(allowed(s,stage));
        return catalog.questions(stage,s.getMajor()).stream().map(LearningCatalog.Question::view).toList();
    }

    @Transactional
    public ExamAttempt submitExam(String email, Stage stage, Map<String,String> answers) {
        Student s=learning.student(email);
        em.lock(s,LockModeType.PESSIMISTIC_WRITE);
        em.refresh(s);
        require(allowed(s,stage));
        var questions=catalog.questions(stage,s.getMajor());
        var evidence=new StringBuilder("Ngân hàng câu hỏi mẫu v1 · ngưỡng đạt 80%\n");
        int correct=0, n5=0, n4=0, n3=0, n2=0;
        var chosenAnswers=new ArrayList<Integer>();
        for(var q:questions) {
            String answer=answers.get("answer_"+q.id());
            int chosen;
            try { chosen=Integer.parseInt(answer); }
            catch(Exception e){throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Hãy trả lời đầy đủ câu hỏi");}
            if(chosen<0 || chosen>=q.options().size())throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Lựa chọn không hợp lệ");
            chosenAnswers.add(chosen);
            boolean right=chosen==q.correct();
            if(right){correct++;if(q.id().startsWith("n5"))n5++;if(q.id().startsWith("n4"))n4++;if(q.id().startsWith("n3"))n3++;if(q.id().startsWith("n2"))n2++;}
            evidence.append(q.prompt()).append("\nĐã chọn: ").append(q.options().get(chosen))
                .append(right?" ✓\n":" ✗\n").append("Đáp án: ").append(q.options().get(q.correct()))
                .append("\nGiải thích: ").append(q.explanation()).append("\n\n");
        }
        int score=correct*100/questions.size();
        boolean pass=score>=80;
        Level assignedLevel=null;
        if(stage==Stage.PLACEMENT) {
            Level assigned=n5>=4?(n4>=4?(n3>=4?(n2>=4?Level.N2:Level.N3):Level.N4):Level.N5):Level.ZERO;
            s.assess(assigned);pass=true;
            assignedLevel=assigned;
            evidence.append("Nhóm N5: ").append(n5).append("/5 · Nhóm N4: ").append(n4)
                .append("/5\nLộ trình được xếp: ").append(assigned)
                .append("\nNhóm N3: ").append(n3).append("/5 · Nhóm N2–N1: ").append(n2).append("/5")
                .append(". Kết quả phân luồng nội bộ, không phải chứng chỉ JLPT.");
        } else if(pass && stage==Stage.N5 && s.getLevel().ordinal()<Level.N5.ordinal())s.assess(Level.N5);
        else if(pass && stage==Stage.N4 && s.getLevel().ordinal()<Level.N4.ordinal())s.assess(Level.N4);
        else if(pass && stage==Stage.N3 && s.getLevel().ordinal()<Level.N3.ordinal())s.assess(Level.N3);
        else if(pass && stage==Stage.N2 && s.getLevel().ordinal()<Level.N2.ordinal())s.assess(Level.N2);
        var attempt=new ExamAttempt(s.getId(),s.getMajor(),stage,score,pass,evidence.toString());
        attempt.assignLevel(assignedLevel);attempt=attempts.save(attempt);
        for(int i=0;i<questions.size();i++)answerSnapshots.save(new ExamAnswerSnapshot(attempt.getId(),i+1,questions.get(i),chosenAnswers.get(i)));
        gamification.exam(s.getId(),stage,s.getMajor(),pass);
        return attempt;
    }

    @Transactional
    public PracticeSubmission submitPractice(String email, PracticeKind kind, String response) {
        Student s=learning.student(email);em.lock(s,LockModeType.PESSIMISTIC_WRITE);em.refresh(s);
        require(allowed(s,kind));
        if(response==null || response.strip().length()<80 || response.length()>10000)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Bài viết cần từ 80 đến 10.000 ký tự");
        if(submissions.existsByStudentIdAndMajorAndKindAndStatus(s.getId(),s.getMajor(),kind,"PENDING"))
            throw new ResponseStatusException(HttpStatus.CONFLICT,"Bạn đã có bài đang chờ chấm");
        var submission=submissions.save(new PracticeSubmission(s.getId(),s.getMajor(),kind,response.strip(),gamification.now()));
        drafts.deleteByStudentIdAndMajorAndKind(s.getId(),s.getMajor(),kind);
        gamification.submitted(s.getId(),"specialized:"+kind+":"+s.getMajor());
        return submission;
    }

    @Transactional
    public void review(String email, Long id, int language, int technical, int communication, int task, String feedback) {
        Student reviewer=learning.student(email);
        require("INSTRUCTOR".equals(reviewer.getRole()));
        var submission=submissions.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));
        em.lock(submission,LockModeType.PESSIMISTIC_WRITE);em.refresh(submission);
        require(!submission.getStudentId().equals(reviewer.getId()));
        if(!"PENDING".equals(submission.getStatus()))throw new ResponseStatusException(HttpStatus.CONFLICT,"Bài đã được chấm");
        if(java.util.stream.IntStream.of(language,technical,communication,task).anyMatch(v->v<0||v>5)
            || feedback==null || feedback.strip().length()<10 || feedback.length()>4000)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Điểm từ 0–5, nhận xét từ 10–4.000 ký tự");
        submission.review(reviewer.getId(),language,technical,communication,task,feedback.strip());
        if("PASSED".equals(submission.getStatus()))gamification.practicePassed(submission.getStudentId(),submission.getKind(),submission.getMajor(),submission.getCreatedAt());
    }

    public ExamAttempt attempt(Student s, Long id) {
        return attempts.findById(id).filter(a->a.getStudentId().equals(s.getId()))
            .orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));
    }
    public List<ExamAttempt> history(Student s){return attempts.findByStudentIdOrderByCreatedAtDesc(s.getId());}
    public List<PracticeSubmission> submissions(Student s){return submissions.findByStudentIdOrderByCreatedAtDesc(s.getId());}
    public List<PracticeSubmission> pending(){return submissions.findByStatusOrderByCreatedAtAsc("PENDING");}
}
