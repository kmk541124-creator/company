package me.kimmukwan.company.monster.repository;

import me.kimmukwan.company.monster.domain.Monster;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MonsterRepository extends JpaRepository<Monster, Long> {

    Optional<Monster> findByName(String name);
}
