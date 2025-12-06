package com.example.holiday.repository;

import com.example.holiday.domain.Holiday;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;

public interface HolidayRepositoryCustom {

    Page<Holiday> search(
            Integer year,
            String countryCode,
            LocalDate from,
            LocalDate to,
            String typeCode,
            Pageable pageable
    );
    void deleteByCountryCodeAndYear(String countryCode, int year);
}
