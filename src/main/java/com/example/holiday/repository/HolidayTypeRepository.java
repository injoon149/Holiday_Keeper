package com.example.holiday.repository;

import com.example.holiday.domain.HolidayType;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HolidayTypeRepository extends JpaRepository<HolidayType, String> {
}
