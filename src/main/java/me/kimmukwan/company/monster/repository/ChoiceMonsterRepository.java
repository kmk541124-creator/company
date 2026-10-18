package me.kimmukwan.company.monster.repository;

import me.kimmukwan.company.login.domain.User;
import me.kimmukwan.company.monster.domain.ChoiceMonster;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChoiceMonsterRepository extends JpaRepository<ChoiceMonster,Long> {

    List<ChoiceMonster> findByUser(User user);

    void deleteByUser(User user);
}
