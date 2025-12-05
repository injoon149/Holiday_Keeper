package com.example.holiday.repository;

import com.example.holiday.domain.Holiday;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;

public interface HolidayRepository extends JpaRepository<Holiday, Long> {

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from Holiday h where h.country.code = :countryCode and h.year = :year")
    void deleteByCountryCodeAndYear(@Param("countryCode") String countryCode,
                                    @Param("year") int year);

    /**
     * 동적 검색 쿼리.
     * 파라미터가 null이면 해당 조건은 무시되도록 설계.
     */
    @Query("""
        select h
        from Holiday h
        where (:year is null or h.year = :year)
          and (:countryCode is null or h.country.code = :countryCode)
          and (:from is null or h.date >= :from)
          and (:to is null or h.date <= :to)
          and (:typeCode is null or h.type.code = :typeCode)
        """)
    Page<Holiday> search(
            @Param("year") Integer year,
            @Param("countryCode") String countryCode,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to,
            @Param("typeCode") String typeCode,
            Pageable pageable
    );
}