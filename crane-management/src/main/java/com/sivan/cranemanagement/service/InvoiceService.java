package com.sivan.cranemanagement.service;

import com.sivan.cranemanagement.model.*;
import com.sivan.cranemanagement.repository.BookingRepository;
import com.sivan.cranemanagement.repository.InvoiceRepository;
import com.sivan.cranemanagement.repository.PaymentRepository;
import com.sivan.cranemanagement.repository.TripSheetRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

@Service
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final TripSheetRepository tripSheetRepository;
    private final BookingRepository bookingRepository;
    private final NumberGeneratorService numberGeneratorService;
    private final AppSettingsService appSettingsService;
    private final PaymentRepository paymentRepository;

    public InvoiceService(InvoiceRepository invoiceRepository, TripSheetRepository tripSheetRepository,
                           BookingRepository bookingRepository,
                           NumberGeneratorService numberGeneratorService,
                           AppSettingsService appSettingsService,
                           PaymentRepository paymentRepository) {
        this.invoiceRepository = invoiceRepository;
        this.tripSheetRepository = tripSheetRepository;
        this.bookingRepository = bookingRepository;
        this.numberGeneratorService = numberGeneratorService;
        this.appSettingsService = appSettingsService;
        this.paymentRepository = paymentRepository;
    }

    public List<Invoice> findAll() {
        return invoiceRepository.findAllByOrderByIdDesc();
    }

    public Invoice findById(Long id) {
        return invoiceRepository.findById(id).orElseThrow(() ->
                new RuntimeException("Invoice not found: " + id));
    }

    public List<Invoice> findPending() {
        return invoiceRepository.findByPaymentStatusNot("Paid");
    }

    public List<Invoice> findBetween(LocalDate start, LocalDate end) {
        return invoiceRepository.findByInvoiceDateBetween(start, end);
    }

    public List<Invoice> findByFinancialYear(String financialYear) {
        return invoiceRepository.findByFinancialYearOrderByInvoiceDateAsc(financialYear);
    }

    public List<String> findFinancialYears() {
        return invoiceRepository.findDistinctFinancialYears();
    }

    /**
     * Builds a new, unsaved Invoice pre-filled from a Trip Sheet: customer, work
     * description and default crane-rate line item all carry forward automatically.
     * This backs the "Generate Invoice from Trip Sheet" feature.
     */
    public Invoice buildFromTripSheet(Long tripSheetId, BigDecimal ratePerHour, BigDecimal mobilizationCharge) {
        TripSheet tripSheet = tripSheetRepository.findById(tripSheetId).orElseThrow(() ->
                new RuntimeException("Trip Sheet not found: " + tripSheetId));

        Invoice invoice = new Invoice();
        invoice.setTripSheet(tripSheet);
        invoice.setBooking(tripSheet.getBooking());
        invoice.setCustomer(tripSheet.getCustomer());
        invoice.setInvoiceDate(LocalDate.now());

        boolean hasTripAmount = tripSheet.getAmount() != null && tripSheet.getAmount().compareTo(BigDecimal.ZERO) > 0;
        InvoiceItem craneItem = new InvoiceItem();
        craneItem.setDescription((tripSheet.getCrane() != null ? tripSheet.getCrane().getCapacity() + " "
                + tripSheet.getCrane().getType() + " Crane" : "Crane Service"));
        craneItem.setHoursOrUnits(hasTripAmount ? BigDecimal.ONE : tripSheet.getTotalHours());
        craneItem.setRate(hasTripAmount ? tripSheet.getAmount() : (ratePerHour != null ? ratePerHour : BigDecimal.ZERO));
        invoice.getItems().add(craneItem);

        if (!hasTripAmount && mobilizationCharge != null && mobilizationCharge.compareTo(BigDecimal.ZERO) > 0) {
            InvoiceItem mobItem = new InvoiceItem();
            mobItem.setDescription("Mobilization Charges");
            mobItem.setHoursOrUnits(BigDecimal.ONE);
            mobItem.setRate(mobilizationCharge);
            invoice.getItems().add(mobItem);
        }

        return invoice;
    }

    public Invoice buildFromBooking(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId).orElseThrow(() ->
                new RuntimeException("Booking not found: " + bookingId));
        List<TripSheet> tripSheets = tripSheetRepository.findByBookingIdOrderByIdDesc(bookingId);

        Invoice invoice = new Invoice();
        invoice.setBooking(booking);
        tripSheets.stream().findFirst().ifPresent(invoice::setTripSheet);
        invoice.setCustomer(booking.getCustomer());
        invoice.setInvoiceDate(LocalDate.now());

        for (TripSheet tripSheet : tripSheets) {
            BigDecimal tripAmount = tripSheet.getHireChargesTotal();
            InvoiceItem item = new InvoiceItem();
            item.setDescription("Trip Sheet " + tripSheet.getTripSheetNo());
            item.setHoursOrUnits(BigDecimal.ONE);
            item.setRate(tripAmount);
            invoice.getItems().add(item);
        }
        invoice.setReceivedAmount(BigDecimal.ZERO);
        return invoice;
    }

    public Invoice save(Invoice invoice) {
        if (invoice.getId() != null) {
            Invoice existing = findById(invoice.getId());
            if ("Final".equalsIgnoreCase(existing.getInvoiceStatus())) {
                invoice.setInvoiceNo(existing.getInvoiceNo());
                invoice.setFinancialYear(existing.getFinancialYear());
                invoice.setInvoiceStatus(existing.getInvoiceStatus());
            }
        }

        if (invoice.getFinancialYear() == null || invoice.getFinancialYear().isBlank()) {
            invoice.setFinancialYear(appSettingsService.getSettings().getCurrentFinancialYear());
        }
        if (invoice.getInvoiceStatus() == null || invoice.getInvoiceStatus().isBlank()) {
            invoice.setInvoiceStatus("Draft");
        }
        if (invoice.getInvoiceNo() == null || invoice.getInvoiceNo().isBlank()) {
            invoice.setInvoiceNo(numberGeneratorService.nextInvoiceNo());
        }

        validateAndSynchronizeManualRunningTime(invoice);
        invoice.setManualRunningHours(nonNull(invoice.getManualRunningHours()));
        invoice.setManualAmount(nonNull(invoice.getManualAmount()));
        if (invoice.getTripSheet() == null
                && invoice.getManualAmount().compareTo(BigDecimal.ZERO) > 0) {
            InvoiceItem manualItem = new InvoiceItem();
            manualItem.setDescription("Manual Trip Sheet Amount");
            manualItem.setHoursOrUnits(BigDecimal.ONE);
            manualItem.setRate(invoice.getManualAmount());
            invoice.getItems().clear();
            invoice.getItems().add(manualItem);
        }

        BigDecimal taxable = BigDecimal.ZERO;
        for (InvoiceItem item : invoice.getItems()) {
            item.setInvoice(invoice);
            BigDecimal hours = nonNull(item.getHoursOrUnits());
            BigDecimal rate = nonNull(item.getRate());
            item.setHoursOrUnits(hours);
            item.setRate(rate);
            BigDecimal amount = rate.multiply(hours).setScale(2, RoundingMode.HALF_UP);
            item.setAmount(amount);
            taxable = taxable.add(amount);
        }
        invoice.setTaxableAmount(taxable);

        BigDecimal cgstPercent = invoice.getCgstPercent() != null ? invoice.getCgstPercent() : new BigDecimal("9");
        BigDecimal sgstPercent = invoice.getSgstPercent() != null ? invoice.getSgstPercent() : new BigDecimal("9");
        invoice.setCgstPercent(cgstPercent);
        invoice.setSgstPercent(sgstPercent);

        BigDecimal cgst = taxable.multiply(cgstPercent)
                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        BigDecimal sgst = taxable.multiply(sgstPercent)
                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        invoice.setCgstAmount(cgst);
        invoice.setSgstAmount(sgst);
        invoice.setTotalAmount(taxable.add(cgst).add(sgst));

        BigDecimal received = invoice.getReceivedAmount() != null ? invoice.getReceivedAmount() : BigDecimal.ZERO;
        invoice.setReceivedAmount(received);
        invoice.setBalanceAmount(invoice.getTotalAmount().subtract(received));
        if (received.compareTo(BigDecimal.ZERO) <= 0) {
            invoice.setPaymentStatus("Pending");
        } else if (received.compareTo(invoice.getTotalAmount()) >= 0) {
            invoice.setPaymentStatus("Paid");
        } else {
            invoice.setPaymentStatus("Partially Paid");
        }

        Invoice saved = invoiceRepository.save(invoice);

        // Mark the source trip sheets as converted so they do not get double-invoiced.
        if (saved.getBooking() != null) {
            for (TripSheet tripSheet : tripSheetRepository.findByBookingIdOrderByIdDesc(saved.getBooking().getId())) {
                tripSheet.setConvertedToInvoice(true);
                tripSheetRepository.save(tripSheet);
            }
        } else if (saved.getTripSheet() != null) {
            TripSheet ts = saved.getTripSheet();
            ts.setConvertedToInvoice(true);
            tripSheetRepository.save(ts);
        }
        return saved;
    }

    public Invoice finalizeInvoice(Long id) {
        Invoice invoice = findById(id);
        if (invoice.getInvoiceNo() == null || invoice.getInvoiceNo().isBlank()) {
            invoice.setInvoiceNo(numberGeneratorService.nextInvoiceNo());
        }
        if (invoice.getFinancialYear() == null || invoice.getFinancialYear().isBlank()) {
            invoice.setFinancialYear(appSettingsService.getSettings().getCurrentFinancialYear());
        }
        invoice.setInvoiceStatus("Final");
        return invoiceRepository.save(invoice);
    }

    private BigDecimal nonNull(BigDecimal value) {
        return Objects.requireNonNullElse(value, BigDecimal.ZERO);
    }

    private void validateAndSynchronizeManualRunningTime(Invoice invoice) {
        Integer hours = invoice.getManualRunningHoursWhole();
        Integer minutes = invoice.getManualRunningMinutes();
        if (hours == null || hours < 0 || minutes == null || minutes < 0 || minutes > 59) {
            throw new IllegalArgumentException("Manual running time must use whole hours and minutes from 0 to 59.");
        }
        invoice.synchronizeManualRunningTime();
    }

    public void delete(Long id) {
        // Payments recorded against this invoice would otherwise block deletion
        // with a foreign key error - unlink them (the payment record stays, it
        // just no longer references a deleted invoice) before removing it.
        for (Payment payment : paymentRepository.findByInvoiceId(id)) {
            payment.setInvoice(null);
            paymentRepository.save(payment);
        }
        invoiceRepository.deleteById(id);
    }

    public long count() {
        return invoiceRepository.count();
    }
}
