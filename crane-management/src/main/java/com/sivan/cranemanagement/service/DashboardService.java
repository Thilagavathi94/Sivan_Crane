package com.sivan.cranemanagement.service;

import com.sivan.cranemanagement.model.Invoice;
import com.sivan.cranemanagement.repository.InvoiceRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
public class DashboardService {

    private final CraneService craneService;
    private final BookingService bookingService;
    private final InvoiceRepository invoiceRepository;
    private final ExpenseService expenseService;

    public DashboardService(CraneService craneService, BookingService bookingService,
                             InvoiceRepository invoiceRepository, ExpenseService expenseService) {
        this.craneService = craneService;
        this.bookingService = bookingService;
        this.invoiceRepository = invoiceRepository;
        this.expenseService = expenseService;
    }

    public long totalCranes() {
        return craneService.count();
    }

    public long todayBookings() {
        return bookingService.countToday();
    }

    public BigDecimal todayIncome() {
        LocalDate today = LocalDate.now();
        return invoiceRepository.findByInvoiceDateBetween(today, today).stream()
                .map(Invoice::getReceivedAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal pendingPayments() {
        List<Invoice> pending = invoiceRepository.findByPaymentStatusNot("Paid");
        return pending.stream().map(Invoice::getBalanceAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal monthlyIncome() {
        LocalDate start = LocalDate.now().withDayOfMonth(1);
        LocalDate end = LocalDate.now();
        return invoiceRepository.findByInvoiceDateBetween(start, end).stream()
                .map(Invoice::getReceivedAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal totalExpensesThisMonth() {
        return expenseService.totalThisMonth();
    }

    public long completedBookings() {
        return bookingService.countByStatus("Completed");
    }

    public long inProgressBookings() {
        return bookingService.countByStatus("In Progress");
    }

    public long cancelledBookings() {
        return bookingService.countByStatus("Cancelled");
    }
}
