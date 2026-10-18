package me.kimmukwan.company.main.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

// 서비스에서 던진 "잘못된 요청" 예외를 400 응답 + { "message": "이유" } JSON으로 바꿔 줌
// 화면(js)은 이 message를 그대로 보여줌
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public ResponseEntity<Map<String, String>> badRequest(RuntimeException e){
        return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
    }
}
