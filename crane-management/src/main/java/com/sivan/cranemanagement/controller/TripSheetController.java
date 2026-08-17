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

    public TripSheetController(TripSheetService tripSheetService, BookingService bookingService,
                                CustomerService customerService, CraneService craneService) {
        this.tripSheetService = tripSheetService;
        this.bookingService = bookingService;
        this.customerService = customerService;
        this.craneService = craneService;
    }

    @GetMapping
    public String list(@RequestParam(required = false) Long craneId, Model model) {
        populateFormLists(model);
        if (craneId != null) {
            model.addAttribute("tripSheets", tripSheetService.findByCraneId(craneId));
        }
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
        populateFormLists(model);
        model.addAttribute("tripSheet", tripSheetService.buildFromBooking(bookingId));
        return "tripsheets";
    }

    @PostMapping("/save")
    public String save(@ModelAttribute TripSheet tripSheet, Model model) {
        if ("GST".equalsIgnoreCase(tripSheet.getBillingType()) && !hasCustomerGst(tripSheet)) {
            populateFormLists(model);
            model.addAttribute("tripSheet", tripSheet);
            model.addAttribute("error", "GST trip sheet needs the selected customer's GST number before saving.");
            return "tripsheets";
        }
        tripSheetService.save(tripSheet);
        return "redirect:/tripsheets";
    }

    @GetMapping("/edit/{id}")
    public String edit(@PathVariable Long id, Model model) {
        populateFormLists(model);
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

    private void populateFormLists(Model model) {
        model.addAttribute("tripSheets", tripSheetService.findAll());
        model.addAttribute("bookings", bookingService.findAll());
        model.addAttribute("customers", customerService.findAll());
        model.addAttribute("cranes", craneService.findAll());
    }

    private boolean hasCustomerGst(TripSheet tripSheet) {
        if (tripSheet.getCustomer() == null || tripSheet.getCustomer().getId() == null) {
            return false;
        }
        String gstNumber = customerService.findById(tripSheet.getCustomer().getId()).getGstNumber();
        return gstNumber != null && !gstNumber.trim().isEmpty();
    }
}
