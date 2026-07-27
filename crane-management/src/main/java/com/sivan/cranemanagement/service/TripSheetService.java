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
        tripSheet.setDriver(booking.getDriver());
        tripSheet.setTripDate(booking.getBookingDate() != null ? booking.getBookingDate() : LocalDate.now());
        tripSheet.setLocation(booking.getLocation());
        String workType = booking.getWorkType() != null ? booking.getWorkType() : "";
        String description = booking.getDescription() != null ? booking.getDescription() : "";
        tripSheet.setWorkDetails((workType + " " + description).trim());
        return tripSheet;
    }

    public TripSheet save(TripSheet tripSheet) {
        if (tripSheet.getId() == null) {
            tripSheet.setTripSheetNo(numberGeneratorService.nextTripSheetNo());
        }
        if (tripSheet.getTotalHours() == null) {
            tripSheet.setTotalHours(java.math.BigDecimal.ZERO);
        }
        if (tripSheet.getAdditionalHours() == null) {
            tripSheet.setAdditionalHours(java.math.BigDecimal.ZERO);
        }
        if (tripSheet.getMinimumOneHourCharges() == null) {
            tripSheet.setMinimumOneHourCharges(java.math.BigDecimal.ZERO);
        }
        if (tripSheet.getMinimumTwoHourCharges() == null) {
            tripSheet.setMinimumTwoHourCharges(java.math.BigDecimal.ZERO);
        }
        if (tripSheet.getAdditionalCharges() == null) {
            tripSheet.setAdditionalCharges(java.math.BigDecimal.ZERO);
        }
        TripSheet saved = tripSheetRepository.save(tripSheet);

        // Mark the originating booking as converted + completed if the trip is done
        if (saved.getBooking() != null) {
            Booking booking = saved.getBooking();
            booking.setConvertedToTripSheet(true);
            if ("Work Completed".equalsIgnoreCase(saved.getStatus())) {
                booking.setStatus("Completed");
            } else {
                booking.setStatus("In Progress");
            }
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
