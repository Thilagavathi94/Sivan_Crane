package com.sivan.cranemanagement.controller;

import com.sivan.cranemanagement.model.Expense;
import com.sivan.cranemanagement.service.CraneService;
import com.sivan.cranemanagement.service.DriverService;
import com.sivan.cranemanagement.service.ExpenseService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/expenses")
public class ExpenseController {

    private final ExpenseService expenseService;
    private final CraneService craneService;
    private final DriverService driverService;

    public ExpenseController(ExpenseService expenseService, CraneService craneService, DriverService driverService) {
        this.expenseService = expenseService;
        this.craneService = craneService;
        this.driverService = driverService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("expenses", expenseService.findAll());
        model.addAttribute("cranes", craneService.findAll());
        model.addAttribute("drivers", driverService.findAll());
        model.addAttribute("expense", new Expense());
        return "expenses";
    }

    @PostMapping("/save")
    public String save(@ModelAttribute Expense expense) {
        expenseService.save(expense);
        return "redirect:/expenses";
    }

    @GetMapping("/delete/{id}")
    public String delete(@PathVariable Long id) {
        expenseService.delete(id);
        return "redirect:/expenses";
    }
}
