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

    private String description; // e.g. "17 Ton Hydra Crane"

    private BigDecimal hoursOrUnits = BigDecimal.ZERO;

    private BigDecimal ratePerHour = BigDecimal.ZERO;

    private BigDecimal additionalHours = BigDecimal.ZERO;

    private BigDecimal additionalRate = BigDecimal.ZERO;

    private BigDecimal additionalAmount = BigDecimal.ZERO;

    private BigDecimal amount = BigDecimal.ZERO;
}
