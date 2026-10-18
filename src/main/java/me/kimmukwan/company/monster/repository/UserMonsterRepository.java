package me.kimmukwan.company.monster.repository;

import me.kimmukwan.company.company.domain.Company;
import me.kimmukwan.company.monster.domain.UserMonster;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserMonsterRepository extends JpaRepository<UserMonster, Long> {

    List<UserMonster> findByCompany(Company company);
}
