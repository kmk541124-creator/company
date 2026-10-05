package me.kimmukwan.company.login.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class UserViewController {

    @GetMapping("/singup")
    public String singup(){
        return "singup";
    }

    @GetMapping("/login")
    public String login(){
        return "login";
    }
}
