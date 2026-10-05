package me.kimmukwan.company.login.controller;

import lombok.RequiredArgsConstructor;
import me.kimmukwan.company.login.dto.AddUserRequest;
import me.kimmukwan.company.login.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
@RequiredArgsConstructor
public class UserApiController {

    private final UserService userService;

    @PostMapping("/user")
    public String singup(AddUserRequest request){
        userService.save(request);
        return  "redirect:/login";
    }
}
