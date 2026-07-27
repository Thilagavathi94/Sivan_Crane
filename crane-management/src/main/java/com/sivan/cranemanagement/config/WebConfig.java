package com.sivan.cranemanagement.config;

import com.sivan.cranemanagement.model.*;
import com.sivan.cranemanagement.repository.*;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Registers explicit String -> Entity converters for every JPA entity that is
 * bound from an HTML <select> dropdown (th:field="*{customer}" etc.).
 *
 * This is required so that:
 *  - a chosen option (the entity's numeric id as a String) resolves to the
 *    actual managed entity, and
 *  - a blank "-- None --" option (empty string) safely resolves to null
 *    instead of throwing a conversion error, for optional relationships.
 *
 * IMPORTANT: these MUST be concrete classes (not lambdas). Spring inspects a
 * Converter's generic type parameters via reflection to know which pair of
 * types (S -> T) it handles. Lambdas erase that generic signature at
 * runtime, which causes:
 *   "Unable to determine source type <S> and target type <T> for your
 *    Converter ...; does the class parameterize those types?"
 * Named/anonymous classes retain the generic signature, so they work.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final CustomerRepository customerRepository;
    private final CraneRepository craneRepository;
    private final DriverRepository driverRepository;
    private final BookingRepository bookingRepository;
    private final TripSheetRepository tripSheetRepository;
    private final InvoiceRepository invoiceRepository;

    public WebConfig(CustomerRepository customerRepository, CraneRepository craneRepository,
                      DriverRepository driverRepository, BookingRepository bookingRepository,
                      TripSheetRepository tripSheetRepository, InvoiceRepository invoiceRepository) {
        this.customerRepository = customerRepository;
        this.craneRepository = craneRepository;
        this.driverRepository = driverRepository;
        this.bookingRepository = bookingRepository;
        this.tripSheetRepository = tripSheetRepository;
        this.invoiceRepository = invoiceRepository;
    }

    private static Long parseId(String source) {
        if (source == null || source.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(source.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addConverter(new StringToCustomerConverter());
        registry.addConverter(new StringToCraneConverter());
        registry.addConverter(new StringToDriverConverter());
        registry.addConverter(new StringToBookingConverter());
        registry.addConverter(new StringToTripSheetConverter());
        registry.addConverter(new StringToInvoiceConverter());
    }

    private class StringToCustomerConverter implements Converter<String, Customer> {
        @Override
        public Customer convert(String source) {
            Long id = parseId(source);
            return id == null ? null : customerRepository.findById(id).orElse(null);
        }
    }

    private class StringToCraneConverter implements Converter<String, Crane> {
        @Override
        public Crane convert(String source) {
            Long id = parseId(source);
            return id == null ? null : craneRepository.findById(id).orElse(null);
        }
    }

    private class StringToDriverConverter implements Converter<String, Driver> {
        @Override
        public Driver convert(String source) {
            Long id = parseId(source);
            return id == null ? null : driverRepository.findById(id).orElse(null);
        }
    }

    private class StringToBookingConverter implements Converter<String, Booking> {
        @Override
        public Booking convert(String source) {
            Long id = parseId(source);
            return id == null ? null : bookingRepository.findById(id).orElse(null);
        }
    }

    private class StringToTripSheetConverter implements Converter<String, TripSheet> {
        @Override
        public TripSheet convert(String source) {
            Long id = parseId(source);
            return id == null ? null : tripSheetRepository.findById(id).orElse(null);
        }
    }

    private class StringToInvoiceConverter implements Converter<String, Invoice> {
        @Override
        public Invoice convert(String source) {
            Long id = parseId(source);
            return id == null ? null : invoiceRepository.findById(id).orElse(null);
        }
    }
}
