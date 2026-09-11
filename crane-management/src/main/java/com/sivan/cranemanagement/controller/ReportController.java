package com.sivan.cranemanagement.controller;

import com.sivan.cranemanagement.model.Crane;
import com.sivan.cranemanagement.model.Expense;
import com.sivan.cranemanagement.model.Invoice;
import com.sivan.cranemanagement.model.Payment;
import com.sivan.cranemanagement.model.TripSheet;
import com.sivan.cranemanagement.service.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Controller
public class ReportController {

    private final BookingService bookingService;
    private final InvoiceService invoiceService;
    private final ExpenseService expenseService;
    private final CustomerService customerService;
    private final CraneService craneService;
    private final DriverService driverService;
    private final PaymentService paymentService;
    private final TripSheetService tripSheetService;
    private final PdfService pdfService;
    private final AppSettingsService appSettingsService;

    public ReportController(BookingService bookingService, InvoiceService invoiceService,
                             ExpenseService expenseService, CustomerService customerService,
                             CraneService craneService, DriverService driverService,
                             PaymentService paymentService, TripSheetService tripSheetService,
                             PdfService pdfService, AppSettingsService appSettingsService) {
        this.bookingService = bookingService;
        this.invoiceService = invoiceService;
        this.expenseService = expenseService;
        this.customerService = customerService;
        this.craneService = craneService;
        this.driverService = driverService;
        this.paymentService = paymentService;
        this.tripSheetService = tripSheetService;
        this.pdfService = pdfService;
        this.appSettingsService = appSettingsService;
    }

    @GetMapping("/reports")
    public String reports(@RequestParam(required = false) String month,
                          @RequestParam(required = false) Long craneId,
                          @RequestParam(required = false) String financialYear,
                          Model model) {
        Map<String, Object> data = buildReportModel(month, craneId, financialYear);
        data.forEach(model::addAttribute);
        return "reports";
    }

