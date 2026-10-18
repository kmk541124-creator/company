package me.kimmukwan.company.main.service;


import lombok.RequiredArgsConstructor;
import me.kimmukwan.company.company.domain.Company;
import me.kimmukwan.company.company.repository.CompanyRepository;
import me.kimmukwan.company.login.domain.User;
import me.kimmukwan.company.login.dto.UserInfoResponse;
import me.kimmukwan.company.login.repository.UserRepository;
import me.kimmukwan.company.main.domain.GamePhase;
import me.kimmukwan.company.monster.domain.ChoiceMonster;
import me.kimmukwan.company.monster.domain.Monster;
import me.kimmukwan.company.monster.domain.UserMonster;
import me.kimmukwan.company.monster.dto.UserMonsterResponse;
import me.kimmukwan.company.monster.repository.ChoiceMonsterRepository;
import me.kimmukwan.company.monster.repository.MonsterRepository;
import me.kimmukwan.company.monster.repository.UserMonsterRepository;
import me.kimmukwan.company.work.WorkService;
import me.kimmukwan.company.work.repository.WorkRepository;
import me.kimmukwan.company.worker.domain.Worker;
import me.kimmukwan.company.worker.repository.WorkerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MainService {

    public static final int MONSTER_PER_COMPANY = 4; // 시설 1개에 수용할 수 있는 괴물 수
    public static final int CHOICE_COUNT = 3;        // 하루에 보여줄 괴물 후보 수

    private final UserRepository userRepository;
    private final WorkerRepository workerRepository;
    private final CompanyRepository companyRepository;
    private final MonsterRepository monsterRepository;
    private final ChoiceMonsterRepository choiceMonsterRepository;
    private final UserMonsterRepository userMonsterRepository;
    private final WorkRepository workRepository;

    // ----- 게임 시작 (START → MONSTER) -----
    @Transactional
    public UserInfoResponse firstGame(Long userId){
        User user = findUser(userId);
        user.checkPhase(GamePhase.START);

        // 예전에 시작 버튼을 눌러서 회사가 이미 있으면 새로 만들지 않음
        if(companyRepository.findByUser(user).isEmpty()){
            Company company = Company.builder()
                    .user(user).location("under").build();
            companyRepository.save(company);
        }

        prepareDay(user);
        return UserInfoResponse.from(user);
    }

    // ----- 괴물 선택 (MONSTER → HIRE) -----
    @Transactional
    public UserMonsterResponse selectMonster(Long userId, Long monsterId){
        User user = findUser(userId);
        user.checkPhase(GamePhase.MONSTER);

        // 오늘 후보로 나온 괴물만 고를 수 있음
        boolean isChoice = choiceMonsterRepository.findByUser(user).stream()
                .anyMatch(c -> c.getMonster().getId().equals(monsterId));
        if(!isChoice){
            throw new IllegalArgumentException("오늘의 괴물 후보가 아닙니다.");
        }

        Company company = findEmptyCompany(user);
        if(company == null){
            throw new IllegalStateException("수용할 빈 자리가 있는 시설이 없습니다.");
        }

        Monster monster = monsterRepository.findById(monsterId)
                .orElseThrow(() -> new IllegalArgumentException("괴물을 찾을 수 없습니다."));
        UserMonster userMonster = UserMonster.builder()
                .monster(monster).company(company).build();
        userMonsterRepository.save(userMonster);

        choiceMonsterRepository.deleteByUser(user); // 하나 고르면 나머지 후보는 사라짐
        user.changePhase(GamePhase.HIRE);
        return UserMonsterResponse.from(userMonster);
    }

    // ----- 준비 끝, 업무 시작 (HIRE → WORK) -----
    @Transactional
    public UserInfoResponse startOperation(Long userId){
        User user = findUser(userId);
        user.checkPhase(GamePhase.HIRE);

        if(workerRepository.findByUserIdOrderByIdDesc(userId).isEmpty()){
            throw new IllegalStateException("직원을 1명 이상 고용해야 업무를 시작할 수 있습니다.");
        }
        if(myMonsters(user).isEmpty()){
            throw new IllegalStateException("수용한 괴물이 없어서 업무를 시작할 수 없습니다.");
        }

        user.changePhase(GamePhase.WORK);
        return UserInfoResponse.from(user);
    }

    // ----- 다음 날로 넘어가기 (WORK → 다음 날 MONSTER) -----
    @Transactional
    public UserInfoResponse nextDay(Long userId){
        User user = findUser(userId);
        user.checkPhase(GamePhase.WORK);

        if(!user.isEnergyFull()){
            throw new IllegalStateException("에너지 할당량을 다 채워야 다음 날로 넘어갈 수 있습니다. ("
                    + user.getNowEnergy() + " / " + user.getMaxEnergy() + ")");
        }
        if(workRepository.existsByUserIdAndResult(userId, WorkService.RUNNING)){
            throw new IllegalStateException("진행 중인 작업이 끝난 뒤에 다음 날로 넘어갈 수 있습니다.");
        }

        // 직원 체력/정신력 회복
        for(Worker worker : workerRepository.findByUserIdOrderByIdDesc(userId)){
            worker.reset();
        }
        user.upDay();   // 날짜 + 1, 에너지 할당량 증가
        user.reset();   // 모은 에너지 0으로

        prepareDay(user);
        return UserInfoResponse.from(user);
    }

    // 내가 수용한 괴물 목록
    @Transactional(readOnly = true)
    public List<UserMonsterResponse> myMonsterList(Long userId){
        return myMonsters(findUser(userId)).stream()
                .map(UserMonsterResponse::from)
                .toList();
    }

    // 하루 준비: 괴물 후보를 만들고 단계를 MONSTER로 — 빈 자리가 없거나 후보가 없으면 바로 HIRE로
    private void prepareDay(User user){
        choiceMonsterRepository.deleteByUser(user); // 어제 남은 후보 정리

        if(findEmptyCompany(user) != null){
            List<Monster> monsters = new ArrayList<>(monsterRepository.findAll());
            Collections.shuffle(monsters); // 무작위로 섞어서 앞에서부터 후보로 사용
            for(Monster monster : monsters.subList(0, Math.min(CHOICE_COUNT, monsters.size()))){
                choiceMonsterRepository.save(ChoiceMonster.builder().user(user).monster(monster).build());
            }
            if(!monsters.isEmpty()){
                user.changePhase(GamePhase.MONSTER);
                return;
            }
        }
        user.changePhase(GamePhase.HIRE);
    }

    // 괴물 자리가 남은 시설 (없으면 null)
    private Company findEmptyCompany(User user){
        for(Company company : companyRepository.findByUser(user)){
            if(userMonsterRepository.findByCompany(company).size() < MONSTER_PER_COMPANY){
                return company;
            }
        }
        return null;
    }

    private List<UserMonster> myMonsters(User user){
        List<UserMonster> result = new ArrayList<>();
        for(Company company : companyRepository.findByUser(user)){
            result.addAll(userMonsterRepository.findByCompany(company));
        }
        return result;
    }

    private User findUser(Long userId){
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("유저를 찾을 수 없습니다."));
    }
}
