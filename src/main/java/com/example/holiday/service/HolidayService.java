package com.example.holiday.service;

import com.example.holiday.domain.Country;
import com.example.holiday.domain.Holiday;
import com.example.holiday.domain.HolidayType;

import com.example.holiday.dto.response.HolidayResponse;
import com.example.holiday.dto.response.NagerCountryResponse;
import com.example.holiday.dto.response.NagerHolidayResponse;
import com.example.holiday.repository.CountryRepository;
import com.example.holiday.repository.HolidayRepository;
import com.example.holiday.repository.HolidayTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class HolidayService {

    private static final int START_YEAR = 2020;
    private static final int END_YEAR = 2025;

    private final NagerClient nagerClient;
    private final CountryRepository countryRepository;
    private final HolidayRepository holidayRepository;
    private final HolidayTypeRepository holidayTypeRepository;

    /**
     * 최초 실행 시 한 번만 전체 국가 + 2020~2025 공휴일 적재.
     * holiday 테이블에 데이터가 이미 있으면 아무것도 안 함.
     */
    @Transactional
    public void initialLoadIfEmpty() {
        long count = holidayRepository.count();
        if (count > 0) {
            return;
        }

        // 1) 외부 API로 전체 국가 목록 가져오기
        List<NagerCountryResponse> externalCountries = nagerClient.getAvailableCountries();

        // 2) Country 엔티티로 변환 후 저장
        List<Country> countries = externalCountries.stream()
                .sorted(Comparator.comparing(NagerCountryResponse::countryCode))
                .map(res -> Country.builder()
                        .code(res.countryCode())
                        .name(res.name())
                        .region(null)
                        .build())
                .toList();

        countryRepository.saveAll(countries);

        // 3) 2020~2025, 모든 국가에 대해 공휴일 적재
        for (int year = START_YEAR; year <= END_YEAR; year++) {
            for (Country country : countries) {
                syncYearCountry(year, country.getCode(), false);
            }
        }
    }

    /**
     * 특정 연도/국가의 공휴일 데이터를 외부 API에서 다시 가져와서
     * 기존 데이터를 삭제 후 재삽입
     */
    @Transactional
    public void refresh(int year, String countryCode) {
        validateYearRange(year);
        String upperCode = countryCode.toUpperCase();

        // 1) Country가 DB에 없으면 외부 API 국가 목록에서 찾아서 저장
        Country country = countryRepository.findById(upperCode)
                .orElseGet(() -> saveCountry(upperCode));

        // 2) 실제 동기화 수행 (기존 데이터 삭제 + 새 데이터 삽입)
        syncYearCountry(year, country.getCode(), true);
    }

    /**
     * 특정 연도/국가의 공휴일 레코드 전체 삭제.
     */
    @Transactional
    public void deleteYearCountry(int year, String countryCode) {
        validateYearRange(year);
        String upperCode = countryCode.toUpperCase();
        holidayRepository.deleteByCountryCodeAndYear(upperCode, year);
    }

    /**
     * (배치용) 전체 국가에 대해 특정 연도 데이터 동기화.
     * 매년 1월 2일, 전년도와 금년도에 대해 호출할 수 있음.
     */
    @Transactional
    public void syncAllCountriesForYear(int year) {
        validateYearRange(year);
        List<Country> countries = countryRepository.findAll();
        for (Country country : countries) {
            syncYearCountry(year, country.getCode(), true);
        }
    }

    /**
     * 검색 기능: year, countryCode, from, to, typeCode를 기반으로 페이징 조회.
     */
    @Transactional(readOnly = true)
    public Page<HolidayResponse> search(
            Integer year,
            String countryCode,
            LocalDate from,
            LocalDate to,
            String typeCode,
            Pageable pageable
    ) {
        String normalizedCountry = (countryCode == null ? null : countryCode.toUpperCase());
        String normalizedType = (typeCode == null ? null : typeCode);

        Page<Holiday> page = holidayRepository.search(
                year,
                normalizedCountry,
                from,
                to,
                normalizedType,
                pageable
        );

        return page.map(this::toResponse);
    }

    //  내부 헬퍼 메서드들

    /**
     * - deleteBeforeInsert == true 이면 연도/국가 기존 데이터 먼저 삭제
     * - 외부 API에서 공휴일 목록 조회
     * - Holiday 엔티티로 변환 후 일괄 저장
     */
    private void syncYearCountry(int year, String countryCode, boolean deleteBeforeInsert) {
        validateYearRange(year);

        if (deleteBeforeInsert) {
            holidayRepository.deleteByCountryCodeAndYear(countryCode, year);
        }

        List<NagerHolidayResponse> externalHolidays =
                nagerClient.getPublicHolidays(year, countryCode);

        Country country = countryRepository.findById(countryCode)
                .orElseThrow(() -> new IllegalStateException("Country must exist before syncing holidays"));

        List<Holiday> holidays = externalHolidays.stream()
                .map(res -> Holiday.builder()
                        .country(country)
                        .date(res.date())
                        .localName(res.localName())
                        .name(res.name())
                        .type(findOrCreateHolidayType(firstType(res)))
                        .fixed(res.fixed())
                        .global(res.global())
                        .launchYear(res.launchYear())
                        .build())
                .toList();

        holidayRepository.saveAll(holidays);
    }

    /**
     * 외부 API /AvailableCountries 응답에서 countryCode에 해당하는 국가를 찾아 DB에 저장.
     * refresh 시 Country가 없을 때만 호출.
     */
    private Country saveCountry(String countryCode) {
        List<NagerCountryResponse> externalCountries = nagerClient.getAvailableCountries();

        Optional<NagerCountryResponse> match = externalCountries.stream()
                .filter(c -> c.countryCode().equalsIgnoreCase(countryCode))
                .findFirst();

        if (match.isEmpty()) {
            throw new IllegalArgumentException("Unknown country code from external API: " + countryCode);
        }

        NagerCountryResponse res = match.get();
        Country country = Country.builder()
                .code(res.countryCode())
                .name(res.name())
                .region(null)
                .build();

        return countryRepository.save(country);
    }

    /**
     * HolidayType 관리:
     * - types 리스트에서 첫 번째 값만 사용
     * - DB에 없으면 새로 만들어 저장
     * - 없거나 빈 리스트면 null 반환
     */
    private HolidayType findOrCreateHolidayType(String typeCode) {
        if (typeCode == null || typeCode.isBlank()) {
            return null;
        }

        return holidayTypeRepository.findById(typeCode)
                .orElseGet(() -> holidayTypeRepository.save(
                        HolidayType.builder()
                                .code(typeCode)
                                .build()
                ));
    }

    private String firstType(NagerHolidayResponse res) {
        if (res.types() == null || res.types().isEmpty()) {
            return null;
        }
        return res.types().getFirst();
    }

    /**
     * 지원 연도 범위 (2020~2025) 체크.
     */
    private void validateYearRange(int year) {
        if (year < START_YEAR || year > END_YEAR) {
            throw new IllegalArgumentException("지원 연도 범위는 2020 ~ 2025 입니다. 요청 연도: " + year);
        }
    }

    /**
     * 엔티티 → 응답 DTO 변환.
     */
    private HolidayResponse toResponse(Holiday holiday) {
        return HolidayResponse.from(holiday);
    }
}
