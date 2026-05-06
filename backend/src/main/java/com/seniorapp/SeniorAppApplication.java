package com.seniorapp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class SeniorAppApplication {

    public static void main(String[] args) {
        SpringApplication.run(SeniorAppApplication.class, args);
    }
}
