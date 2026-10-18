package me.kimmukwan.company.work;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

// 작업 판정을 요청과 따로(백그라운드 스레드에서) 1초마다 돌려주는 클래스
// 요청 안에서 Thread.sleep으로 기다리면 끝날 때까지 화면이 아무것도 받지 못하므로,
// 판정은 여기서 돌리고 화면은 /api/work/{id}로 진행 상황을 계속 물어봄
@Slf4j
@Component
@RequiredArgsConstructor
public class WorkRunner {

    public static final long TICK_MILLIS = 1000; // 판정 1회마다 기다리는 시간 (1초)

    private final WorkService workService; // 판정 1회는 WorkService.tick이 트랜잭션 안에서 처리

    @Async // 이 메서드는 별도 스레드에서 실행됨 (CompanyApplication의 @EnableAsync 필요)
    public void run(Long workId){
        try {
            boolean done = false;
            while(!done){
                Thread.sleep(TICK_MILLIS);
                done = workService.tick(workId); // 판정 1회마다 DB에 저장 → 화면이 바로 읽을 수 있음
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            workService.stop(workId, "작업이 중단되었습니다.");
        } catch (Exception e) {
            log.error("작업 {} 진행 중 오류", workId, e);
            workService.stop(workId, "작업 중 오류가 발생했습니다: " + e.getMessage());
        }
    }
}
