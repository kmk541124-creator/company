package me.kimmukwan.company.worker.repository;

import me.kimmukwan.company.worker.domain.Worker;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WorkerRepository extends JpaRepository<Worker, Long> {

    // 특정 유저(user.id)가 고용한 직원들을 최근 고용 순으로 조회
    List<Worker> findByUserIdOrderByIdDesc(Long userId);
}
