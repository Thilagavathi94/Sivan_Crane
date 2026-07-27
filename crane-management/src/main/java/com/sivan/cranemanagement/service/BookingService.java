package com.sivan.cranemanagement.service;

import com.sivan.cranemanagement.model.Booking;
import com.sivan.cranemanagement.model.Crane;
import com.sivan.cranemanagement.repository.BookingRepository;
import com.sivan.cranemanagement.repository.CraneRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class BookingService {

    private final BookingRepository bookingRepository;
    private final CraneRepository craneRepository;
    private final NumberGeneratorService numberGeneratorService;

    public BookingService(BookingRepository bookingRepository, CraneRepository craneRepository,
                           NumberGeneratorService numberGeneratorService) {
        this.bookingRepository = bookingRepository;
        this.craneRepository = craneRepository;
        this.numberGeneratorService = numberGeneratorService;
    }

    public List<Booking> findAll() {
        return bookingRepository.findAllByOrderByIdDesc();
    }

    public List<Booking> findRecent5() {
        return bookingRepository.findTop5ByOrderByIdDesc();
    }

    public Booking findById(Long id) {
        return bookingRepository.findById(id).orElseThrow(() ->
                new RuntimeException("Booking not found: " + id));
    }

    public Booking save(Booking booking) {
        if (booking.getId() == null) {
            booking.setBookingNo(numberGeneratorService.nextBookingNo());
        }
        Booking saved = bookingRepository.save(booking);

        // If a crane was preferred/assigned, mark it as Working so the dashboard/crane list stays accurate
        if (saved.getPreferredCrane() != null && "In Progress".equalsIgnoreCase(saved.getStatus())) {
            Crane crane = saved.getPreferredCrane();
            crane.setStatus("Working");
            craneRepository.save(crane);
        }
        return saved;
    }

    public void delete(Long id) {
        bookingRepository.deleteById(id);
    }

    public long count() {
        return bookingRepository.count();
    }

    public long countToday() {
        return bookingRepository.countByBookingDate(LocalDate.now());
    }

    public long countByStatus(String status) {
        return bookingRepository.countByStatus(status);
    }
}
