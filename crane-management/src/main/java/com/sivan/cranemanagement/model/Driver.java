package com.sivan.cranemanagement.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "drivers")
@Data
public class Driver {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String phone;

    private String licenseNo;

    @ManyToOne
    @JoinColumn(name = "assigned_crane_id")
    private Crane assignedCrane;

    private String status = "Active"; // Active / Inactive
}
