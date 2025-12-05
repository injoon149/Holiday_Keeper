package com.example.holiday.dto.request;

public record HolidaySyncRequest(
        int year,
        String countryCode
) {
}
