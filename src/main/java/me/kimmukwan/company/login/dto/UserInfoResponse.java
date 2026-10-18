package me.kimmukwan.company.login.dto;

// 화면 상단에 보여줄 내 정보 (비밀번호 등은 제외)
public record UserInfoResponse(
        String username, // 아이디
        int level,       // 회사 레벨
        int lob,         // 현재 보유 LOB 포인트
        int day,         // 현재 게임 날짜(일차)
        String phase,    // 현재 단계 (START / MONSTER / HIRE / WORK) — 화면이 이 값을 보고 버튼을 잠그거나 풂
        int nowEnergy,   // 오늘 모은 에너지
        int maxEnergy    // 오늘의 에너지 할당량 — nowEnergy가 이 값 이상이면 다음 날로 넘어갈 수 있음
) {
    public static UserInfoResponse from(me.kimmukwan.company.login.domain.User user){
        String phase = user.getPhase() == null ? "START" : user.getPhase().name();
        return new UserInfoResponse(user.getUsername(), user.getLevel(), user.getLob(), user.getDay(),
                phase, user.getNowEnergy(), user.getMaxEnergy());
    }
}
