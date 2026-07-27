package com.sivan.cranemanagement.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "customers")
@Data
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String phone;

    private String gstNumber;

    @Column(length = 500)
    private String address;

    private String status = "Active"; // Active / Inactive

    private LocalDateTime createdAt = LocalDateTime.now();
}
