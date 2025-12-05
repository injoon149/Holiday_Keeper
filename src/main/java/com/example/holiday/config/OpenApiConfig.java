package com.example.holiday.config;


import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Holiday Mini Service API",
                version = "v1",
                description = "Nager API 기반 전세계 공휴일 저장·조회·관리 Mini Service"
        )
)
public class OpenApiConfig {
}
