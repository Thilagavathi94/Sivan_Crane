package com.sivan.cranemanagement.service;

import com.sivan.cranemanagement.model.Invoice;
import com.sivan.cranemanagement.model.Payment;
import com.sivan.cranemanagement.model.TripSheet;
import com.sivan.cranemanagement.repository.InvoiceRepository;
import com.sivan.cranemanagement.repository.PaymentRepository;
import com.sivan.cranemanagement.repository.TripSheetRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final InvoiceRepository invoiceRepository;
    private final TripSheetRepository tripSheetRepository;

    public PaymentService(PaymentRepository paymentRepository, InvoiceRepository invoiceRepository,
                          TripSheetRepository tripSheetRepository) {
        this.paymentRepository = paymentRepository;
        this.invoiceRepository = invoiceRepository;
        this.tripSheetRepository = tripSheetRepository;
    }

    public List<Payment> findAll() {
        return paymentRepository.findAllByOrderByIdDesc();
    }

    public List<Payment> findGstPaymentsBetween(LocalDate start, LocalDate end) {
        return paymentRepository.findByInvoiceIsNotNullAndPaymentDateBetweenOrderByPaymentDateAsc(start, end);
    }

    public List<Payment> findBetween(LocalDate start, LocalDate end) {
        return paymentRepository.findByPaymentDateBetweenOrderByPaymentDateAsc(start, end);
    }

    public Payment findById(Long id) {
        return paymentRepository.findById(id).orElseThrow(() ->
                new RuntimeException("Payment not found: " + id));
    }

    /**
     * Records a payment against either an invoice or a regular trip sheet.
     * Invoice payments recalculate received / balance amount automatically.
     */
    public Payment save(Payment payment) {
        if (payment.getReceivedAmount() == null) {
            payment.setReceivedAmount(BigDecimal.ZERO);
        }
        if (payment.getPaymentDate() == null) {
            payment.setPaymentDate(LocalDate.now());
        }
        if (payment.getInvoice() == null && payment.getTripSheet() == null) {
            throw new RuntimeException("Select an invoice number or trip sheet number");
        }
        if (payment.getInvoice() != null && payment.getTripSheet() != null) {
            throw new RuntimeException("Select only one payment target");
        }
        Payment saved = paymentRepository.save(payment);

        if (payment.getInvoice() != null) {
            Invoice invoice = invoiceRepository.findById(payment.getInvoice().getId()).orElseThrow(() ->
                    new RuntimeException("Invoice not found"));

            BigDecimal totalReceived = paymentRepository.findByInvoiceId(invoice.getId()).stream()
                    .map(Payment::getReceivedAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            invoice.setReceivedAmount(totalReceived);
            invoice.setBalanceAmount(invoice.getTotalAmount().subtract(totalReceived));

            if (totalReceived.compareTo(BigDecimal.ZERO) <= 0) {
                invoice.setPaymentStatus("Pending");
            } else if (totalReceived.compareTo(invoice.getTotalAmount()) >= 0) {
                invoice.setPaymentStatus("Paid");
            } else {
                invoice.setPaymentStatus("Partially Paid");
            }
            invoiceRepository.save(invoice);
        } else {
            TripSheet tripSheet = tripSheetRepository.findById(payment.getTripSheet().getId()).orElseThrow(() ->
                    new RuntimeException("Trip Sheet not found"));
            payment.setTripSheet(tripSheet);
        }

        return saved;
    }

    public long count() {
        return paymentRepository.count();
    }
}
