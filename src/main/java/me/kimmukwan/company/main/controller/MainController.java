package me.kimmukwan.company.main.controller;


import lombok.RequiredArgsConstructor;
import me.kimmukwan.company.login.domain.User;
import me.kimmukwan.company.login.dto.UserInfoResponse;
import me.kimmukwan.company.main.service.MainService;
import me.kimmukwan.company.monster.dto.ChoiceMonsterResponse;
import me.kimmukwan.company.monster.dto.UserMonsterResponse;
import me.kimmukwan.company.monster.repository.ChoiceMonsterRepository;
import me.kimmukwan.company.work.WorkRunner;
import me.kimmukwan.company.work.WorkService;
import me.kimmukwan.company.work.dto.WorkResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

// 단계 진행(시작 → 괴물 선택 → 업무 시작 → 다음 날)과 작업 관련 API
// 잘못된 요청(IllegalArgument/IllegalState 예외)은 GlobalExceptionHandler가 { message } JSON으로 바꿔서 돌려줌
@Controller
@RequiredArgsConstructor
@RequestMapping("/api")
public class MainController {

    private final MainService mainService;
    private final ChoiceMonsterRepository choiceMonsterRepository;
    private final WorkService workService;
    private final WorkRunner workRunner;

    // 게임 시작하기 (START → MONSTER)
    @PostMapping("/first/start")
    public ResponseEntity<UserInfoResponse> firstStart(@AuthenticationPrincipal User user){
        return ResponseEntity.ok(mainService.firstGame(user.getId()));
    }

    // 오늘의 괴물 후보
    @GetMapping("/choice")
    public ResponseEntity<List<ChoiceMonsterResponse>> choiceMonster(@AuthenticationPrincipal User user){
        List<ChoiceMonsterResponse> choiceMonsters = choiceMonsterRepository.findByUser(user).stream()
                .map(ChoiceMonsterResponse::from)
                .toList();

        return ResponseEntity.ok().body(choiceMonsters);
    }

    // 괴물 선택 (MONSTER → HIRE)
    @GetMapping("/selectMonster/{monster}")
    public ResponseEntity<UserMonsterResponse> selectMonster(@AuthenticationPrincipal User user,
                                                             @PathVariable Long monster){
        return ResponseEntity.ok(mainService.selectMonster(user.getId(), monster));
    }

    // 준비 완료 → 업무 시작 (HIRE → WORK)
    @PostMapping("/phase/work")
    public ResponseEntity<UserInfoResponse> startOperation(@AuthenticationPrincipal User user){
        return ResponseEntity.ok(mainService.startOperation(user.getId()));
    }

    // 다음 날로 넘어가기 (WORK → 다음 날 MONSTER) — 에너지 할당량을 다 채워야 가능
    @PostMapping("/next/day")
    public ResponseEntity<UserInfoResponse> nextDay(@AuthenticationPrincipal User user){
        return ResponseEntity.ok(mainService.nextDay(user.getId()));
    }

    // 내가 수용한 괴물 목록 — work.js의 loadMonsters()가 이 주소를 호출함
    @GetMapping("/monster/list")
    public ResponseEntity<List<UserMonsterResponse>> myMonsters(@AuthenticationPrincipal User user){
        return ResponseEntity.ok(mainService.myMonsterList(user.getId()));
    }

    // 작업 시작 — "진행중" 작업을 저장하고 바로 돌려줌, 판정은 WorkRunner가 백그라운드에서 1초마다 진행
    @PostMapping("/start/work/{monster}/{worker}")
    public ResponseEntity<WorkResponse> startWork(@PathVariable Long monster, @PathVariable Long worker,
                                                  @RequestParam String type,
                                                  @AuthenticationPrincipal User user){
        // start()의 트랜잭션이 끝나(저장이 확정되)고 나서 runner를 돌려야 runner가 작업을 찾을 수 있음
        WorkResponse response = workService.start(monster, worker, type, user.getId());
        workRunner.run(response.id());
        return ResponseEntity.ok(response);
    }

    // 작업 진행 상황 — 화면이 0.5초마다 호출해서 숫자를 갱신함
    @GetMapping("/work/{id}")
    public ResponseEntity<WorkResponse> work(@PathVariable Long id, @AuthenticationPrincipal User user){
        return ResponseEntity.ok(workService.getWork(id, user.getId()));
    }

    // 진행 중인 작업 목록 — 작업 화면을 새로고침했을 때 이어서 보여주기용
    @GetMapping("/work/active")
    public ResponseEntity<List<WorkResponse>> activeWorks(@AuthenticationPrincipal User user){
        return ResponseEntity.ok(workService.activeWorks(user.getId()));
    }
}
