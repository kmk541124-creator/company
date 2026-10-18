package me.kimmukwan.company.monster.dto;

import me.kimmukwan.company.monster.domain.ChoiceMonster;
import me.kimmukwan.company.monster.domain.Monster;

// 오늘의 괴물 후보 1마리 — ChoiceMonster를 그대로 보내면 user(비밀번호 해시 포함)까지 JSON에 실리므로 괴물 정보만 담음
public record ChoiceMonsterResponse(
        Long id,          // 후보 번호
        Monster monster   // 후보 괴물 정보 — monster.js가 c.monster로 읽음
) {
    public static ChoiceMonsterResponse from(ChoiceMonster choice){
        return new ChoiceMonsterResponse(choice.getId(), choice.getMonster());
    }
}
