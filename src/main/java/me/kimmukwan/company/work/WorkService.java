package me.kimmukwan.company.work;


import lombok.RequiredArgsConstructor;
import me.kimmukwan.company.login.domain.User;
import me.kimmukwan.company.login.repository.UserRepository;
import me.kimmukwan.company.main.domain.GamePhase;
import me.kimmukwan.company.monster.domain.Monster;
import me.kimmukwan.company.monster.domain.UserMonster;
import me.kimmukwan.company.monster.repository.UserMonsterRepository;
import me.kimmukwan.company.work.domain.Work;
import me.kimmukwan.company.work.dto.WorkResponse;
import me.kimmukwan.company.work.repository.WorkRepository;
import me.kimmukwan.company.worker.domain.Worker;
import me.kimmukwan.company.worker.repository.WorkerRepository;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class WorkService {

    public static final Set<String> WORK_TYPES = Set.of("angry", "smart", "love", "auk"); // 허용하는 작업 종류
    public static final String RUNNING = "진행중"; // 진행 중인 작업의 result 값

    private final UserRepository userRepository;               // 단계 확인 / 에너지 추가
    private final UserMonsterRepository userMonsterRepository; // 작업할 괴물 조회
    private final WorkerRepository workerRepository;           // 작업할 직원 조회
    private final WorkRepository workRepository;               // 작업 기록 저장/조회

    // ----- 작업 시작: 검사만 하고 "진행중" 작업을 저장한 뒤 바로 돌려줌 -----
    // 실제 판정(1초마다)은 WorkRunner가 따로 돌리므로, 이 요청은 기다리지 않고 바로 끝남
    @Transactional
    public WorkResponse start(Long userMonsterId, Long workerId, String type, Long userId){
        // 로그인 세션의 User는 DB와 분리되어 있어서 최신 단계를 알 수 없으므로 DB에서 다시 꺼냄
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("유저를 찾을 수 없습니다."));
        user.checkPhase(GamePhase.WORK); // 업무 단계에서만 작업 가능

        UserMonster userMonster = userMonsterRepository.findById(userMonsterId)
                .orElseThrow(() -> new IllegalArgumentException("괴물을 찾을 수 없습니다."));
        Worker worker = workerRepository.findById(workerId)
                .orElseThrow(() -> new IllegalArgumentException("직원을 찾을 수 없습니다."));

        // 다른 유저의 괴물/직원으로 작업하지 못하게 막음
        if(!userMonster.getCompany().getUser().getId().equals(userId)){
            throw new IllegalArgumentException("내 시설에 수용된 괴물이 아닙니다.");
        }
        if(!worker.getUser().getId().equals(userId)){
            throw new IllegalArgumentException("내가 고용한 직원이 아닙니다.");
        }
        if(type == null || !WORK_TYPES.contains(type)){
            throw new IllegalArgumentException("작업 종류가 올바르지 않습니다.");
        }
        if(worker.getHp() <= 0){
            throw new IllegalStateException("체력이 0인 직원은 작업할 수 없습니다.");
        }
        // 한 직원 / 한 괴물은 동시에 하나의 작업만
        if(workRepository.existsByWorkerIdAndResult(workerId, RUNNING)){
            throw new IllegalStateException(worker.getName() + " 직원은 이미 작업 중입니다.");
        }
        if(workRepository.existsByUserMonsterIdAndResult(userMonsterId, RUNNING)){
            throw new IllegalStateException(userMonster.getMonster().getName() + "은(는) 이미 작업을 받고 있습니다.");
        }

        Work work = Work.builder()
                .good(0).bad(0).userMonster(userMonster).worker(worker).user(user).type(type).result(RUNNING)
                .count(userMonster.getMonster().getCount()).build();
        workRepository.save(work);
        return WorkResponse.from(work);
    }

    // ----- 판정 1회 (WorkRunner가 1초마다 호출) -----
    // 반환값: 작업이 끝났으면 true
    @Transactional
    public boolean tick(Long workId){
        Work work = workRepository.findById(workId).orElse(null);
        if(work == null || !work.isRunning()) return true;

        Monster monster = work.getUserMonster().getMonster();
        Worker worker = work.getWorker();

        double randomIndex = Math.random() * 100;
        if(randomIndex < successRate(monster, work.getType())){
            work.upGood();
            userRepository.addEnergy(work.getUser().getId(), 1); // 성공 1회마다 에너지 1 생산
        } else {
            work.upBad();
            work.downCount();                     // 실패할 때마다 카운터가 1씩 줄어듦
            worker.countHp(monster.getDamage());  // 실패하면 직원이 피해를 입음
        }

        if(worker.getHp() <= 0){
            work.fail("직원의 체력이 0이 되었습니다.");
            return true;
        }

        if(work.getCount() <= 0){
            work.fail("카운터가 0이 되었습니다.");
            if(Boolean.TRUE.equals(monster.getEscape())){
                work.markEscaped();
                monsterEscape();
            }
            return true;
        }

        // 판정을 에너지 수치만큼 다 했으면 성공 횟수로 결과 등급을 정하고 끝냄
        if(work.getTicks() >= monster.getEnergy()){
            work.setResult(grade(monster, work.getGood()));
            return true;
        }
        return false;
    }

    // 판정 도중 오류가 나면 작업을 중단 처리 (직원이 "작업 중"으로 영원히 묶이지 않게)
    @Transactional
    public void stop(Long workId, String reason){
        workRepository.findById(workId).ifPresent(work -> {
            if(work.isRunning()){
                work.stop(reason);
            }
        });
    }

    // ----- 진행 상황 조회 -----
    @Transactional(readOnly = true)
    public WorkResponse getWork(Long workId, Long userId){
        Work work = workRepository.findById(workId)
                .orElseThrow(() -> new IllegalArgumentException("작업을 찾을 수 없습니다."));
        if(!work.getUser().getId().equals(userId)){
            throw new IllegalArgumentException("내 작업이 아닙니다.");
        }
        return WorkResponse.from(work);
    }

    // 진행 중인 작업 목록 (작업 화면을 새로고침했을 때 이어서 보여주기용)
    @Transactional(readOnly = true)
    public List<WorkResponse> activeWorks(Long userId){
        return workRepository.findByUserIdAndResultOrderByIdAsc(userId, RUNNING).stream()
                .map(WorkResponse::from)
                .toList();
    }

    // 서버가 켜질 때, 지난번에 서버가 꺼지면서 멈춘 "진행중" 작업들을 중단 처리
    // (그대로 두면 그 직원/괴물이 계속 작업 중으로 남아서 다시 작업할 수 없음)
    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void cleanUpStoppedWorks(){
        workRepository.changeResult(RUNNING, "중단", "서버가 다시 시작되어 작업이 중단되었습니다.");
    }

    // 작업 종류에 맞는 괴물의 성공 확률
    private int successRate(Monster monster, String type){
        return switch (type){
            case "angry" -> monster.getAngry();
            case "smart" -> monster.getSmart();
            case "love" -> monster.getLove();
            case "auk" -> monster.getAuk();
            default -> 0;
        };
    }

    // 성공 횟수 → 결과 등급 (괴물의 good / bad 기준값 사용)
    // good 이상이면 좋음, bad 이하면 나쁨, 그 사이는 보통
    private String grade(Monster monster, int good){
        if(good >= monster.getGood()) return "좋음";
        if(good <= monster.getBad()) return "나쁨";
        return "보통";
    }

    public void monsterEscape(){

    }
}
