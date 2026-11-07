package vn.sanka.lms.gamification;
import org.springframework.boot.*;
import org.springframework.stereotype.Component;
@Component
public class GamificationBootstrap implements ApplicationRunner {
    private final XpEventRepository events;private final GamificationService gamification;
    public GamificationBootstrap(XpEventRepository events,GamificationService gamification){this.events=events;this.gamification=gamification;}
    @Override public void run(ApplicationArguments args){events.activeStudents().forEach(gamification::reconcileExisting);}
}
