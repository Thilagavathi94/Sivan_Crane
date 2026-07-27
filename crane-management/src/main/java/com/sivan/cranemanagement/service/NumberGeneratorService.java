package com.sivan.cranemanagement.service;

import com.sivan.cranemanagement.repository.*;
import org.springframework.stereotype.Service;
import java.time.Year;

/**
 * Generates the human-friendly document numbers used across the system:
 * Booking -> BK-00001
 * Trip Sheet -> TS-00001
 * Quotation -> QUO-00001
 * Invoice -> INV-2026-00001 (year-based, as GST invoices typically are)
 */
@Service
public class NumberGeneratorService {

    private final BookingRepository bookingRepository;
    private final TripSheetRepository tripSheetRepository;
    private final QuotationRepository quotationRepository;
    private final InvoiceRepository invoiceRepository;

    public NumberGeneratorService(BookingRepository bookingRepository,
                                   TripSheetRepository tripSheetRepository,
                                   QuotationRepository quotationRepository,
                                   InvoiceRepository invoiceRepository) {
        this.bookingRepository = bookingRepository;
        this.tripSheetRepository = tripSheetRepository;
        this.quotationRepository = quotationRepository;
        this.invoiceRepository = invoiceRepository;
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
        long next = quotationRepository.count() + 1;
        return "QUO-" + pad(next, 5);
    }

    public String nextInvoiceNo() {
        long next = invoiceRepository.count() + 1;
        int year = Year.now().getValue();
        return "INV-" + year + "-" + pad(next, 5);
    }

    private String pad(long number, int width) {
        return String.format("%0" + width + "d", number);
    }
}
