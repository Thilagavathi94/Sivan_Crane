package com.sivan.cranemanagement.controller;
import com.sivan.cranemanagement.model.*;
import com.sivan.cranemanagement.service.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/payments")
public class PaymentController {
    private final PaymentService paymentService;
    private final InvoiceService invoiceService;
    public PaymentController(PaymentService payments, InvoiceService invoices) { paymentService = payments; invoiceService = invoices; }
    private String page(Model model, Payment payment) {
        model.addAttribute("payments", paymentService.findAll());
        model.addAttribute("pendingInvoices", invoiceService.findAll().stream().filter(i -> i.getBalanceAmount().signum() > 0).toList());
        model.addAttribute("pendingTrips", paymentService.regularBalances().stream().filter(t -> t.balance().signum() > 0).toList());
        model.addAttribute("payment", payment);
        model.addAttribute("selectedReference", payment.getInvoice() != null ? "invoice:" + payment.getInvoice().getId() : payment.getTripSheet() != null ? "trip:" + payment.getTripSheet().getId() : "");
        return "payments";
    }
    @GetMapping public String list(Model model) { return page(model, new Payment()); }
    @GetMapping("/edit/{id}") public String edit(@PathVariable Long id, Model model) { return page(model, paymentService.findById(id)); }
    @GetMapping("/from-invoice/{invoiceId}") public String fromInvoice(@PathVariable Long invoiceId, Model model) {
        Payment p = new Payment(); p.setInvoice(invoiceService.findById(invoiceId)); return page(model, p);
    }
    @PostMapping("/save") public String save(@ModelAttribute Payment payment, @RequestParam(defaultValue="") String reference, RedirectAttributes redirect) {
        try {
            if (payment.getId() == null) {
                payment.setInvoice(null); payment.setTripSheet(null);
                if (reference.startsWith("invoice:")) { Invoice i = new Invoice(); i.setId(Long.valueOf(reference.substring(8))); payment.setInvoice(i); }
                else if (reference.startsWith("trip:")) { TripSheet t = new TripSheet(); t.setId(Long.valueOf(reference.substring(5))); payment.setTripSheet(t); }
            }
            paymentService.save(payment);
        } catch (IllegalArgumentException e) {
            redirect.addFlashAttribute("error", e.getMessage());
            return payment.getId() == null ? "redirect:/payments" : "redirect:/payments/edit/" + payment.getId();
        }
        return "redirect:/payments";
    }
}
