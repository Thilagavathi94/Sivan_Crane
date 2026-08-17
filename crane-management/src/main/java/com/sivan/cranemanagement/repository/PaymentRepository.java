package com.sivan.cranemanagement.repository;

import com.sivan.cranemanagement.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    List<Payment> findAllByOrderByIdDesc();
    List<Payment> findByInvoiceId(Long invoiceId);
    List<Payment> findByTripSheetId(Long tripSheetId);
    List<Payment> findByInvoiceIsNotNullAndPaymentDateBetweenOrderByPaymentDateAsc(LocalDate start, LocalDate end);
    List<Payment> findByPaymentDateBetweenOrderByPaymentDateAsc(LocalDate start, LocalDate end);
}
