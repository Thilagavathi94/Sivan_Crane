package com.sivan.cranemanagement.repository;

import com.sivan.cranemanagement.model.AppSettings;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppSettingsRepository extends JpaRepository<AppSettings, Long> {
}
