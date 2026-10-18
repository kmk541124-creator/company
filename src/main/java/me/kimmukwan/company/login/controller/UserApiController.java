package me.kimmukwan.company.login.controller;

import lombok.RequiredArgsConstructor;
import me.kimmukwan.company.login.domain.User;
import me.kimmukwan.company.login.dto.AddUserRequest;
import me.kimmukwan.company.login.dto.UserInfoResponse;
import me.kimmukwan.company.login.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
@RequiredArgsConstructor
public class UserApiController {

    private final UserService userService;

    @PostMapping("/user")
    public String singup(AddUserRequest request){
        try {
            userService.save(request);
        } catch (IllegalArgumentException e) {
            return "redirect:/singup?duplicate"; // 아이디 중복 → 회원가입 화면에 안내 표시
        }
        return  "redirect:/login";
    }

    // 로그인한 유저의 아이디/레벨/LOB 포인트 조회 (화면 상단 표시용)
    @GetMapping("/api/user/me")
    public ResponseEntity<UserInfoResponse> me(@AuthenticationPrincipal User user){
        return ResponseEntity.ok(userService.getMyInfo(user.getId()));
    }
}
