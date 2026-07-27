package com.sivan.cranemanagement.model;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;

@Entity
@Table(name = "invoice_items")
@Data
public class InvoiceItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "invoice_id", nullable = false)
    private Invoice invoice;

    private String description;

    private BigDecimal hoursOrUnits = BigDecimal.ZERO;

    private BigDecimal rate = BigDecimal.ZERO;

    private BigDecimal amount = BigDecimal.ZERO;
}
