package me.kimmukwan.company.company.domain;


import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import me.kimmukwan.company.login.domain.User;

@Entity
@Table(name="company")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Company {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "location")
    private String location;

    @Builder
    public Company(User user, String location){
        this.user = user;
        this.location = location;
    }
}
