package com.sivan.cranemanagement.controller;

import com.sivan.cranemanagement.model.Payment;
import com.sivan.cranemanagement.service.InvoiceService;
import com.sivan.cranemanagement.service.PaymentService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/payments")
public class PaymentController {

    private final PaymentService paymentService;
    private final InvoiceService invoiceService;

    public PaymentController(PaymentService paymentService, InvoiceService invoiceService) {
        this.paymentService = paymentService;
        this.invoiceService = invoiceService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("payments", paymentService.findAll());
        model.addAttribute("invoices", invoiceService.findAll());
        model.addAttribute("pendingInvoices", invoiceService.findPending());
        model.addAttribute("payment", new Payment());
        return "payments";
    }

    @PostMapping("/save")
    public String save(@ModelAttribute Payment payment) {
        paymentService.save(payment);
        return "redirect:/payments";
    }
}
