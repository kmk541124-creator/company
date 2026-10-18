package me.kimmukwan.company.worker.dto;

import me.kimmukwan.company.worker.domain.Worker;

// 직원 정보를 화면에 돌려줄 때 쓰는 응답 객체
// Worker 엔티티를 그대로 보내면 연결된 User(비밀번호 해시 포함)까지 JSON에 실리므로, 필요한 값만 골라 담는다
public record WorkerResponse(
        Long id,          // 직원 고유 번호
        String name,      // 직원 이름
        int angryStat,    // 용기 수치 (단계 × 20)
        int smartStat,    // 통찰 수치 (단계 × 20)
        int loveStat,     // 절제 수치 (단계 × 20)
        int aukStat,      // 정의 수치 (단계 × 20)
        int hp,           // 체력 — 용기에서 계산됨
        int mind,         // 정신력 — 통찰에서 계산됨
        int attackSpeed,  // 공격 속도 — 정의에서 계산됨
        int speed         // 이동 속도 — 정의에서 계산됨
) {
    // 엔티티 → 응답 객체 변환
    public static WorkerResponse from(Worker worker){
        return new WorkerResponse(
                worker.getId(), worker.getName(),
                worker.getAngryStat(), worker.getSmartStat(), worker.getLoveStat(), worker.getAukStat(),
                worker.getHp(), worker.getMind(), worker.getAttackSpeed(), worker.getSpeed());
    }
}
