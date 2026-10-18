package me.kimmukwan.company.work.dto;

import me.kimmukwan.company.monster.domain.Monster;
import me.kimmukwan.company.work.domain.Work;
import me.kimmukwan.company.worker.domain.Worker;

// 작업 진행 상황 / 결과 — 작업 화면이 0.5초마다 이 값을 받아서 숫자를 실시간으로 바꿈
// Work 엔티티를 그대로 보내면 연결된 User(비밀번호 해시 포함)까지 JSON에 실리므로, 필요한 값만 골라 담는다
public record WorkResponse(
        Long id,              // 작업 기록 번호 — 진행 상황 조회 주소(/api/work/{id})에 쓰임
        Long userMonsterId,   // 작업 중인 괴물(UserMonster) 번호
        String monsterName,   // 괴물 이름
        Long workerId,        // 작업 중인 직원 번호
        String workerName,    // 직원 이름
        String type,          // 작업 종류 (angry / smart / love / auk)
        String result,        // 진행중 / 좋음 / 보통 / 나쁨 / 실패 / 중단
        String reason,        // 실패·중단 이유 (아니면 null)
        int good,             // 지금까지 성공한 횟수
        int bad,              // 지금까지 실패한 횟수
        int total,            // 전체 판정 횟수 (괴물의 에너지 수치)
        int count,            // 남은 카운터
        int workerHp,         // 직원의 현재 체력
        int workerMaxHp,      // 직원의 최대 체력 (체력 막대 계산용)
        int gainEnergy,       // 이 작업으로 지금까지 생산한 에너지 (성공 1회 = 에너지 1)
        int nowEnergy,        // 유저가 오늘 모은 에너지
        int maxEnergy,        // 오늘의 에너지 할당량
        boolean escaped,      // 괴물이 탈출했는지
        boolean done          // 작업이 끝났는지
) {
    public static WorkResponse from(Work work){
        Monster monster = work.getUserMonster().getMonster();
        Worker worker = work.getWorker();
        return new WorkResponse(
                work.getId(), work.getUserMonster().getId(), monster.getName(),
                worker.getId(), worker.getName(), work.getType(), work.getResult(), work.getReason(),
                work.getGood(), work.getBad(), monster.getEnergy(), work.getCount(),
                Math.max(0, worker.getHp()), worker.getMaxHp(),
                work.getGood(), work.getUser().getNowEnergy(), work.getUser().getMaxEnergy(),
                work.isEscaped(), !work.isRunning());
    }
}
