package com.sivan.cranemanagement.repository;

import com.sivan.cranemanagement.model.TripSheet;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TripSheetRepository extends JpaRepository<TripSheet, Long> {
    List<TripSheet> findAllByOrderByIdDesc();
    List<TripSheet> findByCraneIdOrderByIdDesc(Long craneId);
    List<TripSheet> findByBookingIdOrderByIdDesc(Long bookingId);
    List<TripSheet> findByBillingTypeOrderByIdDesc(String billingType);
    long countByCraneId(Long craneId);
}
