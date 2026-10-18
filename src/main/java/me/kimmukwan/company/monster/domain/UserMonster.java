package me.kimmukwan.company.monster.domain;


import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import me.kimmukwan.company.company.domain.Company;

@Entity
@Table(name = "user_monster")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserMonster {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "monster_id")
    private Monster monster;

    @ManyToOne
    @JoinColumn(name = "company_id")
    private Company company;

    @Builder
    public UserMonster(Monster monster, Company company) {
        this.monster = monster;
        this.company = company;
    }
}
