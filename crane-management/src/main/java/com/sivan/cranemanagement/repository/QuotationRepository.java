package com.sivan.cranemanagement.repository;

import com.sivan.cranemanagement.model.Quotation;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface QuotationRepository extends JpaRepository<Quotation, Long> {
    List<Quotation> findAllByOrderByIdDesc();
}
