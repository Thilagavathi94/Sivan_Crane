package com.sivan.cranemanagement.service;

import com.sivan.cranemanagement.repository.BookingRepository;
import com.sivan.cranemanagement.repository.TripSheetRepository;
import org.springframework.stereotype.Service;

/**
 * Generates the human-friendly document numbers used across the system:
 * Booking -> BK-00001
 * Trip Sheet -> TS-00001
 * Quotation -> QUO-001
 * Invoice -> INV-001
 */
@Service
public class NumberGeneratorService {

    private final BookingRepository bookingRepository;
    private final TripSheetRepository tripSheetRepository;
    private final AppSettingsService appSettingsService;

    public NumberGeneratorService(BookingRepository bookingRepository,
                                   TripSheetRepository tripSheetRepository,
                                   AppSettingsService appSettingsService) {
        this.bookingRepository = bookingRepository;
        this.tripSheetRepository = tripSheetRepository;
        this.appSettingsService = appSettingsService;
    }

    public String nextBookingNo() {
        long next = bookingRepository.count() + 1;
        return "BK-" + pad(next, 5);
    }

    public String nextTripSheetNo() {
        long next = tripSheetRepository.count() + 1;
        return "TS-" + pad(next, 5);
    }

    public String nextQuotationNo() {
        return appSettingsService.nextQuotationNo();
    }

    public String nextInvoiceNo() {
        return appSettingsService.nextInvoiceNo();
    }

    private String pad(long number, int width) {
        return String.format("%0" + width + "d", number);
    }
}
