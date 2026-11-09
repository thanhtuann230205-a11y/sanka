package vn.sanka.lms.gamification;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import vn.sanka.lms.learning.*;
import java.security.Principal;
import java.time.*;

@Controller
public class MotivationController {
    private final GamificationService gamification;
    public MotivationController(GamificationService gamification){this.gamification=gamification;}
    @ModelAttribute void common(Principal p,Model m){if(p!=null)m.addAttribute("student",gamification.student(p.getName()));}
    @GetMapping("/motivation") String motivation(Principal p,@RequestParam(defaultValue="") String month,
        @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate day,Model m){
        var s=gamification.student(p.getName());YearMonth selected;
        try{selected=month.isBlank()?YearMonth.from(gamification.today()):YearMonth.parse(month);
            if(selected.getYear()<1900||selected.getYear()>2200)throw new IllegalArgumentException();}
        catch(Exception e){throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Tháng phải có dạng YYYY-MM, trong khoảng 1900–2200");}
        var date=day!=null?day:selected.equals(YearMonth.from(gamification.today()))?gamification.today():selected.atDay(1);
        m.addAttribute("motivation",gamification.overview(s.getId()));m.addAttribute("badges",gamification.badgeViews(s.getId()));
        m.addAttribute("calendar",gamification.calendar(s.getId(),selected));m.addAttribute("month",selected);
        m.addAttribute("previousMonth",selected.equals(YearMonth.of(1900,1))?null:selected.minusMonths(1));
        m.addAttribute("nextMonth",selected.equals(YearMonth.of(2200,12))?null:selected.plusMonths(1));
        m.addAttribute("selectedDay",date);m.addAttribute("dailyEvents",gamification.history(s.getId()).stream().filter(e->e.getActivityDate().equals(date)).toList());
        m.addAttribute("profile",gamification.profile(s.getId()));m.addAttribute("editDailyGoal",true);return "motivation";
    }
    @PostMapping("/motivation/goal") String goal(Principal p,@RequestParam int targetXp,RedirectAttributes flash){
        gamification.setGoal(gamification.student(p.getName()).getId(),targetXp);
        flash.addFlashAttribute("message","Đã lưu mục tiêu cho hôm nay và các ngày tiếp theo. Mục tiêu của ngày cũ được giữ nguyên.");return "redirect:/motivation";
    }
    @GetMapping("/leaderboard") String leaderboard(Principal p,@RequestParam(defaultValue="week") String period,
        @RequestParam(required=false) Major major,@RequestParam(required=false) Level level,Model m){
        var s=gamification.student(p.getName());
        m.addAttribute("ranking",gamification.leaderboard(s.getId(),period,major,level));
        var weekStart=gamification.today().with(java.time.temporal.TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        m.addAttribute("weekStart",weekStart);m.addAttribute("weekEnd",weekStart.plusDays(6));
        m.addAttribute("period",period);m.addAttribute("selectedMajor",major);m.addAttribute("selectedLevel",level);
        m.addAttribute("majors",Major.values());m.addAttribute("levels",Level.values());m.addAttribute("profile",gamification.profile(s.getId()));
        return "leaderboard";
    }
    @PostMapping("/leaderboard/preferences") String ranking(Principal p,@RequestParam(defaultValue="false") boolean enabled,
        @RequestParam(defaultValue="") String nickname,RedirectAttributes flash){
        gamification.rankingPreference(gamification.student(p.getName()).getId(),enabled,nickname);
        flash.addFlashAttribute("message",enabled?"Bạn đã tham gia bảng xếp hạng bằng bí danh đã chọn.":"Bạn đã rút khỏi bảng xếp hạng.");
        return "redirect:/leaderboard";
    }
}
