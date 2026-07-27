package com.sivan.cranemanagement.controller;

import com.sivan.cranemanagement.service.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {

    private final DashboardService dashboardService;
    private final BookingService bookingService;
    private final CraneService craneService;

    public DashboardController(DashboardService dashboardService, BookingService bookingService,
                                CraneService craneService) {
        this.dashboardService = dashboardService;
        this.bookingService = bookingService;
        this.craneService = craneService;
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("totalCranes", dashboardService.totalCranes());
        model.addAttribute("todayBookings", dashboardService.todayBookings());
        model.addAttribute("todayIncome", dashboardService.todayIncome());
        model.addAttribute("pendingPayments", dashboardService.pendingPayments());
        model.addAttribute("monthlyIncome", dashboardService.monthlyIncome());
        model.addAttribute("totalExpenses", dashboardService.totalExpensesThisMonth());

        model.addAttribute("completedBookings", dashboardService.completedBookings());
        model.addAttribute("inProgressBookings", dashboardService.inProgressBookings());
        model.addAttribute("cancelledBookings", dashboardService.cancelledBookings());

        model.addAttribute("recentBookings", bookingService.findRecent5());
        model.addAttribute("cranes", craneService.findAll());

        return "dashboard";
    }
}
