package vn.sanka.lms.gamification;

import jakarta.persistence.*;

@Entity @Table(name="motivation_profiles")
public class MotivationProfile {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(nullable=false,unique=true) private Long studentId;
    @Column(nullable=false) private int targetXp=40;
    @Column(nullable=false) private boolean leaderboardEnabled;
    @Column(nullable=false,length=30) private String nickname;
    protected MotivationProfile(){}
    public MotivationProfile(Long studentId){this.studentId=studentId;this.nickname="Học viên #"+studentId;}
    public int getTargetXp(){return targetXp;}
    public boolean isLeaderboardEnabled(){return leaderboardEnabled;}
    public String getNickname(){return nickname;}
    public void target(int targetXp){this.targetXp=targetXp;}
    public void ranking(boolean enabled,String nickname){this.leaderboardEnabled=enabled;this.nickname=nickname;}
}
