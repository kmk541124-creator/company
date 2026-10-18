package me.kimmukwan.company.login.domain;

import jakarta.persistence.*;
import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import me.kimmukwan.company.main.domain.GamePhase;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@Table(name = "users")
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "username")
    private String username;

    @Column(name = "password")
    private String password;

    @Column(name = "level")
    private int level;

    // 현재 날짜(게임 내 일차) — 새 DB에서는 기본값 1로 만들어짐
    @Column(name = "day", columnDefinition = "integer not null default 1")
    private int day;

    // 보유 LOB 포인트 — 직원 스탯을 올릴 때 소모되는 재화 (기존 회원 행에도 기본값 100이 들어가도록 DB 기본값 지정)
    @Column(name = "lob", columnDefinition = "integer not null default 100")
    private int lob;

    // 최대 에너지 — 새 DB에서는 기본값 70(60 + 1일차 × 10)으로 만들어짐
    @Column(name = "max_energy", columnDefinition = "integer not null default 70")
    private int maxEnergy;

    // 현재 에너지 — 새 DB에서는 기본값 0으로 만들어짐
    @Column(name = "now_energy", columnDefinition = "integer not null default 0")
    private int nowEnergy;

    // 현재 진행 단계 — 단계마다 할 수 있는 일이 정해져 있음 (기존 회원 행에는 기본값 START가 들어감)
    @Enumerated(EnumType.STRING)
    @Column(name = "phase", columnDefinition = "varchar(20) not null default 'START'")
    private GamePhase phase;

    @Builder
    public User(String username, String password){
        this.username = username;
        this.password = password;
        this.level = 1;
        this.lob = 100; // 회원가입 시 시작 LOB 포인트
        this.day = 1;
        this.maxEnergy = 60 + day*10;
        this.nowEnergy = 0;
        this.phase = GamePhase.START;
    }

    // 지금 단계가 expected가 아니면 예외 — 단계에 맞지 않는 행동(업무 중 고용 등)을 서버에서 막음
    public void checkPhase(GamePhase expected){
        if(this.phase != expected){
            throw new IllegalStateException("지금은 '" + phaseLabel() + "' 단계라서 '" + expected.getLabel() + "' 단계의 행동을 할 수 없습니다.");
        }
    }

    public void changePhase(GamePhase phase){
        this.phase = phase;
    }

    // 에너지 할당량을 다 채웠는지 — 다 채워야 다음 날로 넘어갈 수 있음
    public boolean isEnergyFull(){
        return nowEnergy >= maxEnergy;
    }

    private String phaseLabel(){
        return phase == null ? GamePhase.START.getLabel() : phase.getLabel();
    }

    // LOB 포인트를 amount만큼 사용 — 부족하면 예외를 던져서 고용 자체를 취소시킴
    public void useLob(int amount){
        if(amount < 0){
            throw new IllegalArgumentException("사용할 LOB 포인트가 올바르지 않습니다.");
        }
        if(this.lob < amount){
            throw new IllegalStateException("LOB 포인트가 부족합니다. (필요 " + amount + " / 보유 " + this.lob + ")");
        }
        this.lob -= amount;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities(){
        return List.of(new SimpleGrantedAuthority("user"));
    }

    @Override
    public String getUsername(){
        return username;
    }

    @Override
    public String getPassword(){
        return password;
    }

    @Override
    public boolean isAccountNonExpired(){
        return true;
    }

    @Override
    public boolean isAccountNonLocked(){
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired(){
        return true;
    }

    @Override
    public boolean isEnabled(){
        return true;
    }

    @Transactional
    public void upDay(){
        day++;
        this.maxEnergy = 60 + day * 10; // 생성자, UserDataFixer와 같은 공식 (60 + 일차 × 10)
    }

    @Transactional
    public void plusEnergy(int energy){
        this.nowEnergy += energy;
    }

    @Transactional
    public void reset(){
        this.nowEnergy = 0;
    }
}
