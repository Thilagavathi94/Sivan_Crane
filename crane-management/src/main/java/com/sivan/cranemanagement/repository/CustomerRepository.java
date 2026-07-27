package com.sivan.cranemanagement.repository;

import com.sivan.cranemanagement.model.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CustomerRepository extends JpaRepository<Customer, Long> {
    List<Customer> findAllByOrderByIdDesc();
}
