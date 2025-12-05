package com.example.holiday;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class HolidayApplication {

    public static void main(String[] args) {
        SpringApplication.run(com.example.holiday.HolidayApplication.class, args);
    }

}
