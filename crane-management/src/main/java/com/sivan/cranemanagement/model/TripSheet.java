package com.sivan.cranemanagement.model;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

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

    @ManyToOne
    @JoinColumn(name = "driver_id")
    private Driver driver;

    private LocalDate tripDate;

    private String location;

    private LocalTime startTime;

    private LocalTime endTime;

    private BigDecimal totalHours = BigDecimal.ZERO;

    @Column(length = 1000)
    private String workDetails;

    private boolean workLifting = false;

    private boolean workLoading = false;

    private boolean workUnloading = false;

    private boolean workOther = false;

    private BigDecimal additionalHours = BigDecimal.ZERO;

    private BigDecimal minimumOneHourCharges = BigDecimal.ZERO;

    private BigDecimal minimumTwoHourCharges = BigDecimal.ZERO;

    private BigDecimal additionalCharges = BigDecimal.ZERO;

    private String status = "Work In Progress"; // Work In Progress / Work Completed

    // Set to true once an Invoice has been generated from this trip sheet
    private boolean convertedToInvoice = false;

    private LocalDateTime createdAt = LocalDateTime.now();

    public BigDecimal getHireChargesTotal() {
        return amountOrZero(minimumOneHourCharges)
                .add(amountOrZero(minimumTwoHourCharges))
                .add(amountOrZero(additionalCharges));
    }

    private BigDecimal amountOrZero(BigDecimal amount) {
        return amount != null ? amount : BigDecimal.ZERO;
    }
}