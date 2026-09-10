package com.sivan.cranemanagement.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.BufferedWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class BackupService {

    private static final DateTimeFormatter FILE_TIME = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private final JdbcTemplate jdbcTemplate;

    @Value("${app.backup.dir:${user.home}/Sivan-Crane-Backups}")
    private String backupDir;

    public BackupService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Scheduled(cron = "${app.backup.cron:0 0 21 * * *}")
    public void dailyBackup() throws IOException {
        backupNow();
    }

    public Path backupNow() throws IOException {
        Files.createDirectories(backupPath());
        Path outputFile = backupPath().resolve("sivan-crane-backup-" + LocalDateTime.now().format(FILE_TIME) + ".sql");

        try (BufferedWriter writer = Files.newBufferedWriter(outputFile, StandardCharsets.UTF_8)) {
            writer.write("-- Sivan Crane Service backup created at " + LocalDateTime.now());
            writer.newLine();
            writer.write("SET FOREIGN_KEY_CHECKS=0;");
            writer.newLine();
            for (String table : tableNames()) {
                writeTable(writer, table);
            }
            writer.write("SET FOREIGN_KEY_CHECKS=1;");
            writer.newLine();
        }
        return outputFile;
    }

    public String getBackupDir() {
        return backupPath().toString();
    }

    private List<String> tableNames() {
        return jdbcTemplate.queryForList(
                "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_TYPE = 'BASE TABLE' ORDER BY TABLE_NAME",
                String.class);
    }

    private void writeTable(BufferedWriter writer, String table) throws IOException {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("SELECT * FROM `" + table + "`");
        if (rows.isEmpty()) {
            return;
        }

        writer.newLine();
        writer.write("-- Table: " + table);
        writer.newLine();
        writer.write("DELETE FROM `" + table + "`;");
        writer.newLine();

        for (Map<String, Object> row : rows) {
            String columns = row.keySet().stream()
                    .map(column -> "`" + column + "`")
                    .collect(Collectors.joining(", "));
            String values = row.values().stream()
                    .map(this::sqlValue)
                    .collect(Collectors.joining(", "));
            writer.write("INSERT INTO `" + table + "` (" + columns + ") VALUES (" + values + ");");
            writer.newLine();
        }
    }

    private String sqlValue(Object value) {
        if (value == null) {
            return "NULL";
        }
        if (value instanceof Number || value instanceof BigDecimal) {
            return value.toString();
        }
        if (value instanceof Boolean bool) {
            return bool ? "1" : "0";
        }
        if (value instanceof Date || value instanceof Timestamp || value instanceof java.util.Date) {
            return "'" + value + "'";
        }
        return "'" + value.toString().replace("\\", "\\\\").replace("'", "''") + "'";
    }

    private Path backupPath() {
        return Paths.get(backupDir).toAbsolutePath().normalize();
    }
}
