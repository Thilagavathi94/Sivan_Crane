package com.sivan.cranemanagement.repository;

import com.sivan.cranemanagement.model.Driver;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface DriverRepository extends JpaRepository<Driver, Long> {
    List<Driver> findAllByOrderByIdDesc();
}
