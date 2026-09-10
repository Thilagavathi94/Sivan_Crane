package com.sivan.cranemanagement.model;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "quotations")
@Data
public class Quotation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String quotationNo; // QUO-001

    private String financialYear;

    @ManyToOne
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    private LocalDate quotationDate;

    @OneToMany(mappedBy = "quotation", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<QuotationItem> items = new ArrayList<>();

    private BigDecimal subtotal = BigDecimal.ZERO;

    private BigDecimal gstPercent = new BigDecimal("18");

    private BigDecimal gstAmount = BigDecimal.ZERO;

    private BigDecimal totalAmount = BigDecimal.ZERO;

    private String billingBasis = "Daily"; // Hourly / Daily / Monthly

    private String shift = "Day"; // Day / Night — default/fallback for rows that don't set their own

    @Column(length = 1000)
    private String notes;

    private String status = "Pending"; // Pending / Accepted / Rejected / Converted

    /**
     * Which billing basis(es) are actually in use across the line items, e.g.
     * "Hourly / Daily / Monthly" when the quotation mixes crane types instead
     * of always showing just the last-selected dropdown value.
     */
    public String getBasisSummary() {
        boolean hourly = false, daily = false, monthly = false;
        for (QuotationItem item : items) {
            boolean hasMonthLabel = item.getPeriodLabel() != null && !item.getPeriodLabel().isBlank();
            boolean hasDays = item.getPeriodDays() != null && item.getPeriodDays().compareTo(BigDecimal.ZERO) > 0;
            if (hasMonthLabel) {
                monthly = true;
            } else if (hasDays) {
                daily = true;
            } else {
                hourly = true;
            }
        }
        List<String> parts = new ArrayList<>();
        if (hourly) parts.add("Hourly");
        if (daily) parts.add("Daily");
        if (monthly) parts.add("Monthly");
        if (parts.isEmpty()) {
            return billingBasis != null ? billingBasis : "Daily";
        }
        return String.join(" / ", parts);
    }

    /**
     * Which shift(s) (Day/Night) are actually in use across the line items.
     * Rows without their own shift fall back to the quotation-level shift.
     */
    public String getShiftSummary() {
        Set<String> seen = new LinkedHashSet<>();
        for (QuotationItem item : items) {
            String value = item.getShift();
            if (value == null || value.isBlank()) {
                value = shift;
            }
            if (value == null || value.isBlank()) {
                value = "Day";
            }
            seen.add(value);
        }
        if (seen.isEmpty()) {
            return shift != null && !shift.isBlank() ? shift : "Day";
        }
        List<String> ordered = new ArrayList<>();
        if (seen.contains("Day")) ordered.add("Day");
        if (seen.contains("Night")) ordered.add("Night");
        for (String value : seen) {
            if (!ordered.contains(value)) ordered.add(value);
        }
        return String.join(" / ", ordered);
    }
}