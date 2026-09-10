package com.sivan.cranemanagement.controller;

import com.sivan.cranemanagement.model.AppSettings;
import com.sivan.cranemanagement.service.AppSettingsService;
import com.sivan.cranemanagement.service.BackupService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.nio.file.Path;

@Controller
@RequestMapping("/settings")
public class SettingsController {

    private final AppSettingsService appSettingsService;
    private final BackupService backupService;

    public SettingsController(AppSettingsService appSettingsService, BackupService backupService) {
        this.appSettingsService = appSettingsService;
        this.backupService = backupService;
    }

    @GetMapping
    public String settings(Model model) {
        model.addAttribute("settings", appSettingsService.getSettings());
        model.addAttribute("backupDir", backupService.getBackupDir());
        return "settings";
    }

    @PostMapping("/save-numbering")
    public String saveNumbering(@ModelAttribute AppSettings settings, RedirectAttributes redirectAttributes) {
        appSettingsService.saveSettings(settings);
        redirectAttributes.addFlashAttribute("success", "Number settings saved successfully.");
        return "redirect:/settings";
    }

    @PostMapping("/backup-now")
    public String backupNow(RedirectAttributes redirectAttributes) {
        try {
            Path backupFile = backupService.backupNow();
            redirectAttributes.addFlashAttribute("success", "Backup created: " + backupFile);
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/settings";
    }
}
