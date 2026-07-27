package com.sivan.cranemanagement.repository;

import com.sivan.cranemanagement.model.Crane;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CraneRepository extends JpaRepository<Crane, Long> {
    List<Crane> findAllByOrderByIdAsc();
    long countByStatus(String status);
}
