package com.sivan.cranemanagement.service;

import com.sivan.cranemanagement.model.Quotation;
import com.sivan.cranemanagement.model.QuotationItem;
import com.sivan.cranemanagement.repository.QuotationRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class QuotationService {

    private final QuotationRepository quotationRepository;
    private final NumberGeneratorService numberGeneratorService;

    public QuotationService(QuotationRepository quotationRepository, NumberGeneratorService numberGeneratorService) {
        this.quotationRepository = quotationRepository;
        this.numberGeneratorService = numberGeneratorService;
    }

    public List<Quotation> findAll() {
        return quotationRepository.findAllByOrderByIdDesc();
    }

    public Quotation findById(Long id) {
        return quotationRepository.findById(id).orElseThrow(() ->
                new RuntimeException("Quotation not found: " + id));
    }

    public Quotation save(Quotation quotation) {
        if (quotation.getId() == null) {
            quotation.setQuotationNo(numberGeneratorService.nextQuotationNo());
        }

        BigDecimal subtotal = BigDecimal.ZERO;
        for (QuotationItem item : quotation.getItems()) {
            item.setQuotation(quotation);
            BigDecimal hours = item.getHoursOrUnits() != null ? item.getHoursOrUnits() : BigDecimal.ZERO;
            BigDecimal rate = item.getRatePerHour() != null ? item.getRatePerHour() : BigDecimal.ZERO;
            item.setHoursOrUnits(hours);
            item.setRatePerHour(rate);
            BigDecimal amount = rate.multiply(hours).setScale(2, RoundingMode.HALF_UP);
            item.setAmount(amount);

            BigDecimal additionalHours = item.getAdditionalHours() != null ? item.getAdditionalHours() : BigDecimal.ZERO;
            // If no additional rate was given, fall back to the crane's normal hourly rate
            BigDecimal additionalRate = (item.getAdditionalRate() != null && item.getAdditionalRate().compareTo(BigDecimal.ZERO) > 0)
                    ? item.getAdditionalRate() : rate;
            item.setAdditionalHours(additionalHours);
            item.setAdditionalRate(additionalRate);
            BigDecimal additionalAmount = additionalRate.multiply(additionalHours).setScale(2, RoundingMode.HALF_UP);
            item.setAdditionalAmount(additionalAmount);

            subtotal = subtotal.add(amount).add(additionalAmount);
        }
        quotation.setSubtotal(subtotal);

        BigDecimal gstPercent = quotation.getGstPercent() != null ? quotation.getGstPercent() : new BigDecimal("18");
        quotation.setGstPercent(gstPercent);
        BigDecimal gstAmount = subtotal.multiply(gstPercent)
                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        quotation.setGstAmount(gstAmount);
        quotation.setTotalAmount(subtotal.add(gstAmount));

        return quotationRepository.save(quotation);
    }

    public void delete(Long id) {
        quotationRepository.deleteById(id);
    }
}