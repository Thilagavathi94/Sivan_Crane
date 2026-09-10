package com.sivan.cranemanagement.repository;

import com.sivan.cranemanagement.model.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.time.LocalDate;
import java.util.List;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
    List<Invoice> findAllByOrderByIdDesc();
    List<Invoice> findByPaymentStatusNot(String status);
    List<Invoice> findByInvoiceDateBetween(LocalDate start, LocalDate end);
    List<Invoice> findByFinancialYearOrderByInvoiceDateAsc(String financialYear);
    @Query("select distinct i.financialYear from Invoice i where i.financialYear is not null and i.financialYear <> '' order by i.financialYear desc")
    List<String> findDistinctFinancialYears();
    List<Invoice> findByBookingIdOrderByIdDesc(Long bookingId);
    List<Invoice> findByTripSheetIdOrderByIdDesc(Long tripSheetId);
}
