package com.wowconsulting;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class WowConsultingApplication {

    public static void main(String[] args) {
        SpringApplication.run(WowConsultingApplication.class, args);
    }
}