package com.sivan.cranemanagement.controller;

import com.sivan.cranemanagement.model.Invoice;
import com.sivan.cranemanagement.model.InvoiceItem;
import com.sivan.cranemanagement.service.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/invoices")
public class InvoiceController {

    private final InvoiceService invoiceService;
    private final BookingService bookingService;
    private final CustomerService customerService;
    private final TripSheetService tripSheetService;

    public InvoiceController(InvoiceService invoiceService, BookingService bookingService, CustomerService customerService,
                              TripSheetService tripSheetService) {
        this.invoiceService = invoiceService;
        this.bookingService = bookingService;
        this.customerService = customerService;
        this.tripSheetService = tripSheetService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("invoices", invoiceService.findAll());
        model.addAttribute("bookings", bookingService.findAll());
        model.addAttribute("customers", customerService.findAll());
        model.addAttribute("tripSheets", tripSheetService.findAll());
        model.addAttribute("invoice", padItems(new Invoice()));
        return "invoices";
    }

    private Invoice padItems(Invoice invoice) {
        while (invoice.getItems().size() < 5) {
            invoice.getItems().add(new InvoiceItem());
        }
        return invoice;
    }

    // "Generate Invoice from Trip Sheet" - pre-fills customer + a crane-hours line item
    @GetMapping("/from-tripsheet/{tripSheetId}")
    public String fromTripSheet(@PathVariable Long tripSheetId,
                                 @RequestParam(defaultValue = "2000") BigDecimal ratePerHour,
                                 @RequestParam(defaultValue = "1500") BigDecimal mobilizationCharge,
                                 Model model) {
        model.addAttribute("invoices", invoiceService.findAll());
        model.addAttribute("bookings", bookingService.findAll());
        model.addAttribute("customers", customerService.findAll());
        model.addAttribute("tripSheets", tripSheetService.findAll());
        model.addAttribute("invoice", padItems(invoiceService.buildFromTripSheet(tripSheetId, ratePerHour, mobilizationCharge)));
        return "invoices";
    }

    @GetMapping("/from-booking/{bookingId}")
    public String fromBooking(@PathVariable Long bookingId, Model model) {
        model.addAttribute("invoices", invoiceService.findAll());
        model.addAttribute("bookings", bookingService.findAll());
        model.addAttribute("customers", customerService.findAll());
        model.addAttribute("tripSheets", tripSheetService.findAll());
        model.addAttribute("invoice", padItems(invoiceService.buildFromBooking(bookingId)));
        return "invoices";
    }

    @PostMapping("/save")
    public String save(@ModelAttribute Invoice invoice) {
        List<InvoiceItem> cleaned = new ArrayList<>();
        for (InvoiceItem item : invoice.getItems()) {
            if (item.getDescription() != null && !item.getDescription().isBlank()) {
                cleaned.add(item);
            }
        }
        invoice.setItems(cleaned);
        invoiceService.save(invoice);
        return "redirect:/invoices";
    }

    @GetMapping("/edit/{id}")
    public String edit(@PathVariable Long id, Model model) {
        model.addAttribute("invoices", invoiceService.findAll());
        model.addAttribute("bookings", bookingService.findAll());
        model.addAttribute("customers", customerService.findAll());
        model.addAttribute("tripSheets", tripSheetService.findAll());
        model.addAttribute("invoice", padItems(invoiceService.findById(id)));
        return "invoices";
    }

    @GetMapping("/delete/{id}")
    public String delete(@PathVariable Long id) {
        invoiceService.delete(id);
        return "redirect:/invoices";
    }

    // Clean, print-friendly view (no topbar/sidebar) so it can be printed
    // or saved as a PDF via the browser's "Print -> Save as PDF" option,
    // then shared with the customer over email/WhatsApp/etc.
    @GetMapping("/print/{id}")
    public String print(@PathVariable Long id, Model model) {
        model.addAttribute("invoice", invoiceService.findById(id));
        return "invoice-print";
    }
}
