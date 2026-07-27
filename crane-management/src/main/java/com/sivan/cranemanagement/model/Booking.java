package com.sivan.cranemanagement.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "bookings")
@Data
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String bookingNo; // BK-00001

    @ManyToOne
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    private LocalDate bookingDate;

    private String location;

    private String workType; // Machine Loading, Shifting, Erection, etc.

    @Column(length = 1000)
    private String description;

    @ManyToOne
    @JoinColumn(name = "preferred_crane_id")
    private Crane preferredCrane;

    @ManyToOne
    @JoinColumn(name = "driver_id")
    private Driver driver;

    private String status = "Pending"; // Pending / In Progress / Completed / Cancelled

    // Set to true once a Trip Sheet has been generated from this booking,
    // so the UI can hide the "Convert to Trip Sheet" action.
    private boolean convertedToTripSheet = false;

    private LocalDateTime createdAt = LocalDateTime.now();
}
