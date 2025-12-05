package com.example.holiday.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Bean
    public WebClient nagerWebClient() {
        return WebClient.builder()
                .baseUrl("https://date.nager.at/api/v3")
                .build();
    }
}