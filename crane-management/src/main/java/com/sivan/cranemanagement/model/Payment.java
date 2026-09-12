package com.sivan.cranemanagement.model;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "payments")
@Data
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "invoice_id")
    private Invoice invoice;

    @ManyToOne
    @JoinColumn(name = "trip_sheet_id")
    private TripSheet tripSheet;

    public String getReference() {
        return invoice != null ? invoice.getInvoiceNo() : tripSheet != null ? tripSheet.getTripSheetNo() : "Unknown";
    }

    private LocalDate paymentDate;

    private BigDecimal receivedAmount = BigDecimal.ZERO;

    private String paymentMode = "Cash"; // Cash / Bank / UPI / Cheque

    private String paymentType = "Paid"; // Paid / Credit (Pending)

    @Column(length = 500)
    private String notes;
}
