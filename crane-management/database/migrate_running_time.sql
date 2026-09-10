-- Run once when upgrading an existing MySQL/MariaDB database. New installs
-- receive these columns from schema.sql and Hibernate ddl-auto=update adds them.
ALTER TABLE trip_sheets
  ADD COLUMN running_hours INT NULL AFTER total_hours,
  ADD COLUMN running_minutes INT NULL AFTER running_hours;

ALTER TABLE invoices
  ADD COLUMN manual_running_hours_whole INT NULL AFTER manual_running_hours,
  ADD COLUMN manual_running_minutes INT NULL AFTER manual_running_hours_whole;

-- Preserve legacy decimal-hour records and populate their new display fields.
UPDATE trip_sheets
SET running_hours = FLOOR(total_hours),
    running_minutes = ROUND((total_hours - FLOOR(total_hours)) * 60)
WHERE running_hours IS NULL AND total_hours IS NOT NULL;

UPDATE trip_sheets SET running_hours = running_hours + 1, running_minutes = 0 WHERE running_minutes = 60;

UPDATE invoices
SET manual_running_hours_whole = FLOOR(manual_running_hours),
    manual_running_minutes = ROUND((manual_running_hours - FLOOR(manual_running_hours)) * 60)
WHERE manual_running_hours_whole IS NULL AND manual_running_hours IS NOT NULL;

UPDATE invoices
SET manual_running_hours_whole = manual_running_hours_whole + 1, manual_running_minutes = 0 WHERE manual_running_minutes = 60;
