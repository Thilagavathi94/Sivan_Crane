package com.sivan.cranemanagement.controller;

import com.sivan.cranemanagement.model.Crane;
import com.sivan.cranemanagement.model.TripSheet;
import com.sivan.cranemanagement.service.CraneService;
import com.sivan.cranemanagement.service.TripSheetService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/cranes")
public class CraneController {

    private final CraneService craneService;
    private final TripSheetService tripSheetService;

    public CraneController(CraneService craneService, TripSheetService tripSheetService) {
        this.craneService = craneService;
        this.tripSheetService = tripSheetService;
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
        Map<LocalDate, Long> dateCounts = new LinkedHashMap<>();
        tripSheets.stream()
                .filter(t -> t.getTripDate() != null)
                .sorted((left, right) -> left.getTripDate().compareTo(right.getTripDate()))
                .forEach(t -> dateCounts.merge(t.getTripDate(), 1L, Long::sum));

        model.addAttribute("crane", crane);
        model.addAttribute("tripSheets", tripSheets);
        model.addAttribute("tripCount", tripSheets.size());
        model.addAttribute("dateCounts", dateCounts);
        return "crane-trip-records";
    }
}
