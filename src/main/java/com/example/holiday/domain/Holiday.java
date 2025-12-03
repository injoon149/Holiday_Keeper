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
        name = "holiday",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_holiday_country_date", columnNames = {"country_code", "date"})
        }
)
public class Holiday {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "country_code", nullable = false)
    private Country country;

    //공휴일 날짜
    @Column(nullable = false)
    private LocalDate date;

    //공휴일 년도
    @Column(nullable = false)
    private int year;

    @Column(nullable = false, length = 128)
    private String name;

    //공휴일 타입
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "type_code")
    private HolidayType type;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public static class HolidayBuilder {
        public Holiday build() {
            Holiday h = new Holiday(id, country, date, year, name, type, null, null);
            h.year = h.date.getYear();
            return h;
        }
    }
}
