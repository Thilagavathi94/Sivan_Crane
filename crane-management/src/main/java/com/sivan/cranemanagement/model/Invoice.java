package com.sivan.cranemanagement.model;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "invoices")
@Data
public class Invoice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String invoiceNo; // INV-001

    private String financialYear;

    private String invoiceStatus = "Draft"; // Draft / Final

    @ManyToOne
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    // Optional link back to the trip sheet it was generated from
    @ManyToOne
    @JoinColumn(name = "trip_sheet_id")
    private TripSheet tripSheet;

    @ManyToOne
    @JoinColumn(name = "booking_id")
    private Booking booking;

    private LocalDate invoiceDate;

    // Manual trip details used when an invoice is created without a saved Trip Sheet.
    private LocalDate manualTripDate;

    private String manualCraneNo;

    private String manualTripSheetNo;

    private BigDecimal manualRunningHours = BigDecimal.ZERO;

    private Integer manualRunningHoursWhole;

    private Integer manualRunningMinutes;

    private BigDecimal manualAmount = BigDecimal.ZERO;

    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<InvoiceItem> items = new ArrayList<>();

    private BigDecimal taxableAmount = BigDecimal.ZERO;

    private BigDecimal cgstPercent = new BigDecimal("9");
    private BigDecimal sgstPercent = new BigDecimal("9");

    private BigDecimal cgstAmount = BigDecimal.ZERO;
    private BigDecimal sgstAmount = BigDecimal.ZERO;

    private BigDecimal totalAmount = BigDecimal.ZERO;

    // Paid / Partially Paid / Pending
    private String paymentStatus = "Pending";

    private BigDecimal receivedAmount = BigDecimal.ZERO;

    private BigDecimal balanceAmount = BigDecimal.ZERO;

    public Integer getManualRunningHoursWhole() {
        return manualRunningHoursWhole != null ? manualRunningHoursWhole : legacyManualDuration()[0];
    }

    public Integer getManualRunningMinutes() {
        return manualRunningMinutes != null ? manualRunningMinutes : legacyManualDuration()[1];
    }

    public void setManualRunningHoursWhole(Integer manualRunningHoursWhole) {
        this.manualRunningHoursWhole = manualRunningHoursWhole;
    }

    public void setManualRunningMinutes(Integer manualRunningMinutes) {
        this.manualRunningMinutes = manualRunningMinutes;
    }

    public void synchronizeManualRunningTime() {
        int hours = manualRunningHoursWhole == null ? 0 : manualRunningHoursWhole;
        int minutes = manualRunningMinutes == null ? 0 : manualRunningMinutes;
        this.manualRunningHoursWhole = hours;
        this.manualRunningMinutes = minutes;
        this.manualRunningHours = BigDecimal.valueOf(hours)
                .add(BigDecimal.valueOf(minutes).divide(BigDecimal.valueOf(60), 2, java.math.RoundingMode.HALF_UP));
    }

    private int[] legacyManualDuration() {
        BigDecimal value = manualRunningHours == null ? BigDecimal.ZERO : manualRunningHours;
        int hours = value.intValue();
        int minutes = value.subtract(BigDecimal.valueOf(hours)).multiply(BigDecimal.valueOf(60))
                .setScale(0, java.math.RoundingMode.HALF_UP).intValue();
        return minutes == 60 ? new int[]{hours + 1, 0} : new int[]{hours, minutes};
    }
}
