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

    /** Stored separately so 2 hours 15 minutes is never interpreted as 2.15 hours. */
    private Integer runningHours;

    private Integer runningMinutes;

    // Kept for existing records and rate calculations. New values are derived
    // from runningHours + runningMinutes / 60.
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

    public Integer getRunningHours() {
        return runningHours != null ? runningHours : legacyDuration()[0];
    }

    public Integer getRunningMinutes() {
        return runningMinutes != null ? runningMinutes : legacyDuration()[1];
    }

    public void setRunningHours(Integer runningHours) {
        this.runningHours = runningHours;
    }

    public void setRunningMinutes(Integer runningMinutes) {
        this.runningMinutes = runningMinutes;
    }

    public void synchronizeRunningTime() {
        int hours = runningHours == null ? 0 : runningHours;
        int minutes = runningMinutes == null ? 0 : runningMinutes;
        this.runningHours = hours;
        this.runningMinutes = minutes;
        this.totalHours = BigDecimal.valueOf(hours)
                .add(BigDecimal.valueOf(minutes).divide(BigDecimal.valueOf(60), 2, java.math.RoundingMode.HALF_UP));
    }

    private int[] legacyDuration() {
        BigDecimal value = totalHours == null ? BigDecimal.ZERO : totalHours;
        int hours = value.intValue();
        int minutes = value.subtract(BigDecimal.valueOf(hours)).multiply(BigDecimal.valueOf(60))
                .setScale(0, java.math.RoundingMode.HALF_UP).intValue();
        return minutes == 60 ? new int[]{hours + 1, 0} : new int[]{hours, minutes};
    }
}
