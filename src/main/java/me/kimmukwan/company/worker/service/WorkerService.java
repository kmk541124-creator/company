package me.kimmukwan.company.worker.service;

import lombok.RequiredArgsConstructor;
import me.kimmukwan.company.login.domain.User;
import me.kimmukwan.company.login.repository.UserRepository;
import me.kimmukwan.company.main.domain.GamePhase;
import me.kimmukwan.company.worker.domain.Worker;
import me.kimmukwan.company.worker.dto.WorkerResponse;
import me.kimmukwan.company.worker.repository.WorkerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class WorkerService {

    public static final int MIN_LEVEL = 1;        // 스탯 최소 단계
    public static final int MAX_LEVEL = 5;        // 스탯 최대 단계
    public static final int LOB_PER_LEVEL = 5;    // 스탯을 1단계 올릴 때 드는 LOB 포인트
    public static final int STAT_PER_LEVEL = 20;  // 1단계당 실제 스탯 수치 (1단계=20, 5단계=100)
    public static final int MAX_NAME_LENGTH = 20; // 직원 이름 최대 글자 수

    private final WorkerRepository workerRepository; // 직원 저장/조회
    private final UserRepository userRepository;     // LOB 차감을 위해 DB에서 최신 유저 정보를 가져옴

    // 직원 고용: 검증 → LOB 차감 → 직원 저장을 하나의 트랜잭션으로 처리
    // (중간에 예외가 나면 LOB 차감도 함께 취소됨)
    @Transactional
    public WorkerResponse hire(Long userId, String name, int angry, int smart, int love, int auk){
        String trimmedName = validateName(name);
        validateLevel(angry);
        validateLevel(smart);
        validateLevel(love);
        validateLevel(auk);

        // 1단계는 무료, 그 위로 올린 단계 수만큼 비용 발생
        int cost = ((angry - 1) + (smart - 1) + (love - 1) + (auk - 1)) * LOB_PER_LEVEL;

        // 로그인 세션의 User 객체는 DB와 분리되어 있으므로 DB에서 다시 꺼내서 수정해야 변경이 저장됨
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("유저를 찾을 수 없습니다."));
        user.checkPhase(GamePhase.HIRE); // 직원 고용은 준비(HIRE) 단계에서만 가능
        user.useLob(cost);

        Worker worker = Worker.builder()
                .name(trimmedName)
                .angryStat(angry * STAT_PER_LEVEL)
                .smartStat(smart * STAT_PER_LEVEL)
                .loveStat(love * STAT_PER_LEVEL)
                .aukStat(auk * STAT_PER_LEVEL)
                .user(user)
                .build();

        workerRepository.save(worker);
        return WorkerResponse.from(worker);
    }

    // 내가 고용한 직원 목록
    @Transactional(readOnly = true)
    public List<WorkerResponse> findMyWorkers(Long userId){
        return workerRepository.findByUserIdOrderByIdDesc(userId).stream()
                .map(WorkerResponse::from)
                .toList();
    }

    private void validateLevel(int level){
        if(level < MIN_LEVEL || level > MAX_LEVEL){
            throw new IllegalArgumentException("스탯 단계는 " + MIN_LEVEL + "~" + MAX_LEVEL + " 사이여야 합니다.");
        }
    }

    private String validateName(String name){
        if(name == null || name.isBlank()){
            throw new IllegalArgumentException("직원 이름을 입력해 주세요.");
        }
        String trimmed = name.trim();
        if(trimmed.length() > MAX_NAME_LENGTH){
            throw new IllegalArgumentException("직원 이름은 " + MAX_NAME_LENGTH + "자 이하여야 합니다.");
        }
        return trimmed;
    }
}
