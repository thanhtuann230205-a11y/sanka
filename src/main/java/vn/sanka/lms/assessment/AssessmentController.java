package vn.sanka.lms.assessment;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.sanka.lms.learning.LearningService;
import java.security.Principal;
import java.util.*;

@Controller
public class AssessmentController {
    private final WorkflowService workflow;
    private final LearningService learning;
    private final LearningCatalog catalog;
    private final WritingService writing;
    private final SpecializationContent content;
    private final DraftRepository drafts;
    private final ExamResultService results;
    public AssessmentController(WorkflowService workflow,LearningService learning,LearningCatalog catalog,
                                WritingService writing,SpecializationContent content,DraftRepository drafts,ExamResultService results) {
        this.workflow=workflow;this.learning=learning;this.catalog=catalog;this.writing=writing;this.content=content;this.drafts=drafts;
        this.results=results;
    }
    @ModelAttribute void common(Principal p,Model m){if(p!=null)m.addAttribute("student",learning.student(p.getName()));}
    @GetMapping("/journey") String journey(Principal p,Model m) {
        var s=learning.student(p.getName());if(!s.isPlacementCompleted())return "redirect:/exams/PLACEMENT";
        if(learning.canChooseMajor(s) && s.getMajor()==null)return "redirect:/onboarding";
        m.addAttribute("steps",workflow.steps(s));m.addAttribute("kinds",PracticeKind.values());
        var available=new EnumMap<PracticeKind,Boolean>(PracticeKind.class);
        var done=new EnumMap<PracticeKind,Boolean>(PracticeKind.class);
        for(var k:PracticeKind.values()){available.put(k,workflow.allowed(s,k));done.put(k,workflow.reviewed(s,k));}
        m.addAttribute("practiceAllowed",available);m.addAttribute("practiceDone",done);
        m.addAttribute("allDone",workflow.reviewed(s,PracticeKind.ADVANCED));return "journey";
    }
    @GetMapping("/exams/{stage}") String exam(Principal p,@PathVariable Stage stage,Model m) {
        var s=learning.student(p.getName());
        m.addAttribute("questions",workflow.questions(s,stage));m.addAttribute("stage",stage);
        m.addAttribute("material",catalog.lesson(stage,s.getMajor()));return (stage==Stage.PLACEMENT || stage==Stage.N5)?"placement-quiz":"exam";
    }
    @PostMapping("/exams/{stage}") String exam(Principal p,@PathVariable Stage stage,@RequestParam Map<String,String> answers,
                                             Model model,jakarta.servlet.http.HttpServletResponse http) {
        try {
            var result=workflow.submitExam(p.getName(),stage,answers);return "redirect:/results/"+result.getId();
        } catch(org.springframework.web.server.ResponseStatusException error) {
            if((stage!=Stage.PLACEMENT && stage!=Stage.N5) || error.getStatusCode().value()!=400)throw error;
            exam(p,stage,model);model.addAttribute("submittedAnswers",answers);model.addAttribute("quizError",error.getReason());
            http.setStatus(400);return "placement-quiz";
        }
    }
    @GetMapping("/results/{id}") String result(Principal p,@PathVariable Long id,Model m) {
        var attempt=workflow.attempt(learning.student(p.getName()),id);
        m.addAttribute("attempt",attempt);
        m.addAttribute("nextUrl",switch(attempt.getStage()) {
            case PLACEMENT,N5,N4 -> "/start";
            case N3,N2 -> "/language";
            case TECHNICAL -> attempt.isPassed()?"/specialized/READING":"/specialized/TERMS";
            case READING -> attempt.isPassed()?"/specialized/SITUATION":"/specialized/READING";
            case SITUATION -> "/specialized/SITUATION";
            case APPLICATION -> attempt.isPassed()?"/specialized/ADVANCED":"/specialized/APPLICATION";
            case ADVANCED -> "/specialized/ADVANCED";
        });
        if(attempt.getStage()==Stage.PLACEMENT){m.addAttribute("summary",results.summary(attempt));return "placement-result";}
        if(attempt.getStage()==Stage.N5){m.addAttribute("summary",results.summary(attempt));return "foundation-result";}
        return "result";
    }
    @GetMapping("/results/{id}/review") String reviewResult(Principal p,@PathVariable Long id,Model m){
        var attempt=workflow.attempt(learning.student(p.getName()),id);
        m.addAttribute("attempt",attempt);m.addAttribute("summary",results.summary(attempt));
        return "placement-review";
    }
    @GetMapping("/practice/{kind}") String practice(Principal p,@PathVariable PracticeKind kind,Model m) {
        var s=learning.student(p.getName());workflow.require(workflow.allowed(s,kind));
        m.addAttribute("kind",kind);m.addAttribute("prompt",catalog.prompt(kind,s.getMajor()));
        m.addAttribute("responseText",writing.text(s,kind));m.addAttribute("lastSubmission",writing.latest(s,kind));
        m.addAttribute("outline",content.outline(kind));m.addAttribute("checklist",content.checklist(kind));
        m.addAttribute("draft",drafts.findByStudentIdAndMajorAndKind(s.getId(),s.getMajor(),kind).orElse(null));
        return "practice";
    }
    @PostMapping("/practice/{kind}") String practice(Principal p,@PathVariable PracticeKind kind,
                                                    @RequestParam String response,RedirectAttributes flash,Model m,
                                                    jakarta.servlet.http.HttpServletResponse http) {
        try {workflow.submitPractice(p.getName(),kind,response);}
        catch(org.springframework.web.server.ResponseStatusException error) {
            if(error.getStatusCode().value()!=400)throw error;
            practice(p,kind,m);m.addAttribute("responseText",response);m.addAttribute("error",error.getReason());
            http.setStatus(400);return "practice";
        }
        flash.addFlashAttribute("message","Đã nộp bài. Giảng viên sẽ chấm theo rubric; xem phản hồi trong hồ sơ năng lực.");
        return "redirect:/competency";
    }
    @PostMapping("/practice/{kind}/draft") String saveDraft(Principal p,@PathVariable PracticeKind kind,
                                                           @RequestParam(defaultValue="") String response,RedirectAttributes flash) {
        writing.save(p.getName(),kind,response);flash.addFlashAttribute("message","Đã lưu bản nháp. Bạn có thể quay lại và tiếp tục viết.");
        return "redirect:/practice/"+kind;
    }
    @GetMapping("/competency") String competency(Principal p,Model m) {
        var s=learning.student(p.getName());
        m.addAttribute("attempts",workflow.history(s));m.addAttribute("submissions",workflow.submissions(s));
        m.addAttribute("steps",workflow.steps(s));m.addAttribute("allDone",workflow.reviewed(s,PracticeKind.ADVANCED));return "competency";
    }
    @GetMapping("/instructor/reviews") String reviews(Model m){m.addAttribute("submissions",workflow.pending());return "reviews";}
    @PostMapping("/instructor/reviews/{id}") String review(Principal p,@PathVariable Long id,
        @RequestParam int language,@RequestParam int technical,@RequestParam int communication,@RequestParam int task,
        @RequestParam String feedback,RedirectAttributes flash) {
        workflow.review(p.getName(),id,language,technical,communication,task,feedback);
        flash.addFlashAttribute("message","Đã lưu điểm và phản hồi.");return "redirect:/instructor/reviews";
    }
}
