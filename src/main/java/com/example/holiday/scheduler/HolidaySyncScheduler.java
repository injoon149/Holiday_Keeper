package com.example.holiday.scheduler;


import com.example.holiday.service.HolidayService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;

@Slf4j
@Component
@RequiredArgsConstructor
public class HolidaySyncScheduler {

    private final HolidayService holidayService;

    //테스트용
    //@Scheduled(fixedDelay = 10_000, zone = "Asia/Seoul")
    @Scheduled(cron = "0 0 1 2 1 *", zone = "Asia/Seoul")
    public void syncCurrentAndPreviousYear() {
        ZoneId zoneId = ZoneId.of("Asia/Seoul");
        LocalDate now = LocalDate.now(zoneId);

        int currentYear = now.getYear();
        int previousYear = currentYear - 1;

        log.info("[HolidaySyncScheduler] 연간 공휴일 자동 동기화 시작 - prev={}, current={}",
                previousYear, currentYear);

        try {
            holidayService.syncAllCountriesForYear(previousYear);
        } catch (IllegalArgumentException e) {
            // (예: 과제 범위 밖 연도면 스킵)
            log.warn("[HolidaySyncScheduler] previousYear={} 스킵 - {}", previousYear, e.getMessage());
        }

        try {
            holidayService.syncAllCountriesForYear(currentYear);
        } catch (IllegalArgumentException e) {
            log.warn("[HolidaySyncScheduler] currentYear={} 스킵 - {}", currentYear, e.getMessage());
        }

        log.info("[HolidaySyncScheduler] 연간 공휴일 자동 동기화 종료");
    }
}
