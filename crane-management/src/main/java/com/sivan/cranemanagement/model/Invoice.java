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

    @Column(unique = true, nullable = false)
    private String invoiceNo; // INV-2026-00001

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
}
