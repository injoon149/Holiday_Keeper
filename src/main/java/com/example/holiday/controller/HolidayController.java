package com.example.holiday.controller;

import com.example.holiday.dto.response.HolidayResponse;
import com.example.holiday.service.HolidayService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/holidays")
@RequiredArgsConstructor
public class HolidayController {

    private final HolidayService holidayService;

    @Operation(
            summary = "공휴일 검색",
            description = """
                    연도, 국가 코드, 기간(from ~ to), 타입 코드로 공휴일을 검색합니다.
                    모든 파라미터는 선택이며, 페이징(page, size)과 함께 사용할 수 있습니다.
                    """
    )
    @GetMapping
    public ResponseEntity<Page<HolidayResponse>> search(
            @Parameter(description = "연도 (예: 2025)")
            @RequestParam(required = false)
            Integer year,

            @Parameter(description = "국가 코드 (예: KR, US)")
            @RequestParam(required = false)
            String countryCode,

            @Parameter(description = "조회 시작일 (yyyy-MM-dd)")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate from,

            @Parameter(description = "조회 종료일 (yyyy-MM-dd)")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate to,

            @Parameter(description = "공휴일 타입 코드 (예: Public)")
            @RequestParam(required = false)
            String typeCode,

            @Parameter(description = "페이징 파라미터")
            @PageableDefault(size = 20, sort = "date", direction = Sort.Direction.ASC)
            Pageable pageable
    ) {
        Page<HolidayResponse> result = holidayService.search(
                year,
                countryCode,
                from,
                to,
                typeCode,
                pageable
        );
        return ResponseEntity.ok(result);
    }

    @Operation(
            summary = "특정 연도·국가 공휴일 재동기화",
            description = "특정 연도와 국가에 대한 공휴일 데이터를 외부 API에서 다시 조회하여 기존 데이터를 덮어씁니다."
    )
    @PostMapping("/refresh")
    public ResponseEntity<Void> refresh(        @RequestParam int year,
                                                @RequestParam String countryCode) {
        holidayService.refresh(year, countryCode);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "특정 연도·국가 공휴일 전체 삭제",
            description = "특정 연도와 국가에 대한 모든 공휴일 레코드를 삭제합니다."
    )
    @DeleteMapping
    public ResponseEntity<Void> delete(
            @RequestParam int year,
            @RequestParam String countryCode
    ) {
        holidayService.deleteYearCountry(year, countryCode);
        return ResponseEntity.noContent().build();
    }

}

