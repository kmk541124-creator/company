package me.kimmukwan.company.worker.controller;


import lombok.RequiredArgsConstructor;
import me.kimmukwan.company.login.domain.User;
import me.kimmukwan.company.worker.dto.WorkerResponse;
import me.kimmukwan.company.worker.service.WorkerService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequiredArgsConstructor
@RequestMapping("/api/")
public class WorkerController {

    private final WorkerService workerService; // 고용(검증 + 단계 확인 + LOB 차감) / 보유 직원 목록 조회

    // 직원 고용 — {angry} 등은 스탯 단계(1~5)
    // 검증·LOB 차감·저장은 WorkerService.hire가 한 트랜잭션으로 처리하고,
    // 실패하면 예외 → GlobalExceptionHandler가 400 + 이유(message)로 응답
    @PostMapping("worker/get/{name}/{angry}/{smart}/{love}/{auk}")
    public ResponseEntity<WorkerResponse> getWorker(@AuthenticationPrincipal User user,
                                                    @PathVariable String name,
                                                    @PathVariable int angry,
                                                    @PathVariable int smart,
                                                    @PathVariable int love,
                                                    @PathVariable int auk){
        WorkerResponse worker = workerService.hire(user.getId(), name, angry, smart, love, auk);
        return ResponseEntity.status(201).body(worker);
    }

    // 내가 보유한 직원 목록 — hire.js의 loadWorkers()가 이 주소를 호출함
    @GetMapping("worker/list")
    public ResponseEntity<List<WorkerResponse>> myWorkers(@AuthenticationPrincipal User user){
        return ResponseEntity.ok(workerService.findMyWorkers(user.getId()));
    }
}
