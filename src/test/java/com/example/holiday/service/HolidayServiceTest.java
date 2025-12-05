package com.example.holiday.service;

import static org.junit.jupiter.api.Assertions.*;


import com.example.holiday.domain.Country;
import com.example.holiday.domain.Holiday;
import com.example.holiday.domain.HolidayType;

import com.example.holiday.dto.response.HolidayResponse;
import com.example.holiday.dto.response.NagerCountryResponse;
import com.example.holiday.dto.response.NagerHolidayResponse;
import com.example.holiday.repository.CountryRepository;
import com.example.holiday.repository.HolidayRepository;
import com.example.holiday.repository.HolidayTypeRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

/**
 * HolidayService에 대한 통합 테스트.
 * - H2 + JPA 실제로 사용
 * - 외부 API(NagerClient)는 Mocking
 */
@SpringBootTest
@Transactional  // 각 테스트 후 자동 롤백 → DB 깨끗하게 유지
class HolidayServiceTest {

    @Autowired
    HolidayService holidayService;

    @Autowired
    CountryRepository countryRepository;

    @Autowired
    HolidayRepository holidayRepository;

    @Autowired
    HolidayTypeRepository holidayTypeRepository;

    @MockBean
    NagerClient nagerClient; // 실제 HTTP 호출 막고, 우리가 원하는 응답 주입

    @Test
    @DisplayName("initialLoadIfEmpty: 비어 있을 때 2020~2025 공휴일을 적재한다")
    void initialLoadIfEmpty_loadsDataWhenEmpty() {
        // given
        // 1) 외부 국가 목록 Stub (KR 한 개만)
        List<NagerCountryResponse> countryResponses = List.of(
                new NagerCountryResponse("KR", "Korea (Republic of)")
        );
        given(nagerClient.getAvailableCountries()).willReturn(countryResponses);

        // 2) 외부 공휴일 Stub
        // 2020~2025 각 연도마다 1개 공휴일씩 리턴하도록 설정
        given(nagerClient.getPublicHolidays(
                ArgumentMatchers.anyInt(),
                ArgumentMatchers.eq("KR"))
        ).willAnswer(invocation -> {
            int year = invocation.getArgument(0, Integer.class);
            String countryCode = invocation.getArgument(1, String.class);
            return List.of(
                    new NagerHolidayResponse(
                            LocalDate.of(year, 1, 1),   // 날짜는 해당 연도 1/1
                            "새해",
                            "New Year's Day",           // name
                            countryCode,
                            false,
                            true,
                            1949,
                            List.of("Public")
                    )
            );
        });

        // when
        holidayService.initialLoadIfEmpty();

        // then
        // Country는 1개 (KR)
        assertThat(countryRepository.count()).isEqualTo(1);

        // Holiday는 2020~2025 = 6년 × 1개 국가 × 1개 공휴일 = 6개
        assertThat(holidayRepository.count()).isEqualTo(6);

        // 검색 API로도 한번 검증해 보자 (예: 2025년 KR)
        Page<HolidayResponse> page = holidayService.search(
                2025,
                "KR",
                null,
                null,
                null,
                PageRequest.of(0, 10)
        );

        assertThat(page.getTotalElements()).isEqualTo(1);
        HolidayResponse holiday = page.getContent().getFirst();
        assertThat(holiday.countryCode()).isEqualTo("KR");
        assertThat(holiday.year()).isEqualTo(2025);
        assertThat(holiday.date()).isEqualTo(LocalDate.of(2025, 1, 1));
        assertThat(holiday.localName()).isEqualTo("새해");
        assertThat(holiday.name()).isEqualTo("New Year's Day");
        assertThat(holiday.typeCode()).isEqualTo("Public");

        assertThat(holiday.fixed()).isFalse();
        assertThat(holiday.global()).isTrue();
        // HolidayType도 Public 하나가 생성되어 있어야 함
        assertThat(holidayTypeRepository.findById("Public")).isPresent();
    }

