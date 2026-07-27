package com.sivan.cranemanagement.controller;

import com.sivan.cranemanagement.model.Booking;
import com.sivan.cranemanagement.service.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/bookings")
public class BookingController {

    private final BookingService bookingService;
    private final CustomerService customerService;
    private final CraneService craneService;
    private final DriverService driverService;

    public BookingController(BookingService bookingService, CustomerService customerService,
                              CraneService craneService, DriverService driverService) {
        this.bookingService = bookingService;
        this.customerService = customerService;
        this.craneService = craneService;
        this.driverService = driverService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("bookings", bookingService.findAll());
        model.addAttribute("customers", customerService.findAll());
        model.addAttribute("cranes", craneService.findAll());
        model.addAttribute("drivers", driverService.findAll());
        model.addAttribute("booking", new Booking());
        return "bookings";
    }

    @PostMapping("/save")
    public String save(@ModelAttribute Booking booking) {
        bookingService.save(booking);
        return "redirect:/bookings";
    }

    @GetMapping("/edit/{id}")
    public String edit(@PathVariable Long id, Model model) {
        model.addAttribute("bookings", bookingService.findAll());
        model.addAttribute("customers", customerService.findAll());
        model.addAttribute("cranes", craneService.findAll());
        model.addAttribute("drivers", driverService.findAll());
        model.addAttribute("booking", bookingService.findById(id));
        return "bookings";
    }

    @GetMapping("/delete/{id}")
    public String delete(@PathVariable Long id) {
        bookingService.delete(id);
        return "redirect:/bookings";
    }
}
