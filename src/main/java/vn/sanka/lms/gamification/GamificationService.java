package vn.sanka.lms.gamification;

import jakarta.persistence.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import vn.sanka.lms.auth.*;
import vn.sanka.lms.learning.*;
import vn.sanka.lms.assessment.*;
import java.time.*;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class GamificationService {
    private static final ZoneId ZONE=ZoneId.of("Asia/Ho_Chi_Minh");
    private final StudentRepository students;
    private final XpEventRepository events;
    private final MotivationProfileRepository profiles;
    private final DailyGoalRepository goals;
    private final BadgeRepository badges;
    private final Clock clock;
    @PersistenceContext private EntityManager em;
    public GamificationService(StudentRepository students,XpEventRepository events,MotivationProfileRepository profiles,
                               DailyGoalRepository goals,BadgeRepository badges,Clock clock){
        this.students=students;this.events=events;this.profiles=profiles;this.goals=goals;this.badges=badges;this.clock=clock;
    }
    public LocalDateTime now(){return LocalDateTime.now(clock.withZone(ZONE));}
    public LocalDate today(){return now().toLocalDate();}
    public Student student(String email){return students.findByEmail(email).orElseThrow(()->new ResponseStatusException(HttpStatus.UNAUTHORIZED));}
    public MotivationProfile profile(Long studentId){return profiles.findByStudentId(studentId).orElseGet(()->new MotivationProfile(studentId));}
    public List<XpEvent> history(Long studentId){return events.findByStudentIdOrderByOccurredAtDescIdDesc(studentId);}
    private Student lock(Long studentId){return students.findById(studentId).map(s->{em.lock(s,LockModeType.PESSIMISTIC_WRITE);return s;})
        .orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));}

    // Rewards are produced only by trusted application services, never request parameters.
    @Transactional public void lesson(Long studentId,Long lessonId){record(studentId,"lesson:"+lessonId,ActivityKind.LESSON,20,now());}
    @Transactional public void exam(Long studentId,Stage stage,Major major,boolean passed){
        if(stage==Stage.PLACEMENT){record(studentId,"placement",ActivityKind.PLACEMENT,20,now());return;}
        String scope=List.of(Stage.N5,Stage.N4,Stage.N3,Stage.N2).contains(stage)?"language":major.name();
        record(studentId,(passed?"exam:":"exam-practice:")+stage+":"+scope+(passed?"":":"+today()),
            passed?ActivityKind.EXAM_PASS:ActivityKind.EXAM_PRACTICE,passed?40:0,now());
    }
    @Transactional public void submitted(Long studentId,String context){record(studentId,"submit:"+context+":"+today(),ActivityKind.SUBMISSION,0,now());}
    @Transactional public void practicePassed(Long studentId,PracticeKind kind,Major major,LocalDateTime submittedAt){
        record(studentId,"practice:"+kind+":"+major,ActivityKind.PRACTICE_PASS,50,submittedAt);
    }
    @Transactional public void languagePassed(Long studentId,Level level,Major major,LocalDateTime submittedAt){
        record(studentId,"language-application:"+level+":"+major,ActivityKind.LANGUAGE_PASS,40,submittedAt);
    }
    @Transactional public void periodic(Long studentId,Level level){
        var monday=today().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        record(studentId,"periodic:"+level+":"+monday,ActivityKind.PERIODIC,15,now());
    }
    private void record(Long studentId,String key,ActivityKind kind,int points,LocalDateTime activityTime){
        var student=lock(studentId);
        if(!"STUDENT".equals(student.getRole()) || events.existsByStudentIdAndEventKey(studentId,key))return;
        events.save(new XpEvent(studentId,key,kind,points,activityTime));
        var day=activityTime.toLocalDate();
        if(goals.findByStudentIdAndActivityDate(studentId,day).isEmpty())
            goals.save(new DailyGoal(studentId,day,day.equals(today())?profile(studentId).getTargetXp():40));
        reconcileBadges(studentId);
    }

    public record Overview(long totalXp,long todayXp,int targetXp,long goalPercent,boolean goalReached,
                           int currentStreak,int bestStreak,int activeDays){}
    public Overview overview(Long studentId){return overview(studentId,history(studentId));}
    private Overview overview(Long studentId,List<XpEvent> all){
        long total=all.stream().mapToLong(XpEvent::getPoints).sum();
        long daily=all.stream().filter(e->e.getActivityDate().equals(today())).mapToLong(XpEvent::getPoints).sum();
        int target=goals.findByStudentIdAndActivityDate(studentId,today()).map(DailyGoal::getTargetXp).orElse(profile(studentId).getTargetXp());
        var dates=all.stream().map(XpEvent::getActivityDate).filter(d->!d.isAfter(today())).collect(Collectors.toCollection(TreeSet::new));
        int current=0,best=0,run=0;LocalDate previous=null;
        for(var date:dates){run=previous!=null && previous.plusDays(1).equals(date)?run+1:1;best=Math.max(best,run);previous=date;}
        var cursor=dates.contains(today())?today():today().minusDays(1);
        while(dates.contains(cursor)){current++;cursor=cursor.minusDays(1);}
        return new Overview(total,daily,target,Math.min(100,daily*100/target),daily>=target,current,best,dates.size());
    }
    @Transactional public void setGoal(Long studentId,int target){
        if(target<20||target>200)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Mục tiêu ngày từ 20 đến 200 XP");
        lock(studentId);var profile=profile(studentId);profile.target(target);profiles.save(profile);
        var goal=goals.findByStudentIdAndActivityDate(studentId,today()).orElseGet(()->new DailyGoal(studentId,today(),target));
        goal.target(target);goals.save(goal);reconcileBadges(studentId);
    }
    @Transactional public void rankingPreference(Long studentId,boolean enabled,String nickname){
        var s=lock(studentId);
        if(!"STUDENT".equals(s.getRole()))throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Bảng xếp hạng dành cho sinh viên");
        String alias=nickname==null?"":nickname.strip();
        if(enabled && (alias.length()<2||alias.length()>30||alias.contains("<")||alias.contains(">")||alias.contains("@")||alias.chars().anyMatch(Character::isISOControl)))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Bí danh từ 2–30 ký tự, không chứa @, dấu < > hoặc ký tự điều khiển");
        var profile=profile(studentId);profile.ranking(enabled,enabled?alias:profile.getNickname());profiles.save(profile);
    }

    public record BadgeView(String code,String icon,String title,String description,long progress,long target,
                            boolean earned,LocalDateTime earnedAt){}
    public List<BadgeView> badgeViews(Long studentId){return badgeViews(studentId,history(studentId));}
    private List<BadgeView> badgeViews(Long studentId,List<XpEvent> all){
        var earned=badges.findByStudentId(studentId).stream().collect(Collectors.toMap(EarnedBadge::getCode,b->b));
        var summary=overview(studentId,all);
        long lessons=all.stream().filter(e->e.getKind()==ActivityKind.LESSON).count();
        long exams=all.stream().filter(e->e.getKind()==ActivityKind.EXAM_PASS).count();
        long practice=all.stream().filter(e->e.getKind()==ActivityKind.PRACTICE_PASS||e.getKind()==ActivityKind.LANGUAGE_PASS).count();
        long periodic=all.stream().filter(e->e.getKind()==ActivityKind.PERIODIC).count();
        var perDay=all.stream().collect(Collectors.groupingBy(XpEvent::getActivityDate,Collectors.summingLong(XpEvent::getPoints)));
        boolean reached=goals.findByStudentId(studentId).stream().anyMatch(g->perDay.getOrDefault(g.getActivityDate(),0L)>=g.getTargetXp());
        return List.of(
            badge(earned,"FIRST_LESSON","🌱","Bước đầu tiên","Hoàn thành bài học đầu tiên",lessons,1),
            badge(earned,"FIVE_LESSONS","📚","Học đều, tiến vững","Hoàn thành 5 bài học",lessons,5),
            badge(earned,"STREAK_3","🔥","Ba ngày liên tiếp","Có chuỗi học 3 ngày",summary.bestStreak(),3),
            badge(earned,"STREAK_7","七","Một tuần bền bỉ","Có chuỗi học 7 ngày",summary.bestStreak(),7),
            badge(earned,"XP_100","✦","100 XP đầu tiên","Tích lũy 100 XP",summary.totalXp(),100),
            badge(earned,"XP_500","⭐","500 XP học tập","Tích lũy 500 XP",summary.totalXp(),500),
            badge(earned,"FIRST_EXAM","✓","Vượt một cột mốc","Đạt bài kiểm tra đầu tiên (không tính đầu vào)",exams,1),
            badge(earned,"FIRST_PRACTICE","文","Ứng dụng vào công việc","Có bài thực hành được giảng viên chấm đạt",practice,1),
            badge(earned,"FIRST_REVIEW","↻","Nhớ bằng ôn tập","Hoàn thành một lần ôn định kỳ",periodic,1),
            badge(earned,"FIRST_GOAL","🎯","Đúng mục tiêu","Đạt mục tiêu XP của một ngày",reached?1:0,1));
    }
    private BadgeView badge(Map<String,EarnedBadge> earned,String code,String icon,String title,String description,long progress,long target){
        var badge=earned.get(code);return new BadgeView(code,icon,title,description,Math.min(progress,target),target,badge!=null,badge==null?null:badge.getEarnedAt());
    }
    private void reconcileBadges(Long studentId){
        for(var badge:badgeViews(studentId,history(studentId)))if(!badge.earned()&&badge.progress()>=badge.target())
            badges.save(new EarnedBadge(studentId,badge.code(),now()));
    }
    @Transactional public void reconcileExisting(Long studentId){lock(studentId);reconcileBadges(studentId);}

    public record CalendarDay(LocalDate date,boolean inMonth,boolean today,long xp,int activities,boolean goalReached,int intensity){}
    public List<CalendarDay> calendar(Long studentId,YearMonth month){
        var perDay=history(studentId).stream().collect(Collectors.groupingBy(XpEvent::getActivityDate));
        var targets=goals.findByStudentId(studentId).stream().collect(Collectors.toMap(DailyGoal::getActivityDate,DailyGoal::getTargetXp));
        var start=month.atDay(1).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        var days=new ArrayList<CalendarDay>();
        for(int i=0;i<42;i++){
            var date=start.plusDays(i);var daily=perDay.getOrDefault(date,List.of());long xp=daily.stream().mapToLong(XpEvent::getPoints).sum();
            days.add(new CalendarDay(date,YearMonth.from(date).equals(month),date.equals(today()),xp,daily.size(),
                !daily.isEmpty()&&xp>=targets.getOrDefault(date,40),daily.isEmpty()?0:xp>=100?4:xp>=50?3:xp>=20?2:1));
        }return days;
    }
    public record Rank(int rank,Long studentId,String nickname,Major major,Level level,long xp,boolean own){}
    public List<Rank> leaderboard(Long viewerId,String period,Major major,Level level){
        if(!List.of("week","all").contains(period))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Chọn tuần này hoặc toàn thời gian");
        var since=period.equals("week")?today().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)):LocalDate.of(1970,1,1);
        var rows=events.ranking(since,today(),major,level);var result=new ArrayList<Rank>();long previous=-1;int rank=0;
        for(int i=0;i<Math.min(50,rows.size());i++){
            var row=rows.get(i);long xp=row.getXp();if(xp!=previous)rank=i+1;
            result.add(new Rank(rank,row.getStudentId(),row.getNickname(),row.getMajor(),row.getLevel(),xp,row.getStudentId().equals(viewerId)));previous=xp;
        }return result;
    }
}
