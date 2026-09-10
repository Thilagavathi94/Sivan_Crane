package com.sivan.cranemanagement;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class CraneManagementApplication {
    public static void main(String[] args) {
        SpringApplication.run(CraneManagementApplication.class, args);
    }
}
