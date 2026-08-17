package com.sivan.cranemanagement.service;

import com.sivan.cranemanagement.model.Booking;
import com.sivan.cranemanagement.model.TripSheet;
import com.sivan.cranemanagement.repository.BookingRepository;
import com.sivan.cranemanagement.repository.TripSheetRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class TripSheetService {

    private final TripSheetRepository tripSheetRepository;
    private final BookingRepository bookingRepository;
    private final NumberGeneratorService numberGeneratorService;

    public TripSheetService(TripSheetRepository tripSheetRepository, BookingRepository bookingRepository,
                             NumberGeneratorService numberGeneratorService) {
        this.tripSheetRepository = tripSheetRepository;
        this.bookingRepository = bookingRepository;
        this.numberGeneratorService = numberGeneratorService;
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

    public void delete(Long id) {
        tripSheetRepository.deleteById(id);
    }

    public long count() {
        return tripSheetRepository.count();
    }
}
