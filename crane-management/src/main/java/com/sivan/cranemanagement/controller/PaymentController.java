package com.sivan.cranemanagement.controller;

import com.sivan.cranemanagement.model.Payment;
import com.sivan.cranemanagement.model.Invoice;
import com.sivan.cranemanagement.model.TripSheet;
import com.sivan.cranemanagement.service.InvoiceService;
import com.sivan.cranemanagement.service.PaymentService;
import com.sivan.cranemanagement.service.TripSheetService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/payments")
public class PaymentController {

    private final PaymentService paymentService;
    private final InvoiceService invoiceService;
    private final TripSheetService tripSheetService;

    public PaymentController(PaymentService paymentService, InvoiceService invoiceService,
                             TripSheetService tripSheetService) {
        this.paymentService = paymentService;
        this.invoiceService = invoiceService;
        this.tripSheetService = tripSheetService;
    }

    @GetMapping
    public String list(Model model) {
        populateModel(model, new Payment());
        return "payments";
    }

    @PostMapping("/save")
    public String save(@ModelAttribute Payment payment,
                       @RequestParam String paymentTarget,
                       Model model) {
        applyPaymentTarget(payment, paymentTarget);
        paymentService.save(payment);
        return "redirect:/payments";
    }

    private void populateModel(Model model, Payment payment) {
        model.addAttribute("payments", paymentService.findAll());
        model.addAttribute("invoices", invoiceService.findAll());
        model.addAttribute("pendingInvoices", invoiceService.findPending());
        model.addAttribute("regularTripSheets", tripSheetService.findRegularTripSheets());
        model.addAttribute("payment", payment);
    }

    private void applyPaymentTarget(Payment payment, String paymentTarget) {
        if (paymentTarget == null || paymentTarget.isBlank()) {
            throw new RuntimeException("Select an invoice number or trip sheet number");
        }
        String[] parts = paymentTarget.split(":", 2);
        if (parts.length != 2) {
            throw new RuntimeException("Invalid payment target");
        }
        Long id = Long.valueOf(parts[1]);
        if ("INV".equals(parts[0])) {
            Invoice invoice = invoiceService.findById(id);
            payment.setInvoice(invoice);
            payment.setTripSheet(null);
        } else if ("TS".equals(parts[0])) {
            TripSheet tripSheet = tripSheetService.findById(id);
            payment.setTripSheet(tripSheet);
            payment.setInvoice(null);
        } else {
            throw new RuntimeException("Invalid payment target");
        }
    }
}
