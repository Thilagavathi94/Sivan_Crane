package com.sivan.cranemanagement.model;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "quotations")
@Data
public class Quotation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String quotationNo; // QUO-00001

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

    private String billingBasis = "Daily"; // Daily / Monthly

    @Column(length = 1000)
    private String notes;

    private String status = "Pending"; // Pending / Accepted / Rejected / Converted
}
