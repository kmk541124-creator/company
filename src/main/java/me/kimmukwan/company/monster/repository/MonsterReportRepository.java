package me.kimmukwan.company.monster.repository;

import me.kimmukwan.company.monster.domain.MonsterReport;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MonsterReportRepository extends JpaRepository<MonsterReport, Long> {
}
