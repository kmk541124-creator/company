package me.kimmukwan.company.first;

import lombok.RequiredArgsConstructor;
import me.kimmukwan.company.first.monster.test.FirstTest;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class DataIntitalizer implements CommandLineRunner {


    private final FirstTest firstTest;

    @Override
    public void run(String... args){
        firstTest.saveTest();
    }
}
