package com.sivan.cranemanagement.service;

import com.sivan.cranemanagement.model.AppSettings;
import com.sivan.cranemanagement.repository.AppSettingsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
public class AppSettingsService {

    private static final long SETTINGS_ID = 1L;

    private final AppSettingsRepository appSettingsRepository;

    public AppSettingsService(AppSettingsRepository appSettingsRepository) {
        this.appSettingsRepository = appSettingsRepository;
    }

    @Transactional
    public AppSettings getSettings() {
        AppSettings settings = appSettingsRepository.findById(SETTINGS_ID)
                .orElseGet(this::newDefaultSettings);
        String financialYear = currentFinancialYear();
        if (settings.getCurrentFinancialYear() == null || settings.getCurrentFinancialYear().isBlank()) {
            settings.setCurrentFinancialYear(financialYear);
        } else if (!settings.getCurrentFinancialYear().equals(financialYear)) {
            settings.setCurrentFinancialYear(financialYear);
            settings.setNextInvoiceNumber(1);
            settings.setNextQuotationNumber(1);
        }
        return appSettingsRepository.save(settings);
    }

    @Transactional
    public AppSettings saveSettings(AppSettings settings) {
        AppSettings saved = getSettings();
        saved.setCurrentFinancialYear(cleanFinancialYear(settings.getCurrentFinancialYear()));
        saved.setNextInvoiceNumber(Math.max(1, settings.getNextInvoiceNumber()));
        saved.setNextQuotationNumber(Math.max(1, settings.getNextQuotationNumber()));
        return appSettingsRepository.save(saved);
    }

    @Transactional
    public String nextInvoiceNo() {
        AppSettings settings = getSettings();
        String invoiceNo = "INV-" + pad(settings.getNextInvoiceNumber());
        settings.setNextInvoiceNumber(settings.getNextInvoiceNumber() + 1);
        appSettingsRepository.save(settings);
        return invoiceNo;
    }

    @Transactional
    public String nextQuotationNo() {
        AppSettings settings = getSettings();
        String quotationNo = "QUO-" + pad(settings.getNextQuotationNumber());
        settings.setNextQuotationNumber(settings.getNextQuotationNumber() + 1);
        appSettingsRepository.save(settings);
        return quotationNo;
    }

    public String currentFinancialYear() {
        LocalDate today = LocalDate.now();
        int startYear = today.getMonthValue() >= 4 ? today.getYear() : today.getYear() - 1;
        int endYear = startYear + 1;
        return startYear + "-" + String.format("%02d", endYear % 100);
    }

    private AppSettings newDefaultSettings() {
        AppSettings settings = new AppSettings();
        settings.setId(SETTINGS_ID);
        settings.setCurrentFinancialYear(currentFinancialYear());
        return settings;
    }

    private String cleanFinancialYear(String financialYear) {
        if (financialYear == null || financialYear.isBlank()) {
            return currentFinancialYear();
        }
        return financialYear.trim();
    }

    private String pad(int number) {
        return String.format("%03d", number);
    }
}
