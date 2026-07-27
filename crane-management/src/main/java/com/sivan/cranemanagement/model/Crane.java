package com.sivan.cranemanagement.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "cranes")
@Data
public class Crane {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String craneNo; // KCN-01

    private String registrationNo; // TN38CT7504

    private String type; // Hydra, Crawler, Mobile, Tower

    private String capacity; // 12 Ton

    private String status = "Available"; // Available / Working / Service
}
