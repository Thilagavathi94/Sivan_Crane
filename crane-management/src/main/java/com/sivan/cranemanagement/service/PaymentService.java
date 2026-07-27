package com.sivan.cranemanagement.service;

import com.sivan.cranemanagement.model.Invoice;
import com.sivan.cranemanagement.model.Payment;
import com.sivan.cranemanagement.repository.InvoiceRepository;
import com.sivan.cranemanagement.repository.PaymentRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final InvoiceRepository invoiceRepository;

    public PaymentService(PaymentRepository paymentRepository, InvoiceRepository invoiceRepository) {
        this.paymentRepository = paymentRepository;
        this.invoiceRepository = invoiceRepository;
    }

    public List<Payment> findAll() {
        return paymentRepository.findAllByOrderByIdDesc();
    }

    public Payment findById(Long id) {
        return paymentRepository.findById(id).orElseThrow(() ->
                new RuntimeException("Payment not found: " + id));
    }

    /**
     * Records a payment against an invoice and recalculates the invoice's
     * received / balance amount and payment status automatically.
     */
    public Payment save(Payment payment) {
        if (payment.getReceivedAmount() == null) {
            payment.setReceivedAmount(BigDecimal.ZERO);
        }
        Payment saved = paymentRepository.save(payment);

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

        return saved;
    }

    public long count() {
        return paymentRepository.count();
    }
}
