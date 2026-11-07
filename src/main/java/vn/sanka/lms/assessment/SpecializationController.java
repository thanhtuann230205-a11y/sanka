package vn.sanka.lms.assessment;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import vn.sanka.lms.learning.LearningService;
import java.security.Principal;
import java.util.*;

@Controller
public class SpecializationController {
    private final LearningService learning;
    private final WorkflowService workflow;
    private final SpecializationService specialization;
    private final LearningCatalog catalog;
    private final SpecializationContent content;
    public SpecializationController(LearningService learning,WorkflowService workflow,SpecializationService specialization,
                                    LearningCatalog catalog,SpecializationContent content) {
        this.learning=learning;this.workflow=workflow;this.specialization=specialization;this.catalog=catalog;this.content=content;
    }
    @ModelAttribute void common(Principal p,Model m){if(p!=null)m.addAttribute("student",learning.student(p.getName()));}
    @GetMapping("/specialized") String hub(Principal p,Model m) {
        var s=learning.student(p.getName());if(s.getMajor()==null)return "redirect:/onboarding";
        var steps=specialization.steps(s);
        m.addAttribute("entered",specialization.entered(s));m.addAttribute("steps",steps);
        long completed=steps.stream().filter(step->"COMPLETED".equals(step.status())).count();
        m.addAttribute("completedCount",completed);m.addAttribute("percent",completed*100/steps.size());
        m.addAttribute("nextStep",steps.stream().filter(step->!"COMPLETED".equals(step.status())).findFirst().orElse(null));
        return "specialized";
    }
    @GetMapping("/specialized/{phase}") String resource(Principal p,@PathVariable SpecializedPhase phase,
                                                      @RequestParam(defaultValue="false") boolean savedOnly,Model m) {
        var s=learning.student(p.getName());workflow.require(specialization.open(s,phase));
        m.addAttribute("phase",phase);m.addAttribute("topic",catalog.topic(s.getMajor()));
        m.addAttribute("translation",content.translation(s.getMajor()));
        m.addAttribute("dialogue",content.dialogue(s.getMajor()));
        m.addAttribute("tasks",specialization.tasks(s,phase));
        m.addAttribute("examAllowed",phase.getExam()!=null && workflow.allowed(s,phase.getExam()));
        m.addAttribute("examPassed",phase.getExam()!=null && workflow.passed(s,phase.getExam()));
        m.addAttribute("material",phase.getExam()==null?"":catalog.lesson(phase.getExam(),s.getMajor()));
        if(phase==SpecializedPhase.TERMS) {
            m.addAttribute("terms",specialization.terms(s,savedOnly));m.addAttribute("saved",specialization.saved(s));
            m.addAttribute("savedOnly",savedOnly);
            m.addAttribute("introductions",learning.currentCourse(s));m.addAttribute("completed",learning.completed(s));
        }
        if(phase==SpecializedPhase.APPLICATION || phase==SpecializedPhase.ADVANCED) {
            m.addAttribute("skills",specialization.skills(s));
            m.addAttribute("evidence",workflow.submissions(s).stream().filter(a->a.getMajor()==s.getMajor() && a.getReviewedAt()!=null).toList());
            m.addAttribute("attempts",workflow.history(s).stream().filter(a->a.getMajor()==s.getMajor()).toList());
        }
        return "specialized-resource";
    }
    @PostMapping("/specialized/terms/{id}/bookmark") String bookmark(Principal p,@PathVariable Long id,
        @RequestParam boolean saved,@RequestParam(defaultValue="false") boolean savedOnly) {
        specialization.bookmark(p.getName(),id,saved);
        return "redirect:/specialized/TERMS"+(savedOnly?"?savedOnly=true":"");
    }
}
