package me.kimmukwan.company.main.domain;

import lombok.Getter;

// 하루의 진행 단계
// START(게임 시작 전) → MONSTER(괴물 선택) → HIRE(직원 고용) → WORK(업무) → 다음 날 → MONSTER ...
@Getter
public enum GamePhase {
    START("게임 시작 전"),
    MONSTER("괴물 선택"),
    HIRE("직원 고용"),
    WORK("업무");

    private final String label; // 오류 문장 등 화면에 보여줄 이름

    GamePhase(String label){
        this.label = label;
    }
}
