package com.sivan.cranemanagement.service;

import com.sivan.cranemanagement.model.Driver;
import com.sivan.cranemanagement.repository.DriverRepository;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class DriverService {

    private final DriverRepository driverRepository;

    public DriverService(DriverRepository driverRepository) {
        this.driverRepository = driverRepository;
    }

    public List<Driver> findAll() {
        return driverRepository.findAllByOrderByIdDesc();
    }

    public Driver findById(Long id) {
        return driverRepository.findById(id).orElseThrow(() ->
                new RuntimeException("Driver not found: " + id));
    }

    public Driver save(Driver driver) {
        return driverRepository.save(driver);
    }

    public void delete(Long id) {
        driverRepository.deleteById(id);
    }
}
