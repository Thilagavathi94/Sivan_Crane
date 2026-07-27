package com.sivan.cranemanagement.controller;

import com.sivan.cranemanagement.model.Customer;
import com.sivan.cranemanagement.service.CustomerService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/customers")
public class CustomerController {

    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("customers", customerService.findAll());
        model.addAttribute("customer", new Customer());
        return "customers";
    }

    @PostMapping("/save")
    public String save(@ModelAttribute Customer customer) {
        customerService.save(customer);
        return "redirect:/customers";
    }

    @GetMapping("/edit/{id}")
    public String edit(@PathVariable Long id, Model model) {
        model.addAttribute("customers", customerService.findAll());
        model.addAttribute("customer", customerService.findById(id));
        return "customers";
    }

    @GetMapping("/delete/{id}")
    public String delete(@PathVariable Long id) {
        customerService.delete(id);
        return "redirect:/customers";
    }
}
