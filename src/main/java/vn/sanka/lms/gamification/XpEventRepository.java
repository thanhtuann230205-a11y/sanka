package vn.sanka.lms.gamification;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import vn.sanka.lms.learning.*;
import java.time.LocalDate;
import java.util.*;

public interface XpEventRepository extends JpaRepository<XpEvent,Long> {
    boolean existsByStudentIdAndEventKey(Long studentId,String key);
    List<XpEvent> findByStudentIdOrderByOccurredAtDescIdDesc(Long studentId);
    @Query("select distinct e.studentId from XpEvent e") List<Long> activeStudents();
    interface Ranking {
        Long getStudentId();String getNickname();Major getMajor();Level getLevel();Long getXp();
    }
    @Query("""
        select s.id as studentId, p.nickname as nickname, s.major as major, s.level as level,
               coalesce(sum(e.points),0) as xp
        from Student s join MotivationProfile p on p.studentId=s.id
        left join XpEvent e on e.studentId=s.id and e.activityDate>=:since and e.activityDate<=:today
        where p.leaderboardEnabled=true and s.role='STUDENT'
          and (:major is null or s.major=:major) and (:level is null or s.level=:level)
        group by s.id,p.nickname,s.major,s.level
        order by coalesce(sum(e.points),0) desc,s.id asc
        """)
    List<Ranking> ranking(@Param("since") LocalDate since,@Param("today") LocalDate today,
                          @Param("major") Major major,@Param("level") Level level);
}
