package com.sivan.cranemanagement.controller;

import com.sivan.cranemanagement.model.Crane;
import com.sivan.cranemanagement.model.Expense;
import com.sivan.cranemanagement.model.Invoice;
import com.sivan.cranemanagement.service.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Controller
public class ReportController {

    private final BookingService bookingService;
    private final InvoiceService invoiceService;
    private final ExpenseService expenseService;
    private final CustomerService customerService;
    private final CraneService craneService;
    private final DriverService driverService;

    public ReportController(BookingService bookingService, InvoiceService invoiceService,
                             ExpenseService expenseService, CustomerService customerService,
                             CraneService craneService, DriverService driverService) {
        this.bookingService = bookingService;
        this.invoiceService = invoiceService;
        this.expenseService = expenseService;
        this.customerService = customerService;
        this.craneService = craneService;
        this.driverService = driverService;
    }

    @GetMapping("/reports")
    public String reports(@RequestParam(required = false) String month, Model model) {
        YearMonth selectedMonth = (month != null && !month.isBlank()) ? YearMonth.parse(month) : YearMonth.now();
        LocalDate start = selectedMonth.atDay(1);
        LocalDate end = selectedMonth.atEndOfMonth();

        List<Invoice> invoices = invoiceService.findBetween(start, end);
        List<Expense> expenses = expenseService.findBetween(start, end);

        BigDecimal totalIncome = invoices.stream().map(Invoice::getReceivedAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalPending = invoices.stream().map(Invoice::getBalanceAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalGst = invoices.stream()
                .map(inv -> inv.getCgstAmount().add(inv.getSgstAmount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalExpenses = expenses.stream().map(Expense::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        model.addAttribute("totalBookings", bookingService.count());
        model.addAttribute("totalInvoices", invoices.size());
        model.addAttribute("totalIncome", totalIncome);
        model.addAttribute("totalPending", totalPending);
        model.addAttribute("totalGst", totalGst);
        model.addAttribute("totalExpenses", totalExpenses);
        model.addAttribute("netProfit", totalIncome.subtract(totalExpenses));
        model.addAttribute("totalCustomers", customerService.count());
        model.addAttribute("totalCranes", craneService.count());

        model.addAttribute("month", selectedMonth.toString());
        model.addAttribute("dailySummaries", buildDailySummaries(selectedMonth, invoices, expenses));
        model.addAttribute("craneSummaries", buildCraneSummaries(craneService.findAll(), invoices, expenses));
        model.addAttribute("invoices", invoices);
        model.addAttribute("pendingInvoices", invoiceService.findPending());
        model.addAttribute("bookings", bookingService.findAll());
        model.addAttribute("customers", customerService.findAll());
        model.addAttribute("cranes", craneService.findAll());
        model.addAttribute("drivers", driverService.findAll());
        model.addAttribute("expenses", expenses);
        model.addAttribute("driverExpenses", expenses.stream()
                .filter(expense -> expense.getDriver() != null)
                .toList());
        model.addAttribute("today", LocalDate.now());

        return "reports";
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
            summary.income = summary.income.add(invoice.getReceivedAmount());
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
                if (invoice.getTripSheet() != null && invoice.getTripSheet().getCrane() != null
                        && crane.getId().equals(invoice.getTripSheet().getCrane().getId())) {
                    summary.income = summary.income.add(invoice.getReceivedAmount());
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
