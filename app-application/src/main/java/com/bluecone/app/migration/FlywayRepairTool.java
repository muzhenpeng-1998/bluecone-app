package com.bluecone.app.migration;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.util.List;
import java.util.Map;

/**
 * Flyway Migration Repair Tool
 * 
 * This tool repairs failed Flyway migrations by:
 * 1. Rolling back partial database changes
 * 2. Cleaning up the Flyway schema history
 * 
 * Usage:
 * mvn spring-boot:run -Dspring-boot.run.profiles=local,repair -Dspring-boot.run.arguments="--repair.migration.version=20251218001"
 */
@SpringBootApplication(scanBasePackages = "com.bluecone.app")
@Profile("repair")
public class FlywayRepairTool {

    public static void main(String[] args) {
        System.out.println("=================================================================");
        System.out.println("Flyway Migration Repair Tool");
        System.out.println("=================================================================");
        SpringApplication.run(FlywayRepairTool.class, args);
    }

    @Bean
    public CommandLineRunner repairRunner(DataSource dataSource) {
        return args -> {
            JdbcTemplate jdbc = new JdbcTemplate(dataSource);
            
            // Get migration version from command line arguments
            String version = "20251218001"; // Default version
            for (String arg : args) {
                if (arg.startsWith("--repair.migration.version=")) {
                    version = arg.substring("--repair.migration.version=".length());
                }
            }
            
            System.out.println("\nRepairing migration version: " + version);
            System.out.println("-----------------------------------------------------------------");
            
            // Step 1: Check current state
            System.out.println("\n[Step 1] Checking current database state...");
            checkCurrentState(jdbc, version);
            
            // Step 2: Rollback partial changes for V20251218001
            System.out.println("\n[Step 2] Rolling back partial changes...");
            if ("20251218001".equals(version)) {
                rollbackV20251218001(jdbc);
            } else {
                System.out.println("⚠️  Warning: No rollback logic defined for version " + version);
                System.out.println("   You may need to manually rollback changes.");
            }
            
            // Step 3: Clean up Flyway schema history
            System.out.println("\n[Step 3] Cleaning up Flyway schema history...");
            cleanupFlywayHistory(jdbc, version);
            
            // Step 4: Verify cleanup
            System.out.println("\n[Step 4] Verifying cleanup...");
            verifyCleanup(jdbc, version);
            
            System.out.println("\n=================================================================");
            System.out.println("✓ Repair completed successfully!");
            System.out.println("=================================================================");
            System.out.println("\nNext steps:");
            System.out.println("1. Restart your Spring Boot application (without 'repair' profile)");
            System.out.println("2. Flyway will automatically re-run the migration");
            System.out.println("3. The migration should complete successfully this time");
            System.out.println();
        };
    }
    
    private void checkCurrentState(JdbcTemplate jdbc, String version) {
        try {
            // Check Flyway history
            String sql = "SELECT version, description, type, script, checksum, installed_by, " +
                        "installed_on, execution_time, success FROM flyway_schema_history " +
                        "WHERE version = ? ORDER BY installed_rank DESC LIMIT 1";
            List<Map<String, Object>> results = jdbc.queryForList(sql, version);
            
            if (results.isEmpty()) {
                System.out.println("   ℹ️  No Flyway history found for version " + version);
            } else {
                Map<String, Object> row = results.get(0);
                System.out.println("   Found Flyway history:");
                System.out.println("   - Version: " + row.get("version"));
                System.out.println("   - Description: " + row.get("description"));
                System.out.println("   - Success: " + row.get("success"));
                System.out.println("   - Installed on: " + row.get("installed_on"));
            }
            
            // Check specific columns for V20251218001
            if ("20251218001".equals(version)) {
                checkColumnExists(jdbc, "bc_payment_notify_log", "notify_id");
                checkColumnExists(jdbc, "bc_order", "close_reason");
                checkColumnExists(jdbc, "bc_order", "closed_at");
                checkIndexExists(jdbc, "bc_payment_notify_log", "uk_notify_id");
            }
            
        } catch (Exception e) {
            System.err.println("   ✗ Error checking current state: " + e.getMessage());
        }
    }
    
    private void checkColumnExists(JdbcTemplate jdbc, String tableName, String columnName) {
        try {
            String sql = "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS " +
                        "WHERE TABLE_SCHEMA = DATABASE() " +
                        "AND TABLE_NAME = ? AND COLUMN_NAME = ?";
            Integer count = jdbc.queryForObject(sql, Integer.class, tableName, columnName);
            if (count != null && count > 0) {
                System.out.println("   ✓ Column " + tableName + "." + columnName + " exists");
            } else {
                System.out.println("   ✗ Column " + tableName + "." + columnName + " does not exist");
            }
        } catch (Exception e) {
            System.err.println("   ⚠️  Error checking column " + tableName + "." + columnName + ": " + e.getMessage());
        }
    }
    
    private void checkIndexExists(JdbcTemplate jdbc, String tableName, String indexName) {
        try {
            String sql = "SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS " +
                        "WHERE TABLE_SCHEMA = DATABASE() " +
                        "AND TABLE_NAME = ? AND INDEX_NAME = ?";
            Integer count = jdbc.queryForObject(sql, Integer.class, tableName, indexName);
            if (count != null && count > 0) {
                System.out.println("   ✓ Index " + tableName + "." + indexName + " exists");
            } else {
                System.out.println("   ✗ Index " + tableName + "." + indexName + " does not exist");
            }
        } catch (Exception e) {
            System.err.println("   ⚠️  Error checking index " + tableName + "." + indexName + ": " + e.getMessage());
        }
    }
    
