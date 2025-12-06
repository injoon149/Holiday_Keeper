package com.example.holiday.domain;


import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;


@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(builderClassName = "HolidayBuilder")
@Entity
@Table(
        name = "holiday"
)
public class Holiday {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "country_code", nullable = false)
    private Country country;

    //공휴일 날짜
    @Column(name = "holiday_date", nullable = false)
    private LocalDate date;

    //공휴일 년도
    @Column(name = "holiday_year", nullable = false)
    private int year;

    //현지어 공휴일 이름
    @Column(name = "local_name", nullable = false, length = 128)
    private String localName;

    //공식 공휴일 이름
    @Column(nullable = false, length = 128)
    private String name;

    //공휴일 타입
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "type_code")
    private HolidayType type;

    // 매년 날짜가 고정인지 여부
    @Column(name = "is_fixed", nullable = false)
    private boolean fixed;

    // 전국 공통 공휴일인지 여부
    @Column(name = "is_global", nullable = false)
    private boolean global;

    // 최초 시행 연도 (nullable)
    @Column(name = "launch_year")
    private Integer launchYear;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public static class HolidayBuilder {
        public Holiday build() {
            Holiday h = new Holiday(id, country, date, year, localName, name, type,
                    fixed, global, launchYear, null, null);
            h.year = h.date.getYear();
            return h;
        }
    }
}
