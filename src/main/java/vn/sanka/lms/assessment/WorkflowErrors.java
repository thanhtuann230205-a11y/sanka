package vn.sanka.lms.assessment;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.ModelAndView;

@ControllerAdvice
public class WorkflowErrors {
    @ExceptionHandler(ResponseStatusException.class)
    ModelAndView workflowError(ResponseStatusException exception) {
        var view=new ModelAndView("workflow-error");
        view.setStatus(exception.getStatusCode());
        view.addObject("message",exception.getReason()==null?"Nội dung không tồn tại hoặc bạn chưa có quyền truy cập.":exception.getReason());
        return view;
    }
}
