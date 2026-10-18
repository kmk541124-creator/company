package me.kimmukwan.company.login.controller;

import lombok.RequiredArgsConstructor;
import me.kimmukwan.company.login.domain.User;
import me.kimmukwan.company.login.repository.UserRepository;
import me.kimmukwan.company.main.domain.GamePhase;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class UserViewController {

    private final UserRepository userRepository; // 화면을 열기 전에 지금 단계를 DB에서 확인

    @GetMapping("/singup")
    public String singup(){
        return "singup";
    }

    @GetMapping("/login")
    public String login(){
        return "login";
    }

    // 로그인 후 메인 화면 (지금 단계에 맞는 버튼만 열림)
    @GetMapping("/")
    public String home(){
        return "home";
    }

    // 직원 고용 화면 — 준비(HIRE) 단계에서만
    @GetMapping("/hire")
    public String hire(@AuthenticationPrincipal User user){
        return pageFor(user, GamePhase.HIRE, "hire");
    }

    // 괴물 선택 화면 — 괴물 선택(MONSTER) 단계에서만
    @GetMapping("/monster")
    public String monster(@AuthenticationPrincipal User user){
        return pageFor(user, GamePhase.MONSTER, "monster");
    }

    // 작업 지시 화면 — 업무(WORK) 단계에서만
    @GetMapping("/work")
    public String work(@AuthenticationPrincipal User user){
        return pageFor(user, GamePhase.WORK, "work");
    }

    // 지금 단계가 맞으면 그 화면을, 아니면 메인 화면으로 돌려보냄 (주소를 직접 쳐서 들어오는 것도 막음)
    // 로그인 세션의 User는 단계가 바뀌어도 갱신되지 않으므로 DB에서 다시 읽음
    private String pageFor(User user, GamePhase phase, String view){
        GamePhase now = userRepository.findById(user.getId()).map(User::getPhase).orElse(null);
        return now == phase ? view : "redirect:/";
    }
}
