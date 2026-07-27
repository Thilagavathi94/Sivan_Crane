package com.sivan.cranemanagement.model;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "expenses")
@Data
public class Expense {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDate expenseDate;

    private String category; // Diesel / Driver Advance / Repair / Food / Other

    @ManyToOne
    @JoinColumn(name = "crane_id")
    private Crane crane;

    @ManyToOne
    @JoinColumn(name = "driver_id")
    private Driver driver;

    private BigDecimal amount = BigDecimal.ZERO;

    @Column(length = 500)
    private String description;
}
