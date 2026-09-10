package com.sivan.cranemanagement.controller;

import com.sivan.cranemanagement.model.Crane;
import com.sivan.cranemanagement.model.Invoice;
import com.sivan.cranemanagement.model.TripSheet;
import com.sivan.cranemanagement.service.CraneService;
import com.sivan.cranemanagement.service.InvoiceService;
import com.sivan.cranemanagement.service.TripSheetService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Controller
@RequestMapping("/cranes")
public class CraneController {

    private final CraneService craneService;
    private final TripSheetService tripSheetService;
    private final InvoiceService invoiceService;

    public CraneController(CraneService craneService, TripSheetService tripSheetService,
                           InvoiceService invoiceService) {
        this.craneService = craneService;
        this.tripSheetService = tripSheetService;
        this.invoiceService = invoiceService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("cranes", craneService.findAll());
        model.addAttribute("crane", new Crane());
        return "cranes";
    }

    @PostMapping("/save")
    public String save(@ModelAttribute Crane crane) {
        craneService.save(crane);
        return "redirect:/cranes";
    }

    @GetMapping("/edit/{id}")
    public String edit(@PathVariable Long id, Model model) {
        model.addAttribute("cranes", craneService.findAll());
        model.addAttribute("crane", craneService.findById(id));
        return "cranes";
    }

    @GetMapping("/delete/{id}")
    public String delete(@PathVariable Long id) {
        craneService.delete(id);
        return "redirect:/cranes";
    }

    @GetMapping("/{id}/trip-records")
    public String tripRecords(@PathVariable Long id, Model model) {
        Crane crane = craneService.findById(id);
        List<TripSheet> tripSheets = tripSheetService.findByCraneId(id);
        List<CraneTripRecord> records = new ArrayList<>();
        for (TripSheet tripSheet : tripSheets) {
            records.add(CraneTripRecord.fromTripSheet(tripSheet));
        }
        invoiceService.findAll().stream()
                .filter(invoice -> invoice.getTripSheet() == null)
                .filter(invoice -> belongsToCrane(invoice, crane))
                .map(CraneTripRecord::fromInvoice)
                .forEach(records::add);
        records.sort(Comparator.comparing(CraneTripRecord::getDate,
                Comparator.nullsLast(Comparator.reverseOrder())));

        Map<LocalDate, Long> dateCounts = new LinkedHashMap<>();
        records.stream()
                .filter(record -> record.getDate() != null)
                .forEach(record -> dateCounts.merge(record.getDate(), 1L, Long::sum));

        model.addAttribute("crane", crane);
        model.addAttribute("records", records);
        model.addAttribute("tripCount", records.size());
        model.addAttribute("dateCounts", dateCounts);
        return "crane-trip-records";
    }

    private boolean belongsToCrane(Invoice invoice, Crane crane) {
        String manualCraneNo = normalizeCraneNo(invoice.getManualCraneNo());
        return !manualCraneNo.isBlank()
                && (manualCraneNo.equals(normalizeCraneNo(crane.getCraneNo()))
                || manualCraneNo.equals(normalizeCraneNo(crane.getRegistrationNo())));
    }

    private String normalizeCraneNo(String value) {
        return value == null ? "" : value.replaceAll("[^A-Za-z0-9]", "").toLowerCase(Locale.ROOT);
    }

    public static class CraneTripRecord {
        private final LocalDate date;
        private final String party;
        private final BigDecimal hours;
        private final BigDecimal amount;
        private final String printUrl;
        private final String printLabel;

        private CraneTripRecord(LocalDate date, String party, BigDecimal hours, BigDecimal amount,
                                String printUrl, String printLabel) {
            this.date = date;
            this.party = party;
            this.hours = hours != null ? hours : BigDecimal.ZERO;
            this.amount = amount != null ? amount : BigDecimal.ZERO;
            this.printUrl = printUrl;
            this.printLabel = printLabel;
        }

        public static CraneTripRecord fromTripSheet(TripSheet tripSheet) {
            return new CraneTripRecord(tripSheet.getTripDate(),
                    tripSheet.getCustomer() != null ? tripSheet.getCustomer().getName() : "-",
                    tripSheet.getTotalHours(), tripSheet.getAmount(),
                    "/tripsheets/print/" + tripSheet.getId(), "PDF");
        }

        public static CraneTripRecord fromInvoice(Invoice invoice) {
            return new CraneTripRecord(invoice.getManualTripDate() != null ? invoice.getManualTripDate() : invoice.getInvoiceDate(),
                    invoice.getCustomer() != null ? invoice.getCustomer().getName() : "-",
                    invoice.getManualRunningHours(), invoice.getTotalAmount(),
                    "/invoices/print/" + invoice.getId(), "Invoice");
        }

        public LocalDate getDate() { return date; }
        public String getParty() { return party; }
        public BigDecimal getHours() { return hours; }
        public BigDecimal getAmount() { return amount; }
        public String getPrintUrl() { return printUrl; }
        public String getPrintLabel() { return printLabel; }
    }
}
