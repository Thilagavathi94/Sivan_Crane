package com.sivan.cranemanagement.controller;

import com.sivan.cranemanagement.model.Booking;
import com.sivan.cranemanagement.model.Quotation;
import com.sivan.cranemanagement.model.QuotationItem;
import com.sivan.cranemanagement.service.BookingService;
import com.sivan.cranemanagement.service.CustomerService;
import com.sivan.cranemanagement.service.QuotationService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/quotations")
public class QuotationController {

    private final QuotationService quotationService;
    private final CustomerService customerService;
    private final BookingService bookingService;

    public QuotationController(QuotationService quotationService, CustomerService customerService,
                                BookingService bookingService) {
        this.quotationService = quotationService;
        this.customerService = customerService;
        this.bookingService = bookingService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("quotations", quotationService.findAll());
        model.addAttribute("customers", customerService.findAll());
        model.addAttribute("quotation", padItems(new Quotation()));
        return "quotations";
    }

    private Quotation padItems(Quotation quotation) {
        while (quotation.getItems().size() < 5) {
            quotation.getItems().add(new QuotationItem());
        }
        return quotation;
    }

    @PostMapping("/save")
    public String save(@ModelAttribute Quotation quotation) {
        // Remove blank rows the user didn't fill in
        List<QuotationItem> cleaned = new ArrayList<>();
        for (QuotationItem item : quotation.getItems()) {
            if (item.getDescription() != null && !item.getDescription().isBlank()) {
                cleaned.add(item);
            }
        }
        quotation.setItems(cleaned);
        quotationService.save(quotation);
        return "redirect:/quotations";
    }

    @GetMapping("/edit/{id}")
    public String edit(@PathVariable Long id, Model model) {
        model.addAttribute("quotations", quotationService.findAll());
        model.addAttribute("customers", customerService.findAll());
        model.addAttribute("quotation", padItems(quotationService.findById(id)));
        return "quotations";
    }

    @GetMapping("/delete/{id}")
    public String delete(@PathVariable Long id) {
        quotationService.delete(id);
        return "redirect:/quotations";
    }

    // Clean, print-friendly view (no topbar/sidebar) so it can be printed
    // or saved as a PDF via the browser's "Print -> Save as PDF" option,
    // then shared with the customer over email/WhatsApp/etc.
    @GetMapping("/print/{id}")
    public String print(@PathVariable Long id, Model model) {
        model.addAttribute("quotation", quotationService.findById(id));
        return "quotation-print";
    }

    // "Customer Accepts" -> converts the quotation into a Booking automatically
    @GetMapping("/convert-to-booking/{id}")
    public String convertToBooking(@PathVariable Long id) {
        Quotation quotation = quotationService.findById(id);
        quotation.setStatus("Converted");
        quotationService.save(quotation);

        Booking booking = new Booking();
        booking.setCustomer(quotation.getCustomer());
        booking.setBookingDate(LocalDate.now());
        booking.setWorkType("From Quotation " + quotation.getQuotationNo());
        booking.setStatus("Pending");
        bookingService.save(booking);

        return "redirect:/bookings";
    }
}