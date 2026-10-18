package me.kimmukwan.company.work.repository;

import me.kimmukwan.company.work.domain.Work;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface WorkRepository extends JpaRepository<Work, Long> {

    // 이 직원이 지금 작업 중인지 (한 직원은 한 번에 한 작업만)
    boolean existsByWorkerIdAndResult(Long workerId, String result);

    // 이 괴물이 지금 작업 받는 중인지 (한 괴물은 한 번에 한 작업만)
    boolean existsByUserMonsterIdAndResult(Long userMonsterId, String result);

    // 이 유저에게 진행 중인 작업이 남아 있는지 (남아 있으면 다음 날로 못 넘어감)
    boolean existsByUserIdAndResult(Long userId, String result);

    // 진행 중인 작업 목록 — 작업 화면을 새로고침해도 진행 상황을 다시 이어서 보여주기 위함
    List<Work> findByUserIdAndResultOrderByIdAsc(Long userId, String result);

    // 서버가 꺼지면서 멈춘 작업들의 결과를 한 번에 바꿈
    @Modifying
    @Query("update Work w set w.result = :toResult, w.reason = :reason where w.result = :fromResult")
    int changeResult(@Param("fromResult") String fromResult, @Param("toResult") String toResult, @Param("reason") String reason);
}
