package com.sivan.cranemanagement.service;

import com.sivan.cranemanagement.model.Customer;
import com.sivan.cranemanagement.repository.CustomerRepository;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class CustomerService {

    private final CustomerRepository customerRepository;

    public CustomerService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    public List<Customer> findAll() {
        return customerRepository.findAllByOrderByIdDesc();
    }

    public Customer findById(Long id) {
        return customerRepository.findById(id).orElseThrow(() ->
                new RuntimeException("Customer not found: " + id));
    }

    public Customer save(Customer customer) {
        return customerRepository.save(customer);
    }

    public void delete(Long id) {
        customerRepository.deleteById(id);
    }

    public long count() {
        return customerRepository.count();
    }
}
