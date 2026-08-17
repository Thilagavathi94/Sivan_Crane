package com.sivan.cranemanagement.model;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "trip_sheets")
@Data
public class TripSheet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String tripSheetNo; // TS-00001

    // Optional link back to the booking it was generated from (may be null for walk-in trips)
    @ManyToOne
    @JoinColumn(name = "booking_id")
    private Booking booking;

    @ManyToOne
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @ManyToOne
    @JoinColumn(name = "crane_id", nullable = false)
    private Crane crane;

    private LocalDate tripDate;

    private BigDecimal totalHours = BigDecimal.ZERO;

    // Total amount charged for this trip sheet
    private BigDecimal amount = BigDecimal.ZERO;

    private String billingType = "Regular"; // Regular / GST

    // Set to true once an Invoice has been generated from this trip sheet
    private boolean convertedToInvoice = false;

    private LocalDateTime createdAt = LocalDateTime.now();

    public BigDecimal getHireChargesTotal() {
        return amountOrZero(amount);
    }

    private BigDecimal amountOrZero(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }
}
