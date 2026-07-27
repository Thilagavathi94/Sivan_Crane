package com.sivan.cranemanagement.controller;

import com.sivan.cranemanagement.model.TripSheet;
import com.sivan.cranemanagement.service.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/tripsheets")
public class TripSheetController {

    private final TripSheetService tripSheetService;
    private final BookingService bookingService;
    private final CustomerService customerService;
    private final CraneService craneService;
    private final DriverService driverService;

    public TripSheetController(TripSheetService tripSheetService, BookingService bookingService,
                                CustomerService customerService, CraneService craneService,
                                DriverService driverService) {
        this.tripSheetService = tripSheetService;
        this.bookingService = bookingService;
        this.customerService = customerService;
        this.craneService = craneService;
        this.driverService = driverService;
    }

    @GetMapping
    public String list(@RequestParam(required = false) Long craneId, Model model) {
        model.addAttribute("tripSheets", craneId != null ? tripSheetService.findByCraneId(craneId) : tripSheetService.findAll());
        model.addAttribute("bookings", bookingService.findAll());
        model.addAttribute("customers", customerService.findAll());
        model.addAttribute("cranes", craneService.findAll());
        model.addAttribute("drivers", driverService.findAll());
        model.addAttribute("tripSheet", new TripSheet());
        if (craneId != null) {
            model.addAttribute("selectedCrane", craneService.findById(craneId));
            model.addAttribute("craneTripCount", tripSheetService.countByCraneId(craneId));
        }
        return "tripsheets";
    }

    // "Convert Booking to Trip Sheet" - pre-fills the form from an existing booking
    @GetMapping("/from-booking/{bookingId}")
    public String fromBooking(@PathVariable Long bookingId, Model model) {
        model.addAttribute("tripSheets", tripSheetService.findAll());
        model.addAttribute("bookings", bookingService.findAll());
        model.addAttribute("customers", customerService.findAll());
        model.addAttribute("cranes", craneService.findAll());
        model.addAttribute("drivers", driverService.findAll());
        model.addAttribute("tripSheet", tripSheetService.buildFromBooking(bookingId));
        return "tripsheets";
    }

    @PostMapping("/save")
    public String save(@ModelAttribute TripSheet tripSheet) {
        tripSheetService.save(tripSheet);
        return "redirect:/tripsheets";
    }

    @GetMapping("/edit/{id}")
    public String edit(@PathVariable Long id, Model model) {
        model.addAttribute("tripSheets", tripSheetService.findAll());
        model.addAttribute("bookings", bookingService.findAll());
        model.addAttribute("customers", customerService.findAll());
        model.addAttribute("cranes", craneService.findAll());
        model.addAttribute("drivers", driverService.findAll());
        model.addAttribute("tripSheet", tripSheetService.findById(id));
        return "tripsheets";
    }

    @GetMapping("/delete/{id}")
    public String delete(@PathVariable Long id) {
        tripSheetService.delete(id);
        return "redirect:/tripsheets";
    }

    // Clean, print-friendly view (no topbar/sidebar) so it can be printed
    // or saved as a PDF via the browser's "Print -> Save as PDF" option.
    @GetMapping("/print/{id}")
    public String print(@PathVariable Long id, Model model) {
        model.addAttribute("tripSheet", tripSheetService.findById(id));
        return "tripsheet-print";
    }
}
