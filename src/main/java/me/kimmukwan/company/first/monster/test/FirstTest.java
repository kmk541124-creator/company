package me.kimmukwan.company.first.monster.test;


import lombok.RequiredArgsConstructor;
import me.kimmukwan.company.monster.domain.Monster;
import me.kimmukwan.company.monster.domain.MonsterReport;
import me.kimmukwan.company.monster.repository.MonsterReportRepository;
import me.kimmukwan.company.monster.repository.MonsterRepository;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FirstTest {

    private final MonsterRepository monsterRepository;
    private final MonsterReportRepository monsterReportRepository;

    public void saveTest(){
        if(monsterRepository.findByName("테스트").isPresent()){
            return;
        }
        Monster monster = Monster.builder()
                .name("테스트").grade("ZAYIN").angry(100).smart(0)
                .love(50).auk(25).code("F0-00-00").escape(false)
                .energy(10).count(2).good(8).normal(5).bad(3).damage(0).build();
        monsterRepository.save(monster);

        MonsterReport monsterReport = MonsterReport.builder()
                .info("테스트용 로봇이다 토끼 같다").count(1).monster(monster).build();
        monsterReportRepository.save(monsterReport);

        monsterReport = MonsterReport.builder()
                .info("나쁨뜨면 죽는다").count(2).monster(monster).build();
        monsterReportRepository.save(monsterReport);
    }
}
