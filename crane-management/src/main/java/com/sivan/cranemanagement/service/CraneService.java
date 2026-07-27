package com.sivan.cranemanagement.service;

import com.sivan.cranemanagement.model.Crane;
import com.sivan.cranemanagement.repository.CraneRepository;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class CraneService {

    private final CraneRepository craneRepository;

    public CraneService(CraneRepository craneRepository) {
        this.craneRepository = craneRepository;
    }

    public List<Crane> findAll() {
        return craneRepository.findAllByOrderByIdAsc();
    }

    public Crane findById(Long id) {
        return craneRepository.findById(id).orElseThrow(() ->
                new RuntimeException("Crane not found: " + id));
    }

    public Crane save(Crane crane) {
        return craneRepository.save(crane);
    }

    public void delete(Long id) {
        craneRepository.deleteById(id);
    }

    public long count() {
        return craneRepository.count();
    }

    public long countByStatus(String status) {
        return craneRepository.countByStatus(status);
    }
}