    // Generates a real PDF file (rendered with a headless browser, same approach
    // used for invoices/quotations) so the Reports page's PDF buttons actually
    // download a .pdf file instead of relying on window.print().
    // section=gst -> only the selected month's GST Bill Details table is included in the PDF.
    @GetMapping("/reports/download")
    public ResponseEntity<byte[]> download(@RequestParam(required = false) String month,
                                            @RequestParam(required = false) Long craneId,
                                            @RequestParam(required = false) String section,
                                            @RequestParam(required = false) String financialYear,
                                            HttpServletRequest request) {
        boolean gstOnly = "gst".equalsIgnoreCase(section);
        Map<String, Object> data = buildReportModel(month, craneId, financialYear);

        byte[] pdf = pdfService.renderPdf(gstOnly ? "gst-report-print" : "monthly-report-print", data, baseUrl(request));

        String fileName = gstOnly
                ? "GST-Bill-Report-" + data.get("month") + ".pdf"
                : "Monthly-Report-" + data.get("month") + ".pdf";

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(fileName, StandardCharsets.UTF_8)
                        .build()
                        .toString())
                .body(pdf);
    }

    private String baseUrl(HttpServletRequest request) {
        return request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort();
    }

    private Map<String, Object> buildReportModel(String month, Long craneId, String financialYear) {
        YearMonth selectedMonth = (month != null && !month.isBlank()) ? YearMonth.parse(month) : YearMonth.now();
        String selectedFinancialYear = (financialYear == null || financialYear.isBlank())
                ? appSettingsService.getSettings().getCurrentFinancialYear()
                : financialYear;
        LocalDate start = selectedMonth.atDay(1);
        LocalDate end = selectedMonth.atEndOfMonth();

        List<Invoice> invoices = invoiceService.findBetween(start, end);
        List<Invoice> gstBillInvoices = invoices.stream()
                .filter(invoice -> matchesSelectedCrane(invoice, craneId))
                .toList();
        List<Payment> payments = paymentService.findBetween(start, end);
        List<Payment> gstPayments = payments.stream()
                .filter(payment -> payment.getInvoice() != null)
                .toList();
        List<Payment> regularPayments = payments.stream()
                .filter(payment -> payment.getTripSheet() != null)
                .toList();
        List<Expense> expenses = expenseService.findBetween(start, end);

        BigDecimal totalIncome = invoices.stream().map(Invoice::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalGst = invoices.stream()
                .map(inv -> inv.getCgstAmount().add(inv.getSgstAmount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalExpenses = expenses.stream().map(Expense::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal gstBillValue = gstBillInvoices.stream().map(Invoice::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal gstBillTaxable = gstBillInvoices.stream().map(Invoice::getTaxableAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal gstBillCgst = gstBillInvoices.stream().map(Invoice::getCgstAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal gstBillSgst = gstBillInvoices.stream().map(Invoice::getSgstAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal gstBillTax = gstBillInvoices.stream()
                .map(invoice -> invoice.getCgstAmount().add(invoice.getSgstAmount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, Object> model = new LinkedHashMap<>();
        model.put("totalBookings", bookingService.count());
        model.put("totalInvoices", invoices.size());
        model.put("totalIncome", totalIncome);
        model.put("totalGst", totalGst);
        model.put("totalExpenses", totalExpenses);
        model.put("netProfit", totalIncome.subtract(totalExpenses));
        model.put("totalCustomers", customerService.count());
        model.put("totalCranes", craneService.count());

        model.put("month", selectedMonth.toString());
        model.put("financialYear", selectedFinancialYear);
        model.put("financialYears", financialYears(selectedFinancialYear));
        model.put("selectedCraneId", craneId);
        model.put("dailySummaries", buildDailySummaries(selectedMonth, invoices, expenses));
        model.put("craneSummaries", buildCraneSummaries(craneService.findAll(), invoices, expenses));
        model.put("invoices", invoices);
        model.put("gstBillInvoices", gstBillInvoices);
        model.put("gstBillValue", gstBillValue);
        model.put("gstBillTaxable", gstBillTaxable);
        model.put("gstBillCgst", gstBillCgst);
        model.put("gstBillSgst", gstBillSgst);
        model.put("gstBillTax", gstBillTax);
        model.put("payments", payments);
        model.put("gstPayments", gstPayments);
        model.put("regularPayments", regularPayments);
        model.put("pendingInvoices", invoiceService.findPending());
        model.put("bookings", bookingService.findAll());
        model.put("customers", customerService.findAll());
        model.put("cranes", craneService.findAll());
        model.put("drivers", driverService.findAll());
        model.put("expenses", expenses);
        model.put("driverExpenses", expenses.stream()
                .filter(expense -> expense.getDriver() != null)
                .toList());
        model.put("today", LocalDate.now());

        return model;
    }

    private List<String> financialYears(String selectedFinancialYear) {
        List<String> years = new ArrayList<>(invoiceService.findFinancialYears());
        if (!years.contains(selectedFinancialYear)) {
            years.add(0, selectedFinancialYear);
        }
        return years;
    }

    private boolean matchesSelectedCrane(Invoice invoice, Long craneId) {
        if (craneId == null) {
            return true;
        }
        Crane selectedCrane = craneService.findById(craneId);
        if (belongsToCrane(invoice, selectedCrane)) {
            return true;
        }
        String selectedCraneNo = normalizeCraneNo(selectedCrane.getCraneNo());
        String selectedRegistrationNo = normalizeCraneNo(selectedCrane.getRegistrationNo());
        if (invoice.getBooking() == null) {
            return false;
        }
        for (TripSheet tripSheet : tripSheetService.findByBookingId(invoice.getBooking().getId())) {
            if (tripSheet.getCrane() != null && craneId.equals(tripSheet.getCrane().getId())) {
                return true;
            }
            String tripCraneNo = tripSheet.getCrane() == null ? "" : normalizeCraneNo(tripSheet.getCrane().getCraneNo());
            String tripRegistrationNo = tripSheet.getCrane() == null ? "" : normalizeCraneNo(tripSheet.getCrane().getRegistrationNo());
            if ((!selectedCraneNo.isBlank() && selectedCraneNo.equals(tripCraneNo))
                    || (!selectedRegistrationNo.isBlank() && selectedRegistrationNo.equals(tripRegistrationNo))) {
                return true;
            }
        }
        return false;
    }

    private List<DailySummary> buildDailySummaries(YearMonth selectedMonth, List<Invoice> invoices, List<Expense> expenses) {
        Map<LocalDate, DailySummary> summaries = new LinkedHashMap<>();
        for (int day = 1; day <= selectedMonth.lengthOfMonth(); day++) {
            LocalDate date = selectedMonth.atDay(day);
            summaries.put(date, new DailySummary(date));
        }
        for (Invoice invoice : invoices) {
            if (invoice.getInvoiceDate() == null) {
                continue;
            }
            DailySummary summary = summaries.get(invoice.getInvoiceDate());
            if (summary == null) {
                continue;
            }
            summary.income = summary.income.add(invoice.getTotalAmount());
        }
        for (Expense expense : expenses) {
            if (expense.getExpenseDate() == null) {
                continue;
            }
            DailySummary summary = summaries.get(expense.getExpenseDate());
            if (summary == null) {
                continue;
            }
            summary.expense = summary.expense.add(expense.getAmount());
        }
        return new ArrayList<>(summaries.values());
    }

    private List<CraneSummary> buildCraneSummaries(List<Crane> cranes, List<Invoice> invoices, List<Expense> expenses) {
        List<CraneSummary> summaries = new ArrayList<>();
        for (Crane crane : cranes) {
            CraneSummary summary = new CraneSummary(crane);
            for (Invoice invoice : invoices) {
                if (belongsToCrane(invoice, crane)) {
                    summary.income = summary.income.add(invoice.getTotalAmount());
                    summary.tripCount++;
                }
            }
            for (Expense expense : expenses) {
                if (expense.getCrane() != null && crane.getId().equals(expense.getCrane().getId())) {
                    summary.expense = summary.expense.add(expense.getAmount());
                }
            }
            summaries.add(summary);
        }
        return summaries;
    }

    private boolean belongsToCrane(Invoice invoice, Crane crane) {
        if (invoice.getTripSheet() != null && invoice.getTripSheet().getCrane() != null
                && crane.getId().equals(invoice.getTripSheet().getCrane().getId())) {
            return true;
        }
        String manualCraneNo = normalizeCraneNo(invoice.getManualCraneNo());
        return !manualCraneNo.isBlank()
                && (manualCraneNo.equals(normalizeCraneNo(crane.getCraneNo()))
                || manualCraneNo.equals(normalizeCraneNo(crane.getRegistrationNo())));
    }

    private String normalizeCraneNo(String value) {
        return value == null ? "" : value.replaceAll("[^A-Za-z0-9]", "").toLowerCase(Locale.ROOT);
    }

    public static class DailySummary {
        public final LocalDate date;
        public BigDecimal income = BigDecimal.ZERO;
        public BigDecimal expense = BigDecimal.ZERO;

        public DailySummary(LocalDate date) {
            this.date = date;
        }

        public BigDecimal getProfit() {
            return income.subtract(expense);
        }
    }

    public static class CraneSummary {
        public final Crane crane;
        public BigDecimal income = BigDecimal.ZERO;
        public BigDecimal expense = BigDecimal.ZERO;
        public long tripCount = 0;

        public CraneSummary(Crane crane) {
            this.crane = crane;
        }

        public BigDecimal getProfit() {
            return income.subtract(expense);
        }
    }
}
