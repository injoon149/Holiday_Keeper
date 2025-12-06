package com.example.holiday.repository;

import com.example.holiday.domain.Holiday;
import com.example.holiday.domain.QCountry;
import com.example.holiday.domain.QHoliday;
import com.example.holiday.domain.QHolidayType;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.impl.JPADeleteClause;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;

import java.time.LocalDate;
import java.util.List;

@RequiredArgsConstructor
public class HolidayRepositoryImpl implements HolidayRepositoryCustom {

    private final JPAQueryFactory queryFactory;
    private final EntityManager em;

    @Override
    public Page<Holiday> search(
            Integer year,
            String countryCode,
            LocalDate from,
            LocalDate to,
            String typeCode,
            Pageable pageable
    ) {
        QHoliday holiday = QHoliday.holiday;
        QCountry country = QCountry.country;
        QHolidayType type = QHolidayType.holidayType;

        // 동적 where 조건
        BooleanBuilder builder = new BooleanBuilder();
        if (year != null) {
            builder.and(holiday.year.eq(year));
        }
        if (countryCode != null && !countryCode.isBlank()) {
            builder.and(holiday.country.code.eq(countryCode));
        }
        if (from != null) {
            builder.and(holiday.date.goe(from));
        }
        if (to != null) {
            builder.and(holiday.date.loe(to));
        }
        if (typeCode != null && !typeCode.isBlank()) {
            builder.and(holiday.type.code.eq(typeCode));
        }

        // content 조회 (fetch join으로 N+1 방지)
        List<Holiday> content = queryFactory
                .selectFrom(holiday)
                .leftJoin(holiday.country, country).fetchJoin()
                .leftJoin(holiday.type, type).fetchJoin()
                .where(builder)
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .orderBy(holiday.date.asc(), holiday.id.asc())
                .fetch();

        // total count
        Long total = queryFactory
                .select(holiday.count())
                .from(holiday)
                .leftJoin(holiday.country, country)
                .leftJoin(holiday.type, type)
                .where(builder)
                .fetchOne();

        long totalCount = (total == null ? 0L : total);

        return new PageImpl<>(content, pageable, totalCount);
    }

    @Override
    public void deleteByCountryCodeAndYear(String countryCode, int year) {
        QHoliday h = QHoliday.holiday;

        new JPADeleteClause(em, h)
                .where(h.country.code.eq(countryCode)
                        .and(h.year.eq(year)))
                .execute();
    }
}
