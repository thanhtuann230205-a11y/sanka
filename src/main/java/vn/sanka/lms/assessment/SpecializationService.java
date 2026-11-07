package vn.sanka.lms.assessment;

import jakarta.persistence.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import vn.sanka.lms.auth.Student;
import vn.sanka.lms.learning.*;
import java.util.*;

@Service
public class SpecializationService {
    private final WorkflowService workflow;
    private final LearningService learning;
    private final TermRepository terms;
    private final BookmarkRepository bookmarks;
    private final WritingService writing;
    @PersistenceContext private EntityManager em;
    public SpecializationService(WorkflowService workflow,LearningService learning,TermRepository terms,
                                 BookmarkRepository bookmarks,WritingService writing) {
        this.workflow=workflow;this.learning=learning;this.terms=terms;this.bookmarks=bookmarks;this.writing=writing;
    }
    public boolean entered(Student s){return s.getMajor()!=null && s.isPlacementCompleted() && s.getLevel().ordinal()>=Level.N4.ordinal();}
    public boolean open(Student s,SpecializedPhase phase) {
        if(!entered(s))return false;
        return switch(phase) {
            case TERMS -> true;
            case READING -> workflow.allowed(s,Stage.READING);
            case SITUATION -> workflow.allowed(s,Stage.SITUATION);
            case WRITING -> workflow.reviewed(s,PracticeKind.SITUATION);
            case APPLICATION -> workflow.allowed(s,Stage.APPLICATION);
            case ADVANCED -> workflow.allowed(s,Stage.ADVANCED);
        };
    }
    public boolean complete(Student s,SpecializedPhase phase) {
        if(!entered(s))return false;
        return switch(phase) {
            case TERMS -> workflow.passed(s,Stage.TECHNICAL);
            case READING -> workflow.passed(s,Stage.READING);
            case SITUATION -> workflow.passed(s,Stage.SITUATION) && workflow.reviewed(s,PracticeKind.SITUATION);
            case WRITING -> workflow.reviewed(s,PracticeKind.EMAIL) && workflow.reviewed(s,PracticeKind.REPORT);
            case APPLICATION -> workflow.passed(s,Stage.APPLICATION);
            case ADVANCED -> workflow.passed(s,Stage.ADVANCED) && workflow.reviewed(s,PracticeKind.ADVANCED);
        };
    }
    public record PhaseStep(SpecializedPhase phase,String status,String description,String url) {}
    public List<PhaseStep> steps(Student s) {
        return Arrays.stream(SpecializedPhase.values()).map(p->new PhaseStep(p,
            complete(s,p)?"COMPLETED":open(s,p)?"OPEN":"LOCKED",switch(p){
                case TERMS -> "Học Kanji, cách đọc, giải thích tiếng Nhật, nghĩa và ví dụ; lưu từ cần ôn.";
                case READING -> "Đọc biên bản kỹ thuật, xác định dữ kiện và yêu cầu, kiểm tra đọc hiểu.";
                case SITUATION -> "Đóng vai với trưởng nhóm, chọn cách phản hồi và nộp hội thoại để được góp ý.";
                case WRITING -> "Luyện email, báo cáo có bố cục; lưu nháp, nhận phản hồi và nộp lại.";
                case APPLICATION -> "Tổng hợp kết quả kiến thức và rubric thực hành, đánh giá ứng dụng.";
                case ADVANCED -> "Học ngôn ngữ cải tiến, lập đề xuất và nhận đánh giá chuyên ngành.";
            },"/specialized/"+p)).toList();
    }
    public record Task(PracticeKind kind,boolean available,boolean passed,String status,String feedback) {}
    public List<Task> tasks(Student s,SpecializedPhase phase) {
        List<PracticeKind> kinds=switch(phase){
            case SITUATION -> List.of(PracticeKind.SITUATION);
            case WRITING -> List.of(PracticeKind.EMAIL,PracticeKind.REPORT);
            case ADVANCED -> List.of(PracticeKind.ADVANCED);
            default -> List.of();
        };
        return kinds.stream().map(k->{var last=writing.latest(s,k);return new Task(k,workflow.allowed(s,k),
            workflow.reviewed(s,k),last==null?"NEW":last.getStatus(),last==null?null:last.getFeedback());}).toList();
    }
    public List<Term> terms(Student s,boolean savedOnly) {
        var saved=saved(s);
        return terms.findAll().stream().filter(t->t.getMajor()==s.getMajor()).filter(t->!savedOnly||saved.contains(t.getId())).toList();
    }
    public Set<Long> saved(Student s){return new HashSet<>(bookmarks.findByStudentId(s.getId()).stream().map(TermBookmark::getTermId).toList());}
    @Transactional public void bookmark(String email,Long termId,boolean saved) {
        var s=learning.student(email);em.lock(s,LockModeType.PESSIMISTIC_WRITE);em.refresh(s);
        workflow.require(entered(s));
        terms.findById(termId).filter(t->t.getMajor()==s.getMajor()).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));
        var existing=bookmarks.findByStudentIdAndTermId(s.getId(),termId);
        if(saved && existing.isEmpty())bookmarks.save(new TermBookmark(s.getId(),termId));
        else if(!saved)existing.ifPresent(bookmarks::delete);
    }
    public record Skill(String label,Double score,String recommendation) {}
    public List<Skill> skills(Student s) {
        var reviewed=workflow.submissions(s).stream().filter(a->a.getMajor()==s.getMajor() && a.getReviewedAt()!=null).toList();
        var latest=new EnumMap<PracticeKind,PracticeSubmission>(PracticeKind.class);
        reviewed.forEach(a->latest.putIfAbsent(a.getKind(),a));
        return List.of(
            skill("Ngôn ngữ",latest.values().stream().mapToInt(PracticeSubmission::getLanguageScore).average(),"Ôn mẫu câu, trợ từ và từ vựng trong bài đã được nhận xét."),
            skill("Chính xác kỹ thuật",latest.values().stream().mapToInt(PracticeSubmission::getTechnicalScore).average(),"Đọc lại dữ kiện tài liệu, phân biệt hiện tượng với nguyên nhân giả định."),
            skill("Giao tiếp",latest.values().stream().mapToInt(PracticeSubmission::getCommunicationScore).average(),"Luyện cách nhờ xác nhận, báo cáo rõ ý và dùng lối nói lịch sự."),
            skill("Hoàn thành nhiệm vụ",latest.values().stream().mapToInt(PracticeSubmission::getTaskScore).average(),"Đối chiếu checklist; bổ sung thông tin, người phụ trách và bước tiếp theo."));
    }
    private Skill skill(String label,OptionalDouble score,String recommendation) {
        return new Skill(label,score.isPresent()?Math.round(score.getAsDouble()*10)/10.0:null,
            score.isEmpty()?"Nộp bài thực hành để nhận điểm và phản hồi từ giảng viên.":score.getAsDouble()<3.5?recommendation:"Tiếp tục áp dụng trong đề xuất nâng cao; xem phản hồi chi tiết để hoàn thiện.");
    }
}
