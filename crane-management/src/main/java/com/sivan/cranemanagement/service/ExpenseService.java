package com.sivan.cranemanagement.service;

import com.sivan.cranemanagement.model.Expense;
import com.sivan.cranemanagement.repository.ExpenseRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
public class ExpenseService {

    private final ExpenseRepository expenseRepository;

    public ExpenseService(ExpenseRepository expenseRepository) {
        this.expenseRepository = expenseRepository;
    }

    public List<Expense> findAll() {
        return expenseRepository.findAllByOrderByIdDesc();
    }

    public Expense findById(Long id) {
        return expenseRepository.findById(id).orElseThrow(() ->
                new RuntimeException("Expense not found: " + id));
    }

    public Expense save(Expense expense) {
        if (expense.getAmount() == null) {
            expense.setAmount(BigDecimal.ZERO);
        }
        return expenseRepository.save(expense);
    }

    public void delete(Long id) {
        expenseRepository.deleteById(id);
    }

    public BigDecimal totalThisMonth() {
        LocalDate now = LocalDate.now();
        LocalDate start = now.withDayOfMonth(1);
        LocalDate end = now.withDayOfMonth(now.lengthOfMonth());
        return expenseRepository.findByExpenseDateBetween(start, end).stream()
                .map(Expense::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public List<Expense> findBetween(LocalDate start, LocalDate end) {
        return expenseRepository.findByExpenseDateBetweenOrderByExpenseDateAsc(start, end);
    }
}
