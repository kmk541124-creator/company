package me.kimmukwan.company.monster.domain;


import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import me.kimmukwan.company.login.domain.User;

@Entity
@Table(name = "choice_monster")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChoiceMonster {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne
    @JoinColumn(name = "monster_id")
    private Monster monster;

    @Builder
    public ChoiceMonster(User user, Monster monster){
        this.user = user;
        this.monster = monster;
    }
}
