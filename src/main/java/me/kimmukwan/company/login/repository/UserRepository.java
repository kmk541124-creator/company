package me.kimmukwan.company.login.repository;

import me.kimmukwan.company.login.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    // 같은 아이디가 여러 개 저장돼 있어도 오류가 나지 않도록 가장 최근에 가입한 계정 1개만 가져옴
    Optional<User> findFirstByUsernameOrderByIdDesc(String username);

    // 회원가입 시 아이디 중복 확인용
    boolean existsByUsername(String username);

    // 에너지를 DB에서 바로 더함 — 작업 여러 개가 동시에 에너지를 올려도 값이 덮어써지지 않게 하기 위함
    @Modifying
    @Query("update User u set u.nowEnergy = u.nowEnergy + :amount where u.id = :id")
    void addEnergy(@Param("id") Long id, @Param("amount") int amount);
}
