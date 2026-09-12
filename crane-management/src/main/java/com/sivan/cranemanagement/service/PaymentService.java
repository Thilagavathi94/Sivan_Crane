package com.sivan.cranemanagement.service;

import com.sivan.cranemanagement.model.*;
import com.sivan.cranemanagement.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

@Service
public class PaymentService {
    private final PaymentRepository paymentRepository;
    private final InvoiceRepository invoiceRepository;
    private final TripSheetRepository tripSheetRepository;
    public PaymentService(PaymentRepository payments, InvoiceRepository invoices, TripSheetRepository trips) {
        paymentRepository = payments; invoiceRepository = invoices; tripSheetRepository = trips;
    }
    public List<Payment> findAll() { return paymentRepository.findAllByOrderByIdDesc(); }
    public List<Payment> findGstPaymentsBetween(LocalDate start, LocalDate end) {
        return paymentRepository.findByInvoiceIsNotNullAndPaymentDateBetweenOrderByPaymentDateAsc(start, end);
    }
    public List<Payment> findBetween(LocalDate start, LocalDate end) {
        return paymentRepository.findByPaymentDateBetweenOrderByPaymentDateAsc(start, end);
    }
    public List<TripBalance> regularBalancesBetween(LocalDate start, LocalDate end, Long craneId) {
        return regularBalances().stream()
            .filter(r -> r.trip().getTripDate() != null && !r.trip().getTripDate().isBefore(start) && !r.trip().getTripDate().isAfter(end))
            .filter(r -> craneId == null || r.trip().getCrane() != null && Objects.equals(r.trip().getCrane().getId(), craneId))
            .toList();
    }
    public Payment findById(Long id) { return paymentRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Payment not found")); }
    public long count() { return paymentRepository.count(); }
    public record TripBalance(TripSheet trip, BigDecimal received, BigDecimal balance) {
        public TripSheet getTrip() { return trip; }
        public BigDecimal getReceived() { return received; }
        public BigDecimal getBalance() { return balance; }
        public String getStatus() { return balance.signum() <= 0 ? "Paid" : received.signum() > 0 ? "Partially Paid" : "Pending"; }
    }
    public List<TripBalance> regularBalances() {
        Map<Long, BigDecimal> totals = new HashMap<>();
        for (Payment p : findAll()) {
            if (p.getTripSheet() != null && p.getInvoice() == null)
                totals.merge(p.getTripSheet().getId(), money(p.getReceivedAmount()), BigDecimal::add);
        }
        return tripSheetRepository.findAllByOrderByIdDesc().stream()
            .filter(t -> "Regular".equalsIgnoreCase(t.getBillingType()) && !t.isConvertedToInvoice())
            .map(t -> new TripBalance(t, totals.getOrDefault(t.getId(), BigDecimal.ZERO),
                t.getAmount().subtract(totals.getOrDefault(t.getId(), BigDecimal.ZERO)))) .toList();
    }
    @Transactional
    public Payment save(Payment input) {
        Payment p = input.getId() == null ? new Payment() : findById(input.getId());
        if (input.getId() == null) {
            if ((input.getInvoice() == null) == (input.getTripSheet() == null))
                throw new IllegalArgumentException("Select one invoice or regular trip sheet.");
            p.setInvoice(input.getInvoice() == null ? null : invoiceRepository.findById(input.getInvoice().getId()).orElseThrow());
            p.setTripSheet(input.getTripSheet() == null ? null : tripSheetRepository.findById(input.getTripSheet().getId()).orElseThrow());
        }
        BigDecimal amount = money(input.getReceivedAmount());
        if (amount.signum() < 0) throw new IllegalArgumentException("Received amount cannot be negative.");
        if (!List.of("Paid", "Credit", "Partially Paid", "Part Payment").contains(input.getPaymentType()))
            throw new IllegalArgumentException("Invalid payment type.");
        if ("Credit".equals(input.getPaymentType()) && amount.signum() != 0)
            throw new IllegalArgumentException("Credit means no money received. Enter zero, or choose Partially Paid for money received.");
        BigDecimal total;
        if (p.getInvoice() != null) total = money(p.getInvoice().getTotalAmount());
        else {
            TripSheet trip = p.getTripSheet();
            if (!"Regular".equalsIgnoreCase(trip.getBillingType()) || trip.isConvertedToInvoice())
                throw new IllegalArgumentException("This trip must be paid against its invoice.");
            total = trip.getAmount();
        }
        BigDecimal others = findAll().stream().filter(x -> !Objects.equals(x.getId(), p.getId()))
            .filter(x -> p.getInvoice() != null ? x.getInvoice() != null && Objects.equals(x.getInvoice().getId(), p.getInvoice().getId())
                : x.getInvoice() == null && x.getTripSheet() != null && Objects.equals(x.getTripSheet().getId(), p.getTripSheet().getId()))
            .map(x -> money(x.getReceivedAmount())).reduce(BigDecimal.ZERO, BigDecimal::add);
        if (amount.add(others).compareTo(total) > 0) throw new IllegalArgumentException("Received amount exceeds the remaining balance.");
        p.setReceivedAmount(amount);
        p.setPaymentDate(input.getPaymentDate() == null ? LocalDate.now() : input.getPaymentDate());
        p.setPaymentMode(input.getPaymentMode()); p.setNotes(input.getNotes());
        p.setPaymentType(amount.signum() == 0 ? "Credit" : amount.add(others).compareTo(total) >= 0 ? "Paid" : "Partially Paid");
        Payment saved = paymentRepository.save(p);
        if (p.getInvoice() != null) {
            Invoice invoice = p.getInvoice();
            invoice.setReceivedAmount(others.add(amount));
            invoice.setBalanceAmount(total.subtract(others).subtract(amount));
            invoice.setPaymentStatus(others.add(amount).signum() == 0 ? "Pending" : invoice.getBalanceAmount().signum() <= 0 ? "Paid" : "Partially Paid");
            invoiceRepository.save(invoice);
        }
        return saved;
    }
    private static BigDecimal money(BigDecimal value) { return value == null ? BigDecimal.ZERO : value; }
}
