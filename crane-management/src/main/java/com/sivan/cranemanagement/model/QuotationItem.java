package com.sivan.cranemanagement.model;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;

@Entity
@Table(name = "quotation_items")
@Data
public class QuotationItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "quotation_id", nullable = false)
    private Quotation quotation;

    private String description; // e.g. "17 Ton Hydra Crane" (shown as Crane Capacity)

    // Hourly basis: minimum/base hours. Daily basis: hrs per day. Monthly basis: working hours per day.
    private BigDecimal hoursOrUnits = BigDecimal.ZERO;

    private BigDecimal ratePerHour = BigDecimal.ZERO;

    // Additional hour count (Hourly basis)
    private BigDecimal additionalHours = BigDecimal.ZERO;

    private BigDecimal additionalRate = BigDecimal.ZERO;

    // Additional hour amount (Hourly / Daily basis)
    private BigDecimal additionalAmount = BigDecimal.ZERO;

    // Daily basis = e.g. "1 Day". Monthly basis = number of months, e.g. "1 Month".
    private String periodLabel;

    // Monthly basis only: number of working days in the month, e.g. 26
    private BigDecimal periodDays = BigDecimal.ZERO;

    private BigDecimal amount = BigDecimal.ZERO;
}