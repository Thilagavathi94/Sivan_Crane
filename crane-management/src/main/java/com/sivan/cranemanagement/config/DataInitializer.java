package com.sivan.cranemanagement.config;

import com.sivan.cranemanagement.model.Crane;
import com.sivan.cranemanagement.model.User;
import com.sivan.cranemanagement.repository.CraneRepository;
import com.sivan.cranemanagement.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Runs once at application startup.
 *
 * Creates:
 * - Default admin login (username: admin / password: admin123)
 * - 5 sample crane records if no cranes exist
 *
 * Does NOT create:
 * - Sample customers
 * - Sample drivers
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final CraneRepository craneRepository;
    private final PasswordEncoder passwordEncoder;
    private final JdbcTemplate jdbcTemplate;

    public DataInitializer(UserRepository userRepository,
                           CraneRepository craneRepository,
                           PasswordEncoder passwordEncoder,
                           JdbcTemplate jdbcTemplate) {
        this.userRepository = userRepository;
        this.craneRepository = craneRepository;
        this.passwordEncoder = passwordEncoder;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) {

        // Database repair / migration checks
        repairLegacyTripSheetColumns();
        repairPaymentTargetColumns();
        repairDocumentNumberingColumns();

        // Create default admin user if it does not exist
        if (userRepository.findByUsername("admin").isEmpty()) {
            User admin = new User();
            admin.setUsername("admin");
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setFullName("Administrator");
            admin.setRole("ADMIN");

            userRepository.save(admin);
        }

        // Create 5 crane records only if no cranes exist
        if (craneRepository.count() == 0) {

            saveCrane(
                    "KCN-01",
                    "TN38CT7504",
                    "Hydra",
                    "12 Ton",
                    "Available"
            );

            saveCrane(
                    "KCN-02",
                    "TN38DS9893",
                    "Hydra",
                    "17 Ton",
                    "Working"
            );

            saveCrane(
                    "KCN-03",
                    "TN38CR1648",
                    "Hydra",
                    "12 Ton",
                    "Working"
            );

            saveCrane(
                    "KCN-04",
                    "TN38CT0931",
                    "Hydra",
                    "16 Ton",
                    "Available"
            );

            saveCrane(
                    "KCN-05",
                    "TN38DF6791",
                    "Hydra",
                    "13 Ton",
                    "Service"
            );
        }
    }

    /**
     * Repairs payment table columns for older database versions.
     */
    private void repairPaymentTargetColumns() {

        try {
            jdbcTemplate.execute(
                    "ALTER TABLE payments MODIFY invoice_id BIGINT NULL"
            );
        } catch (Exception ignored) {
            // Older or empty databases may not have payments yet.
        }

        try {
            if (!columnExists("payments", "trip_sheet_id")) {
                jdbcTemplate.execute(
                        "ALTER TABLE payments ADD COLUMN trip_sheet_id BIGINT NULL"
                );
            }
        } catch (Exception ignored) {
            // Hibernate will create the column for fresh databases.
        }
    }

    /**
     * Checks whether a column exists in the current database.
     */
    private boolean columnExists(String tableName, String columnName) {

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) " +
                "FROM INFORMATION_SCHEMA.COLUMNS " +
                "WHERE TABLE_SCHEMA = DATABASE() " +
                "AND TABLE_NAME = ? " +
                "AND COLUMN_NAME = ?",
                Integer.class,
                tableName,
                columnName
        );

        return count != null && count > 0;
    }

    /**
     * Keeps old customer databases compatible with financial-year document numbers.
     */
    private void repairDocumentNumberingColumns() {

        try {
            if (!columnExists("invoices", "financial_year")) {
                jdbcTemplate.execute("ALTER TABLE invoices ADD COLUMN financial_year VARCHAR(20) NULL");
            }
            if (!columnExists("invoices", "invoice_status")) {
                jdbcTemplate.execute("ALTER TABLE invoices ADD COLUMN invoice_status VARCHAR(50) DEFAULT 'Draft'");
            }
            jdbcTemplate.update("UPDATE invoices SET financial_year = '2026-27' WHERE financial_year IS NULL OR financial_year = ''");
            jdbcTemplate.update("UPDATE invoices SET invoice_status = 'Final' WHERE invoice_status IS NULL OR invoice_status = ''");
            dropUniqueIndexesForColumn("invoices", "invoice_no");
        } catch (Exception ignored) {
            // Hibernate will create the columns for fresh databases.
        }

        try {
            if (!columnExists("quotations", "financial_year")) {
                jdbcTemplate.execute("ALTER TABLE quotations ADD COLUMN financial_year VARCHAR(20) NULL");
            }
            jdbcTemplate.update("UPDATE quotations SET financial_year = '2026-27' WHERE financial_year IS NULL OR financial_year = ''");
            dropUniqueIndexesForColumn("quotations", "quotation_no");
        } catch (Exception ignored) {
            // Hibernate will create the columns for fresh databases.
        }
    }

    private void dropUniqueIndexesForColumn(String tableName, String columnName) {

        try {
            jdbcTemplate.queryForList(
                    "SELECT INDEX_NAME FROM INFORMATION_SCHEMA.STATISTICS " +
                    "WHERE TABLE_SCHEMA = DATABASE() " +
                    "AND TABLE_NAME = ? " +
                    "AND COLUMN_NAME = ? " +
                    "AND NON_UNIQUE = 0 " +
                    "AND INDEX_NAME <> 'PRIMARY'",
                    String.class,
                    tableName,
                    columnName
            ).forEach(indexName -> jdbcTemplate.execute(
                    "ALTER TABLE " + tableName + " DROP INDEX " + indexName
            ));
        } catch (Exception ignored) {
            // Some MySQL versions do not expose the legacy index in the same way.
        }
    }

    /**
     * Creates a crane record.
     */
    private void saveCrane(String no,
                           String reg,
                           String type,
                           String capacity,
                           String status) {

        Crane crane = new Crane();

        crane.setCraneNo(no);
        crane.setRegistrationNo(reg);
        crane.setType(type);
        crane.setCapacity(capacity);
        crane.setStatus(status);

        craneRepository.save(crane);
    }

    /**
     * Repairs legacy trip sheet columns.
     */
    private void repairLegacyTripSheetColumns() {

        String[] legacyWorkColumns = {
                "work_lifting",
                "work_loading",
                "work_unloading",
                "work_other"
        };

        for (String column : legacyWorkColumns) {

            try {

                jdbcTemplate.execute(
                        "ALTER TABLE trip_sheets MODIFY "
                        + column
                        + " BIT(1) NOT NULL DEFAULT b'0'"
                );

            } catch (Exception ignored) {
                // Clean databases do not have these legacy columns.
            }
        }
    }
}
