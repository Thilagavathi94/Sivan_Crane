package com.sivan.cranemanagement.service;

import com.sivan.cranemanagement.model.Booking;
import com.sivan.cranemanagement.model.Invoice;
import com.sivan.cranemanagement.model.Payment;
import com.sivan.cranemanagement.model.TripSheet;
import com.sivan.cranemanagement.repository.BookingRepository;
import com.sivan.cranemanagement.repository.InvoiceRepository;
import com.sivan.cranemanagement.repository.PaymentRepository;
import com.sivan.cranemanagement.repository.TripSheetRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
public class TripSheetService {

    private final TripSheetRepository tripSheetRepository;
    private final BookingRepository bookingRepository;
    private final NumberGeneratorService numberGeneratorService;
    private final InvoiceRepository invoiceRepository;
    private final PaymentRepository paymentRepository;

    public TripSheetService(TripSheetRepository tripSheetRepository, BookingRepository bookingRepository,
                             NumberGeneratorService numberGeneratorService,
                             InvoiceRepository invoiceRepository,
                             PaymentRepository paymentRepository) {
        this.tripSheetRepository = tripSheetRepository;
        this.bookingRepository = bookingRepository;
        this.numberGeneratorService = numberGeneratorService;
        this.invoiceRepository = invoiceRepository;
        this.paymentRepository = paymentRepository;
    }

    public List<TripSheet> findAll() {
        return tripSheetRepository.findAllByOrderByIdDesc();
    }

    public List<TripSheet> findByCraneId(Long craneId) {
        return tripSheetRepository.findByCraneIdOrderByIdDesc(craneId);
    }

    public List<TripSheet> findByBookingId(Long bookingId) {
        return tripSheetRepository.findByBookingIdOrderByIdDesc(bookingId);
    }

    public List<TripSheet> findRegularTripSheets() {
        return tripSheetRepository.findByBillingTypeOrderByIdDesc("Regular");
    }

    public long countByCraneId(Long craneId) {
        return tripSheetRepository.countByCraneId(craneId);
    }

    public TripSheet findById(Long id) {
        return tripSheetRepository.findById(id).orElseThrow(() ->
                new RuntimeException("Trip Sheet not found: " + id));
    }

    /**
     * Builds a new, unsaved TripSheet pre-filled from an existing Booking so the
     * office staff never has to re-type the customer, crane, driver or date.
     * This backs the "Convert Booking to Trip Sheet" feature.
     */
    public TripSheet buildFromBooking(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId).orElseThrow(() ->
                new RuntimeException("Booking not found: " + bookingId));

        TripSheet tripSheet = new TripSheet();
        tripSheet.setBooking(booking);
        tripSheet.setCustomer(booking.getCustomer());
        tripSheet.setCrane(booking.getPreferredCrane());
        tripSheet.setTripDate(booking.getBookingDate() != null ? booking.getBookingDate() : LocalDate.now());
        return tripSheet;
    }

    public TripSheet save(TripSheet tripSheet) {
        validateAndSynchronizeRunningTime(tripSheet);
        // Trip Sheet No is entered by the user. Only auto-generate as a fallback
        // if it was left blank, and only for brand-new trip sheets.
        if (tripSheet.getId() == null &&
                (tripSheet.getTripSheetNo() == null || tripSheet.getTripSheetNo().trim().isEmpty())) {
            tripSheet.setTripSheetNo(numberGeneratorService.nextTripSheetNo());
        }
        if (tripSheet.getTotalHours() == null) {
            tripSheet.setTotalHours(java.math.BigDecimal.ZERO);
        }
        if (tripSheet.getAmount() == null) {
            tripSheet.setAmount(java.math.BigDecimal.ZERO);
        }
        if (tripSheet.getBillingType() == null || tripSheet.getBillingType().trim().isEmpty()) {
            tripSheet.setBillingType("Regular");
        }
        TripSheet saved = tripSheetRepository.save(tripSheet);

        // Mark the originating booking as converted / in progress
        if (saved.getBooking() != null) {
            Booking booking = saved.getBooking();
            booking.setConvertedToTripSheet(true);
            booking.setStatus("In Progress");
            bookingRepository.save(booking);
        }
        return saved;
    }

    private void validateAndSynchronizeRunningTime(TripSheet tripSheet) {
        Integer hours = tripSheet.getRunningHours();
        Integer minutes = tripSheet.getRunningMinutes();
        if (hours == null || hours < 0 || minutes == null || minutes < 0 || minutes > 59) {
            throw new IllegalArgumentException("Running time must use whole hours and minutes from 0 to 59.");
        }
        if (hours == 0 && minutes == 0) {
            throw new IllegalArgumentException("Running time must be greater than zero.");
        }
        if (tripSheet.getAmount() == null || tripSheet.getAmount().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Amount cannot be negative.");
        }
        tripSheet.synchronizeRunningTime();
    }

    public void delete(Long id) {
        // Invoices/payments referencing this trip sheet would otherwise block
        // deletion with a foreign key error - unlink them (records stay, just
        // drop the trip sheet reference) before removing the trip sheet itself.
        for (Invoice invoice : invoiceRepository.findByTripSheetIdOrderByIdDesc(id)) {
            invoice.setTripSheet(null);
            invoiceRepository.save(invoice);
        }
        for (Payment payment : paymentRepository.findByTripSheetId(id)) {
            payment.setTripSheet(null);
            paymentRepository.save(payment);
        }
        tripSheetRepository.deleteById(id);
    }

    public long count() {
        return tripSheetRepository.count();
    }
}