    @Test
    @DisplayName("initialLoadIfEmpty: 이미 데이터가 있으면 아무 작업도 하지 않는다")
    void initialLoadIfEmpty_doesNothingWhenDataExists() {
        // given
        // 우선 KR 국가 & Holiday 한 건을 직접 저장해서 "이미 데이터 있는 상황" 만들기
        Country kr = Country.builder()
                .code("KR")
                .name("Korea (Republic of)")
                .region(null)
                .build();
        countryRepository.save(kr);

        HolidayType type = HolidayType.builder()
                .code("Public")
                .build();
        holidayTypeRepository.save(type);

        Holiday holiday = Holiday.builder()
                .country(kr)
                .date(LocalDate.of(2025, 1, 1))
                .localName("설날")
                .name("Lunar New Year")
                .type(type)
                .fixed(false)
                .global(true)
                .launchYear(2025)
                .build();
        holidayRepository.save(holiday);

        long beforeCountryCount = countryRepository.count();
        long beforeHolidayCount = holidayRepository.count();

        // when
        holidayService.initialLoadIfEmpty();

        // then
        // holidayRepository.count() > 0 이므로, NagerClient는 호출되지 않아야 함
        verify(nagerClient, never()).getAvailableCountries();
        verify(nagerClient, never()).getPublicHolidays(anyInt(), anyString());

        // DB에 들어 있던 데이터는 그대로 유지
        assertThat(countryRepository.count()).isEqualTo(beforeCountryCount);
        assertThat(holidayRepository.count()).isEqualTo(beforeHolidayCount);
    }

    @Test
    @DisplayName("refresh: 특정 연도/국가 데이터를 삭제 후 재삽입한다")
    void refresh_replacesHolidaysForYearAndCountry() {
        // given
        // 1) 먼저 initialLoadIfEmpty()로 기본 데이터 적재
        List<NagerCountryResponse> countryResponses = List.of(
                new NagerCountryResponse("KR", "Korea (Republic of)")
        );
        given(nagerClient.getAvailableCountries()).willReturn(countryResponses);
        given(nagerClient.getPublicHolidays(anyInt(), eq("KR")))
                .willAnswer(invocation -> {
                    int year = invocation.getArgument(0, Integer.class);
                    String countryCode = invocation.getArgument(1, String.class);
                    return List.of(
                            new NagerHolidayResponse(
                                    LocalDate.of(year, 1, 1),
                                    "새해 첫날",
                                    "New Year's Day",
                                    countryCode,
                                    true,
                                    true,
                                    1949,
                                    List.of("Public")
                            )
                    );
                });

        holidayService.initialLoadIfEmpty();
        long beforeCount2025 = holidayService.search(
                2025,
                "KR",
                null,
                null,
                null,
                PageRequest.of(0, 10)
        ).getTotalElements();
        assertThat(beforeCount2025).isEqualTo(1);

        // 2) 이제 refresh(2025, "KR")를 호출했을 때,
        //    다른 공휴일 데이터로 갈아끼우도록 Stub 변경
        reset(nagerClient); // 이전 stubbing 초기화

        // 국가 목록은 refresh에서 fetchAndSaveCountry가 필요 없도록, 미리 country는 DB에 있으니 호출 안될 예정이지만,
        // 혹시를 위해 기본 Stub 정도만
        when(nagerClient.getAvailableCountries()).thenReturn(countryResponses);

        // 2025년 KR 공휴일을 "다른 이름"으로 바꿔서 리턴하도록 Stub
        given(nagerClient.getPublicHolidays(2025, "KR"))
                .willReturn(List.of(
                        new NagerHolidayResponse(
                                LocalDate.of(2025, 2, 1),
                                "새로운 휴일",
                                "New Holiday",
                                "KR",
                                false,
                                true,
                                2025,
                                List.of("Public")
                        )
                ));

        // when
        holidayService.refresh(2025, "KR");

        // then
        Page<HolidayResponse> pageAfter = holidayService.search(
                2025,
                "KR",
                null,
                null,
                null,
                PageRequest.of(0, 10)
        );

        assertThat(pageAfter.getTotalElements()).isEqualTo(1);
        HolidayResponse refreshed = pageAfter.getContent().getFirst();
        assertThat(refreshed.date()).isEqualTo(LocalDate.of(2025, 2, 1));
        assertThat(refreshed.name()).isEqualTo("New Holiday");
    }
}
