package com.sivan.cranemanagement.repository;

import com.sivan.cranemanagement.model.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
    List<Invoice> findAllByOrderByIdDesc();
    List<Invoice> findByPaymentStatusNot(String status);
    List<Invoice> findByInvoiceDateBetween(LocalDate start, LocalDate end);
}
