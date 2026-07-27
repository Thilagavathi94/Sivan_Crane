package com.sivan.cranemanagement.service;

import com.sivan.cranemanagement.model.*;
import com.sivan.cranemanagement.repository.BookingRepository;
import com.sivan.cranemanagement.repository.InvoiceRepository;
import com.sivan.cranemanagement.repository.TripSheetRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

@Service
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final TripSheetRepository tripSheetRepository;
    private final BookingRepository bookingRepository;
    private final NumberGeneratorService numberGeneratorService;

    public InvoiceService(InvoiceRepository invoiceRepository, TripSheetRepository tripSheetRepository,
                           BookingRepository bookingRepository,
                           NumberGeneratorService numberGeneratorService) {
        this.invoiceRepository = invoiceRepository;
        this.tripSheetRepository = tripSheetRepository;
        this.bookingRepository = bookingRepository;
        this.numberGeneratorService = numberGeneratorService;
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

        InvoiceItem craneItem = new InvoiceItem();
        craneItem.setDescription((tripSheet.getCrane() != null ? tripSheet.getCrane().getCapacity() + " "
                + tripSheet.getCrane().getType() + " Crane" : "Crane Service"));
        craneItem.setHoursOrUnits(tripSheet.getTotalHours());
        craneItem.setRate(ratePerHour != null ? ratePerHour : BigDecimal.ZERO);
        invoice.getItems().add(craneItem);

        if (mobilizationCharge != null && mobilizationCharge.compareTo(BigDecimal.ZERO) > 0) {
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
        TripSheet firstTripSheet = tripSheets.stream()
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Trip Sheet not found for booking: " + booking.getBookingNo()));

        Invoice invoice = new Invoice();
        invoice.setBooking(booking);
        invoice.setTripSheet(firstTripSheet);
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
        if (invoice.getId() == null) {
            invoice.setInvoiceNo(numberGeneratorService.nextInvoiceNo());
        }

        BigDecimal taxable = BigDecimal.ZERO;
        for (InvoiceItem item : invoice.getItems()) {
            item.setInvoice(invoice);
            BigDecimal hours = item.getHoursOrUnits() != null ? item.getHoursOrUnits() : BigDecimal.ZERO;
            BigDecimal rate = item.getRate() != null ? item.getRate() : BigDecimal.ZERO;
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

    public void delete(Long id) {
        invoiceRepository.deleteById(id);
    }

    public long count() {
        return invoiceRepository.count();
    }
}
