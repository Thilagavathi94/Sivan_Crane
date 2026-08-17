package com.sivan.cranemanagement.config;

import com.sivan.cranemanagement.model.*;
import com.sivan.cranemanagement.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Runs once at application startup.
 * Creates a default admin login (username: admin / password: admin123)
 * and a few sample master records so the system isn't empty on first use.
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final CraneRepository craneRepository;
    private final DriverRepository driverRepository;
    private final CustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;
    private final JdbcTemplate jdbcTemplate;

    public DataInitializer(UserRepository userRepository, CraneRepository craneRepository,
                            DriverRepository driverRepository, CustomerRepository customerRepository,
                            PasswordEncoder passwordEncoder, JdbcTemplate jdbcTemplate) {
        this.userRepository = userRepository;
        this.craneRepository = craneRepository;
        this.driverRepository = driverRepository;
        this.customerRepository = customerRepository;
        this.passwordEncoder = passwordEncoder;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) {
        repairLegacyTripSheetColumns();
        repairPaymentTargetColumns();

        if (userRepository.findByUsername("admin").isEmpty()) {
            User admin = new User();
            admin.setUsername("admin");
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setFullName("Administrator");
            admin.setRole("ADMIN");
            userRepository.save(admin);
        }

        if (craneRepository.count() == 0) {
            saveCrane("KCN-01", "TN38CT7504", "Hydra", "12 Ton", "Available");
            saveCrane("KCN-02", "TN38DS9893", "Hydra", "17 Ton", "Working");
            saveCrane("KCN-03", "TN38CR1648", "Hydra", "12 Ton", "Working");
            saveCrane("KCN-04", "TN38CT0931", "Hydra", "16 Ton", "Available");
            saveCrane("KCN-05", "TN38DF6791", "Hydra", "13 Ton", "Service");
        }

        if (driverRepository.count() == 0) {
            saveDriver("Murugan", "9876543210", "TN-DL-00123");
            saveDriver("Selvam", "9876543211", "TN-DL-00456");
            saveDriver("Karthik", "9876543212", "TN-DL-00789");
        }

        if (customerRepository.count() == 0) {
            saveCustomer("Siva Construction", "9787654321", "33ABCCD1234F1Z5", "Coimbatore");
            saveCustomer("Raj Builders", "9798765432", "33ABQFG5678H1Z2", "Tirupur");
            saveCustomer("Kumar Transport", "9845678901", "33AACVB1111L1Z1", "Erode");
            saveCustomer("Vetri Infra", "9889879697", "33AACVV2222R1Z3", "Chennai");
        }
    }

    private void repairPaymentTargetColumns() {
        try {
            jdbcTemplate.execute("ALTER TABLE payments MODIFY invoice_id BIGINT NULL");
        } catch (Exception ignored) {
            // Older or empty databases may not have payments yet.
        }
        try {
            if (!columnExists("payments", "trip_sheet_id")) {
                jdbcTemplate.execute("ALTER TABLE payments ADD COLUMN trip_sheet_id BIGINT NULL");
            }
        } catch (Exception ignored) {
            // Hibernate will create the column for fresh databases.
        }
    }

    private boolean columnExists(String tableName, String columnName) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND COLUMN_NAME = ?",
                Integer.class,
                tableName,
                columnName);
        return count != null && count > 0;
    }

    private void saveCrane(String no, String reg, String type, String capacity, String status) {
        Crane c = new Crane();
        c.setCraneNo(no);
        c.setRegistrationNo(reg);
        c.setType(type);
        c.setCapacity(capacity);
        c.setStatus(status);
        craneRepository.save(c);
    }

    private void saveDriver(String name, String phone, String license) {
        Driver d = new Driver();
        d.setName(name);
        d.setPhone(phone);
        d.setLicenseNo(license);
        driverRepository.save(d);
    }

    private void repairLegacyTripSheetColumns() {
        String[] legacyWorkColumns = {"work_lifting", "work_loading", "work_unloading", "work_other"};
        for (String column : legacyWorkColumns) {
            try {
                jdbcTemplate.execute("ALTER TABLE trip_sheets MODIFY " + column + " BIT(1) NOT NULL DEFAULT b'0'");
            } catch (Exception ignored) {
                // Clean databases do not have these legacy columns.
            }
        }
    }

    private void saveCustomer(String name, String phone, String gst, String address) {
        Customer c = new Customer();
        c.setName(name);
        c.setPhone(phone);
        c.setGstNumber(gst);
        c.setAddress(address);
        customerRepository.save(c);
    }
}
