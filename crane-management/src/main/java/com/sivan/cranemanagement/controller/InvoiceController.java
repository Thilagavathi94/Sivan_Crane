package com.sivan.cranemanagement.controller;

import com.sivan.cranemanagement.model.Invoice;
import com.sivan.cranemanagement.model.InvoiceItem;
import com.sivan.cranemanagement.service.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/invoices")
public class InvoiceController {

    private final InvoiceService invoiceService;
    private final BookingService bookingService;
    private final CustomerService customerService;
    private final TripSheetService tripSheetService;
    private final CraneService craneService;
    private final PdfService pdfService;

    public InvoiceController(InvoiceService invoiceService, BookingService bookingService, CustomerService customerService,
                              TripSheetService tripSheetService, CraneService craneService, PdfService pdfService) {
        this.invoiceService = invoiceService;
        this.bookingService = bookingService;
        this.customerService = customerService;
        this.tripSheetService = tripSheetService;
        this.craneService = craneService;
        this.pdfService = pdfService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("invoices", invoiceService.findAll());
        model.addAttribute("bookings", bookingService.findAll());
        model.addAttribute("customers", customerService.findAll());
        model.addAttribute("tripSheets", tripSheetService.findAll());
        model.addAttribute("cranes", craneService.findAll());
        model.addAttribute("invoice", padItems(new Invoice()));
        return "invoices";
    }

    private Invoice padItems(Invoice invoice) {
        boolean useDefaultHireRows = invoice.getTripSheet() == null && invoice.getItems().isEmpty();
        while (invoice.getItems().size() < 5) {
            InvoiceItem item = new InvoiceItem();
            if (useDefaultHireRows) {
                item.setDescription(defaultHireChargeDescription(invoice.getItems().size()));
            }
            invoice.getItems().add(item);
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
        model.addAttribute("cranes", craneService.findAll());
        model.addAttribute("invoice", padItems(invoiceService.buildFromTripSheet(tripSheetId, ratePerHour, mobilizationCharge)));
        return "invoices";
    }

    @GetMapping("/from-booking/{bookingId}")
    public String fromBooking(@PathVariable Long bookingId, Model model) {
        model.addAttribute("invoices", invoiceService.findAll());
        model.addAttribute("bookings", bookingService.findAll());
        model.addAttribute("customers", customerService.findAll());
        model.addAttribute("tripSheets", tripSheetService.findAll());
        model.addAttribute("cranes", craneService.findAll());
        model.addAttribute("invoice", padItems(invoiceService.buildFromBooking(bookingId)));
        return "invoices";
    }

    @PostMapping("/save")
    public String save(@ModelAttribute Invoice invoice) {
        List<InvoiceItem> cleaned = new ArrayList<>();
        for (int i = 0; i < invoice.getItems().size(); i++) {
            InvoiceItem item = invoice.getItems().get(i);
            boolean hasDescription = item.getDescription() != null && !item.getDescription().isBlank();
            boolean hasUnits = item.getHoursOrUnits() != null && item.getHoursOrUnits().compareTo(BigDecimal.ZERO) > 0;
            boolean hasAmount = item.getRate() != null && item.getRate().compareTo(BigDecimal.ZERO) > 0;
            if (hasDescription || hasUnits || hasAmount) {
                if (!hasDescription) {
                    item.setDescription(defaultHireChargeDescription(i));
                }
                if (!hasUnits && hasAmount) {
                    item.setHoursOrUnits(BigDecimal.ONE);
                }
                cleaned.add(item);
            }
        }
        invoice.setItems(cleaned);
        invoiceService.save(invoice);
        return "redirect:/invoices";
    }

    private String defaultHireChargeDescription(int index) {
        return switch (index) {
            case 0 -> "Minimum 1 Hrs Charges";
            case 1 -> "Minimum 2 Hrs Charges";
            case 2 -> "Additional Hrs";
            case 3 -> "Betta";
            default -> "Other Charge";
        };
    }

    @GetMapping("/edit/{id}")
    public String edit(@PathVariable Long id, Model model) {
        model.addAttribute("invoices", invoiceService.findAll());
        model.addAttribute("bookings", bookingService.findAll());
        model.addAttribute("customers", customerService.findAll());
        model.addAttribute("tripSheets", tripSheetService.findAll());
        model.addAttribute("cranes", craneService.findAll());
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

    @GetMapping("/download/{id}")
    public ResponseEntity<byte[]> download(@PathVariable Long id, HttpServletRequest request) {
        Invoice invoice = invoiceService.findById(id);
        byte[] pdf = pdfService.renderPdf("invoice-print", Map.of("invoice", invoice), baseUrl(request));

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(invoice.getInvoiceNo() + ".pdf", StandardCharsets.UTF_8)
                        .build()
                        .toString())
                .body(pdf);
    }

    private String baseUrl(HttpServletRequest request) {
        return request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort();
    }
}
