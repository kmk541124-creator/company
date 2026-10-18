package me.kimmukwan.company.monster.dto;

import me.kimmukwan.company.monster.domain.Monster;
import me.kimmukwan.company.monster.domain.UserMonster;

// 내가 수용한 괴물 1마리를 화면에 돌려줄 때 쓰는 응답 객체
// UserMonster를 그대로 보내면 company → user(비밀번호 해시 포함)까지 JSON에 실리므로, 괴물 정보만 담는다
public record UserMonsterResponse(
        Long id,          // UserMonster 번호 — 작업 시작 주소의 {monster} 자리에 들어감
        Monster monster   // 괴물 원본 정보 (이름, 등급, 작업별 확률 등)
) {
    public static UserMonsterResponse from(UserMonster userMonster){
        return new UserMonsterResponse(userMonster.getId(), userMonster.getMonster());
    }
}
