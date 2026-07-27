package com.sivan.cranemanagement.controller;

import com.sivan.cranemanagement.model.Driver;
import com.sivan.cranemanagement.service.CraneService;
import com.sivan.cranemanagement.service.DriverService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/drivers")
public class DriverController {

    private final DriverService driverService;
    private final CraneService craneService;

    public DriverController(DriverService driverService, CraneService craneService) {
        this.driverService = driverService;
        this.craneService = craneService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("drivers", driverService.findAll());
        model.addAttribute("cranes", craneService.findAll());
        model.addAttribute("driver", new Driver());
        return "drivers";
    }

    @PostMapping("/save")
    public String save(@ModelAttribute Driver driver) {
        driverService.save(driver);
        return "redirect:/drivers";
    }

    @GetMapping("/edit/{id}")
    public String edit(@PathVariable Long id, Model model) {
        model.addAttribute("drivers", driverService.findAll());
        model.addAttribute("cranes", craneService.findAll());
        model.addAttribute("driver", driverService.findById(id));
        return "drivers";
    }

    @GetMapping("/delete/{id}")
    public String delete(@PathVariable Long id) {
        driverService.delete(id);
        return "redirect:/drivers";
    }
}
