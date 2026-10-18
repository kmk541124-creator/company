package me.kimmukwan.company.monster.domain;


import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "monster")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Monster {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "name")
    private String name;

    @Column(name = "grade")
    private String grade;

    @Column(name = "angry")
    private int angry;

    @Column(name = "smart")
    private int smart;

    @Column(name = "love")
    private int love;

    @Column(name = "auk")
    private int auk;

    @Column(name = "code")
    private String code;

    @Column(name = "escape")
    private Boolean escape;

    @Column(name = "count")
    private int count;

    @Column(name = "energy")
    private int energy;

    @Column(name = "good")
    private int good;

    @Column(name = "normal")
    private int normal;

    @Column(name = "bad")
    private int bad;

    // 작업 실패 시 직원이 입는 피해량 — 새 DB에서는 기본값 0으로 만들어짐
    @Column(name = "damage", columnDefinition = "integer not null default 0")
    private int damage;

    @Builder
    public Monster(String name, String grade, int angry, int smart, int love, int auk,
                   String code, Boolean escape, int count, int energy, int good,
                   int normal, int bad, int damage){
        this.name = name;
        this.grade = grade;
        this.angry = angry;
        this.smart = smart;
        this.love = love;
        this.auk = auk;
        this.code = code;
        this.escape = escape;
        this.count = count;
        this.energy = energy;
        this.good = good;
        this.normal = normal;
        this.bad = bad;
        this.damage = damage;
    }
}
