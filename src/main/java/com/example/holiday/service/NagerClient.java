package com.example.holiday.service;

import com.example.holiday.dto.response.NagerCountryResponse;
import com.example.holiday.dto.response.NagerHolidayResponse;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

public class NagerClient {
    private final WebClient webClient;

    public NagerClient(WebClient webClient) {
        this.webClient = webClient;
    }

    public List<NagerCountryResponse> getAvailableCountries() {
        return webClient.get()
                .uri("/AvailableCountries")
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<NagerCountryResponse>>() {})
                .block();
    }

    public List<NagerHolidayResponse> getPublicHolidays(int year, String countryCode) {
        return webClient.get()
                .uri("/PublicHolidays/{year}/{countryCode}", year, countryCode)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<NagerHolidayResponse>>() {})
                .block();
    }
}
