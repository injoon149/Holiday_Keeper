package com.example.holiday.dto.response;


import com.example.holiday.domain.Holiday;

import java.time.LocalDate;

public record HolidayResponse(
        Long id,
        String countryCode,
        String countryName,
        LocalDate date,
        int year,
        String localName,
        String name,
        String typeCode,
        boolean fixed,
        boolean global,
        Integer launchYear
) {

    public static HolidayResponse from(Holiday holiday) {
        return new HolidayResponse(
                holiday.getId(),
                holiday.getCountry().getCode(),
                holiday.getCountry().getName(),
                holiday.getDate(),
                holiday.getYear(),
                holiday.getLocalName(),
                holiday.getName(),
                holiday.getType() != null ? holiday.getType().getCode() : null,
                holiday.isFixed(),
                holiday.isGlobal(),
                holiday.getLaunchYear()
        );
    }
}
