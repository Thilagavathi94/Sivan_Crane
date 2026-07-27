package com.sivan.cranemanagement.repository;

import com.sivan.cranemanagement.model.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Long> {
    List<Booking> findAllByOrderByIdDesc();
    long countByBookingDate(LocalDate date);
    long countByStatus(String status);
    List<Booking> findTop5ByOrderByIdDesc();
}
