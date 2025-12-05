package com.example.holiday.dto.response;

import java.time.LocalDate;
import java.util.List;

public record NagerHolidayResponse(
        LocalDate date,
        String localName,
        String name,
        String countryCode,
        boolean fixed,
        boolean global,
        Integer launchYear,
        List<String> types
) {
}
