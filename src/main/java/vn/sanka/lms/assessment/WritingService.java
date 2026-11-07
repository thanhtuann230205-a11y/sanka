package vn.sanka.lms.assessment;

import jakarta.persistence.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import vn.sanka.lms.learning.LearningService;
import vn.sanka.lms.auth.Student;

@Service
public class WritingService {
    private final WorkflowService workflow;
    private final LearningService learning;
    private final DraftRepository drafts;
    @PersistenceContext private EntityManager em;
    public WritingService(WorkflowService workflow,LearningService learning,DraftRepository drafts) {
        this.workflow=workflow;this.learning=learning;this.drafts=drafts;
    }
    public PracticeSubmission latest(Student s,PracticeKind kind) {
        return workflow.submissions(s).stream().filter(a->a.getMajor()==s.getMajor() && a.getKind()==kind).findFirst().orElse(null);
    }
    public String text(Student s,PracticeKind kind) {
        return drafts.findByStudentIdAndMajorAndKind(s.getId(),s.getMajor(),kind).map(PracticeDraft::getContent)
            .orElseGet(()->{var last=latest(s,kind);return last!=null && "REVISION".equals(last.getStatus())?last.getResponse():"";});
    }
    @Transactional public void save(String email,PracticeKind kind,String text) {
        var s=learning.student(email);em.lock(s,LockModeType.PESSIMISTIC_WRITE);em.refresh(s);
        workflow.require(workflow.allowed(s,kind));
        var last=latest(s,kind);
        if(last!=null && "PENDING".equals(last.getStatus()))
            throw new ResponseStatusException(HttpStatus.CONFLICT,"Bài đã nộp và đang chờ chấm; xem trong hồ sơ năng lực.");
        if(text==null || text.length()>10000)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Bản nháp tối đa 10.000 ký tự");
        var draft=drafts.findByStudentIdAndMajorAndKind(s.getId(),s.getMajor(),kind)
            .orElseGet(()->new PracticeDraft(s.getId(),s.getMajor(),kind));
        draft.update(text);drafts.save(draft);
    }
}
