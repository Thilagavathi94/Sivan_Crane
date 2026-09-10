package com.sivan.cranemanagement.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "app_settings")
@Data
public class AppSettings {

    @Id
    private Long id = 1L;

    private String currentFinancialYear;

    private int nextInvoiceNumber = 1;

    private int nextQuotationNumber = 1;
}
