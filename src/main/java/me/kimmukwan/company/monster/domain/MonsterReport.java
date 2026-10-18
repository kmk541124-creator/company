package me.kimmukwan.company.monster.domain;


import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "monster_report")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MonsterReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "info", columnDefinition = "TEXT")
    private String info;

    @Column(name = "count")
    private int count;

    @ManyToOne
    @JoinColumn(name = "monster_id")
    private Monster monster;

    @Builder
    public MonsterReport(String info, int count, Monster monster){
        this.info = info;
        this.count = count;
        this.monster = monster;
    }
}
