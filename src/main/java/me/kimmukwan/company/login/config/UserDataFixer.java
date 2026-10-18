package me.kimmukwan.company.login.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

// 서버가 켜질 때 한 번 실행되어, 예전에 가입한 계정에서 비어 있는(null) 값을 기본값으로 채워 줌
// day, max_energy, now_energy 컬럼이 나중에 추가되면서 기존 계정은 그 값이 null로 남아 있었고,
// int 필드에 null을 넣을 수 없어서 로그인할 때 오류가 났음
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE) // 다른 초기화(DataIntitalizer)보다 먼저 실행돼야 null 때문에 서버가 죽지 않음
@RequiredArgsConstructor
public class UserDataFixer implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate; // SQL을 직접 실행하기 위한 도구

    @Override
    public void run(String... args){
        int day = jdbcTemplate.update("UPDATE users SET day = 1 WHERE day IS NULL");
        int max = jdbcTemplate.update("UPDATE users SET max_energy = 60 + day * 10 WHERE max_energy IS NULL");
        int now = jdbcTemplate.update("UPDATE users SET now_energy = 0 WHERE now_energy IS NULL");
        int lob = jdbcTemplate.update("UPDATE users SET lob = 100 WHERE lob IS NULL");
        int level = jdbcTemplate.update("UPDATE users SET level = 1 WHERE level IS NULL");
        jdbcTemplate.update("UPDATE users SET phase = 'START' WHERE phase IS NULL"); // 단계 컬럼이 나중에 추가됨
        if(day + max + now + lob + level > 0){
            log.info("기존 계정 null 값 보정: day={}, max_energy={}, now_energy={}, lob={}, level={}", day, max, now, lob, level);
        }

        // monster 테이블에 damage 컬럼이 나중에 추가되면서 기존 괴물(테스트)의 damage가 null로 남아 있었음
        int damage = jdbcTemplate.update("UPDATE monster SET damage = 0 WHERE damage IS NULL");
        if(damage > 0){
            log.info("기존 괴물 null 값 보정: damage={}", damage);
        }
    }
}
