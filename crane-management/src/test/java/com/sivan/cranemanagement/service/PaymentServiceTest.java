package com.sivan.cranemanagement.service;
import com.sivan.cranemanagement.model.*;
import com.sivan.cranemanagement.repository.*;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class PaymentServiceTest {
    PaymentRepository payments = mock(PaymentRepository.class);
    InvoiceRepository invoices = mock(InvoiceRepository.class);
    TripSheetRepository trips = mock(TripSheetRepository.class);
    PaymentService service = new PaymentService(payments, invoices, trips);
    TripSheet trip() { TripSheet t = new TripSheet(); t.setId(1L); t.setAmount(new BigDecimal("1200")); return t; }
    @Test void paidRegularTripHasZeroBalance() {
        TripSheet t = trip(); Payment p = new Payment(); p.setId(2L); p.setTripSheet(t); p.setReceivedAmount(new BigDecimal("1200"));
        when(trips.findAllByOrderByIdDesc()).thenReturn(List.of(t)); when(payments.findAllByOrderByIdDesc()).thenReturn(List.of(p));
        assertEquals(0, service.regularBalances().get(0).balance().signum());
        assertEquals("Paid", service.regularBalances().get(0).getStatus());
    }
    @Test void editingPaymentReplacesAmountAndReopensBalance() {
        TripSheet t = trip(); Payment old = new Payment(); old.setId(2L); old.setTripSheet(t); old.setReceivedAmount(new BigDecimal("1200"));
        when(payments.findById(2L)).thenReturn(Optional.of(old)); when(payments.findAllByOrderByIdDesc()).thenReturn(List.of(old));
        when(payments.save(any())).thenAnswer(i -> i.getArgument(0)); when(trips.findAllByOrderByIdDesc()).thenReturn(List.of(t));
        Payment edit = new Payment(); edit.setId(2L); edit.setReceivedAmount(new BigDecimal("500")); edit.setPaymentType("Partially Paid");
        service.save(edit);
        assertEquals(new BigDecimal("700"), service.regularBalances().get(0).balance());
        assertEquals("Partially Paid", old.getPaymentType());
    }
    @Test void creditCannotCountUnreceivedMoney() {
        TripSheet t = trip(); when(trips.findById(1L)).thenReturn(Optional.of(t));
        Payment p = new Payment(); p.setTripSheet(t); p.setPaymentType("Credit"); p.setReceivedAmount(new BigDecimal("1000"));
        assertThrows(IllegalArgumentException.class, () -> service.save(p)); verify(payments, never()).save(any());
    }
    @Test void rejectsOverpayment() {
        TripSheet t = trip(); when(trips.findById(1L)).thenReturn(Optional.of(t)); when(payments.findAllByOrderByIdDesc()).thenReturn(List.of());
        Payment p = new Payment(); p.setTripSheet(t); p.setReceivedAmount(new BigDecimal("1201"));
        assertThrows(IllegalArgumentException.class, () -> service.save(p));
    }
    @Test void invoiceEditRecalculatesWithoutDoubleCounting() {
        Invoice i = new Invoice(); i.setId(3L); i.setTotalAmount(new BigDecimal("1770"));
        Payment old = new Payment(); old.setId(2L); old.setInvoice(i); old.setReceivedAmount(BigDecimal.ZERO);
        when(payments.findById(2L)).thenReturn(Optional.of(old)); when(payments.findAllByOrderByIdDesc()).thenReturn(List.of(old));
        when(payments.save(any())).thenAnswer(a -> a.getArgument(0));
        Payment edit = new Payment(); edit.setId(2L); edit.setReceivedAmount(new BigDecimal("1000")); edit.setPaymentType("Partially Paid");
        service.save(edit);
        assertEquals(new BigDecimal("770"), i.getBalanceAmount()); assertEquals("Partially Paid", i.getPaymentStatus());
    }
    @Test void monthlyRegularReportUsesTripDateAndCrane() {
        TripSheet august = trip(); august.setTripDate(java.time.LocalDate.of(2026,8,31));
        TripSheet september = trip(); september.setId(2L); september.setTripDate(java.time.LocalDate.of(2026,9,1));
        Crane crane = new Crane(); crane.setId(7L); september.setCrane(crane);
        when(trips.findAllByOrderByIdDesc()).thenReturn(List.of(august, september));
        when(payments.findAllByOrderByIdDesc()).thenReturn(List.of());
        var result = service.regularBalancesBetween(java.time.LocalDate.of(2026,9,1), java.time.LocalDate.of(2026,9,30), 7L);
        assertEquals(1, result.size()); assertEquals(2L, result.get(0).trip().getId());
        assertTrue(service.regularBalancesBetween(java.time.LocalDate.of(2026,9,1), java.time.LocalDate.of(2026,9,30), 9L).isEmpty());
    }
    @Test void secondReceiptCompletesRegularTrip() {
        TripSheet t = trip(); Payment first = new Payment(); first.setId(2L); first.setTripSheet(t); first.setReceivedAmount(new BigDecimal("500"));
        when(trips.findById(1L)).thenReturn(Optional.of(t)); when(payments.findAllByOrderByIdDesc()).thenReturn(List.of(first));
        when(payments.save(any())).thenAnswer(a -> a.getArgument(0));
        Payment next = new Payment(); next.setTripSheet(t); next.setReceivedAmount(new BigDecimal("700"));
        assertEquals("Paid", service.save(next).getPaymentType());
    }
}

