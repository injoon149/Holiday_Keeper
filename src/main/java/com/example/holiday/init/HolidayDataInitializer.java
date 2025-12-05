package com.example.holiday.init;

import com.example.holiday.service.HolidayService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class HolidayDataInitializer implements ApplicationRunner {

    private final HolidayService holidayService;

    @Override
    public void run(ApplicationArguments args) {
        log.info("[HolidayDataInitializer] 초기 공휴일 데이터 적재 시작");

        try {
            holidayService.initialLoadIfEmpty();
            log.info("[HolidayDataInitializer] 초기 공휴일 데이터 적재 완료 (이미 데이터가 있으면 스킵)");
        } catch (Exception e) {
            log.error("[HolidayDataInitializer] 초기 데이터 적재 중 예외 발생", e);
        }
    }
}
