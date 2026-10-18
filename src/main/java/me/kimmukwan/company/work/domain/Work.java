package me.kimmukwan.company.work.domain;


import jakarta.persistence.*;
import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import me.kimmukwan.company.login.domain.User;
import me.kimmukwan.company.monster.domain.UserMonster;
import me.kimmukwan.company.worker.domain.Worker;

@Entity
@Table(name = "work")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Work {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "good")
    private int good;

    @Column(name = "bad")
    private int bad;

    @ManyToOne
    @JoinColumn(name = "user_monster_id")
    private UserMonster userMonster;

    @ManyToOne
    @JoinColumn(name = "user_worker_id")
    private Worker worker;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "type")
    private String type;

    @Column(name = "result")
    private String result;

    @Column(name = "count")
    private int count;

    // 실패했을 때 이유 (성공했으면 null)
    @Column(name = "reason")
    private String reason;

    // 카운터가 0이 되어 괴물이 탈출했는지
    @Column(name = "escaped", columnDefinition = "boolean not null default false")
    private boolean escaped;

    @Builder
    public Work(int good, int bad, UserMonster userMonster , Worker worker,
                User user, String type, String result, int count){
        this.good = good;
        this.bad = bad;
        this.userMonster = userMonster;
        this.worker = worker;
        this.user = user;
        this.type = type;
        this.result = result;
        this.count = count;
    }

    @Transactional
    public void upGood(){
        good++;
    }

    @Transactional
    public void upBad(){
        bad++;
    }

    // 실패로 끝냄 (이유와 함께 저장)
    public void fail(String reason){
        this.result = "실패";
        this.reason = reason;
    }

    // 오류 등으로 중간에 멈춤
    public void stop(String reason){
        this.result = "중단";
        this.reason = reason;
    }

    public void markEscaped(){
        this.escaped = true;
    }

    // 진행 중인지 — "진행중"이 아니면 이미 끝난 작업
    public boolean isRunning(){
        return "진행중".equals(result);
    }

    // 지금까지 판정한 횟수 (성공 + 실패)
    public int getTicks(){
        return good + bad;
    }

    // 작업 판정에 실패할 때마다 카운터를 1 줄임
    public void downCount(){
        count--;
    }

    @Transactional
    public void setResult(String result){
        this.result = result;
    }
}