    private void rollbackV20251218001(JdbcTemplate jdbc) {
        // Drop unique index if exists
        dropIndexIfExists(jdbc, "bc_payment_notify_log", "uk_notify_id");
        
        // Drop columns if they exist
        dropColumnIfExists(jdbc, "bc_payment_notify_log", "notify_id");
        dropColumnIfExists(jdbc, "bc_order", "close_reason");
        dropColumnIfExists(jdbc, "bc_order", "closed_at");
    }
    
    private void dropIndexIfExists(JdbcTemplate jdbc, String tableName, String indexName) {
        try {
            // Check if index exists
            String checkSql = "SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS " +
                            "WHERE TABLE_SCHEMA = DATABASE() " +
                            "AND TABLE_NAME = ? AND INDEX_NAME = ?";
            Integer count = jdbc.queryForObject(checkSql, Integer.class, tableName, indexName);
            
            if (count != null && count > 0) {
                String dropSql = "ALTER TABLE " + tableName + " DROP INDEX " + indexName;
                jdbc.execute(dropSql);
                System.out.println("   ✓ Dropped index " + tableName + "." + indexName);
            } else {
                System.out.println("   ℹ️  Index " + tableName + "." + indexName + " does not exist (skipped)");
            }
        } catch (Exception e) {
            System.err.println("   ✗ Error dropping index " + tableName + "." + indexName + ": " + e.getMessage());
        }
    }
    
    private void dropColumnIfExists(JdbcTemplate jdbc, String tableName, String columnName) {
        try {
            // Check if column exists
            String checkSql = "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS " +
                            "WHERE TABLE_SCHEMA = DATABASE() " +
                            "AND TABLE_NAME = ? AND COLUMN_NAME = ?";
            Integer count = jdbc.queryForObject(checkSql, Integer.class, tableName, columnName);
            
            if (count != null && count > 0) {
                String dropSql = "ALTER TABLE " + tableName + " DROP COLUMN " + columnName;
                jdbc.execute(dropSql);
                System.out.println("   ✓ Dropped column " + tableName + "." + columnName);
            } else {
                System.out.println("   ℹ️  Column " + tableName + "." + columnName + " does not exist (skipped)");
            }
        } catch (Exception e) {
            System.err.println("   ✗ Error dropping column " + tableName + "." + columnName + ": " + e.getMessage());
        }
    }
    
    private void cleanupFlywayHistory(JdbcTemplate jdbc, String version) {
        try {
            String sql = "DELETE FROM flyway_schema_history WHERE version = ?";
            int deleted = jdbc.update(sql, version);
            if (deleted > 0) {
                System.out.println("   ✓ Deleted " + deleted + " record(s) from flyway_schema_history");
            } else {
                System.out.println("   ℹ️  No records found in flyway_schema_history for version " + version);
            }
        } catch (Exception e) {
            System.err.println("   ✗ Error cleaning up Flyway history: " + e.getMessage());
            throw new RuntimeException("Failed to clean up Flyway history", e);
        }
    }
    
    private void verifyCleanup(JdbcTemplate jdbc, String version) {
        try {
            // Verify Flyway history is cleaned
            String sql = "SELECT COUNT(*) FROM flyway_schema_history WHERE version = ?";
            Integer count = jdbc.queryForObject(sql, Integer.class, version);
            
            if (count != null && count == 0) {
                System.out.println("   ✓ Flyway history cleaned successfully");
            } else {
                System.err.println("   ✗ Flyway history still contains records for version " + version);
            }
            
            // Verify columns are removed for V20251218001
            if ("20251218001".equals(version)) {
                verifyColumnRemoved(jdbc, "bc_payment_notify_log", "notify_id");
                verifyColumnRemoved(jdbc, "bc_order", "close_reason");
                verifyColumnRemoved(jdbc, "bc_order", "closed_at");
                verifyIndexRemoved(jdbc, "bc_payment_notify_log", "uk_notify_id");
            }
            
        } catch (Exception e) {
            System.err.println("   ✗ Error verifying cleanup: " + e.getMessage());
        }
    }
    
    private void verifyColumnRemoved(JdbcTemplate jdbc, String tableName, String columnName) {
        try {
            String sql = "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS " +
                        "WHERE TABLE_SCHEMA = DATABASE() " +
                        "AND TABLE_NAME = ? AND COLUMN_NAME = ?";
            Integer count = jdbc.queryForObject(sql, Integer.class, tableName, columnName);
            
            if (count != null && count == 0) {
                System.out.println("   ✓ Column " + tableName + "." + columnName + " removed");
            } else {
                System.err.println("   ✗ Column " + tableName + "." + columnName + " still exists");
            }
        } catch (Exception e) {
            System.err.println("   ⚠️  Error verifying column " + tableName + "." + columnName + ": " + e.getMessage());
        }
    }
    
    private void verifyIndexRemoved(JdbcTemplate jdbc, String tableName, String indexName) {
        try {
            String sql = "SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS " +
                        "WHERE TABLE_SCHEMA = DATABASE() " +
                        "AND TABLE_NAME = ? AND INDEX_NAME = ?";
            Integer count = jdbc.queryForObject(sql, Integer.class, tableName, indexName);
            
            if (count != null && count == 0) {
                System.out.println("   ✓ Index " + tableName + "." + indexName + " removed");
            } else {
                System.err.println("   ✗ Index " + tableName + "." + indexName + " still exists");
            }
        } catch (Exception e) {
            System.err.println("   ⚠️  Error verifying index " + tableName + "." + indexName + ": " + e.getMessage());
        }
    }
}
