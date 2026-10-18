package me.kimmukwan.company.worker.domain;


import jakarta.persistence.*;
import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import me.kimmukwan.company.login.domain.User;

@Entity
@Table(name = "worker")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class Worker {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "name")
    private String name;

    @Column(name = "angry_stat")
    private int angryStat;

    @Column(name = "smartStat")
    private int smartStat;

    @Column(name = "loveStat")
    private int loveStat;

    @Column(name = "aukStat")
    private int aukStat;

    @Column(name = "hp")
    private int hp;

    @Column(name = "mind")
    private int mind;

    @Column(name = "attackSpeed")
    private int attackSpeed;

    @Column(name = "speed")
    private int speed;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @Builder
    public Worker(String name ,int angryStat, int smartStat, int loveStat, int aukStat, User user){
        this.angryStat = angryStat;
        this.smartStat = smartStat;
        this.loveStat = loveStat;
        this.aukStat = aukStat;
        this.hp = angryStat + (int)(angryStat*0.5);
        this.mind = smartStat + (int)(smartStat * 0.5);
        this.attackSpeed = aukStat + 50;
        this.speed = aukStat + 50;
        this.user = user;
        this.name = name;
    }

    // 최대 체력 — 생성자/reset과 같은 공식 (용기 × 1.5)
    public int getMaxHp(){
        return angryStat + (int)(angryStat * 0.5);
    }

    public void reset(){
        this.hp = angryStat + (int)(angryStat*0.5);
        this.mind = smartStat + (int)(smartStat * 0.5);
    }

    @Transactional
    public void countHp(int hp){
        this.hp -= hp;
    }
}
