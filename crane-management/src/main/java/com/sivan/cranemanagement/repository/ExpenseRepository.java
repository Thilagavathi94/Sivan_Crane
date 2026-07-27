package com.sivan.cranemanagement.repository;

import com.sivan.cranemanagement.model.Expense;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;

public interface ExpenseRepository extends JpaRepository<Expense, Long> {
    List<Expense> findAllByOrderByIdDesc();
    List<Expense> findByExpenseDateBetween(LocalDate start, LocalDate end);
    List<Expense> findByExpenseDateBetweenOrderByExpenseDateAsc(LocalDate start, LocalDate end);
}
