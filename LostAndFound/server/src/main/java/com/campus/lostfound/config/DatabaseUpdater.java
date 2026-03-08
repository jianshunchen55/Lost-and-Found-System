package com.campus.lostfound.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class DatabaseUpdater implements CommandLineRunner {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) throws Exception {
        System.out.println("Starting database schema update...");
        
        addColumn("lost_item", "audit_status", "TINYINT DEFAULT 0");
        addColumn("lost_item", "claim_status", "TINYINT DEFAULT 0");
        addColumn("lost_item", "claim_user_id", "BIGINT");
        addColumn("lost_item", "claim_time", "DATETIME");
        addColumn("lost_item", "deleted", "TINYINT DEFAULT 0");
        
        addColumn("found_item", "audit_status", "TINYINT DEFAULT 0");
        addColumn("found_item", "claim_status", "TINYINT DEFAULT 0");
        addColumn("found_item", "claim_user_id", "BIGINT");
        addColumn("found_item", "claim_time", "DATETIME");
        addColumn("found_item", "deleted", "TINYINT DEFAULT 0");

        // Add missing columns for found_item location features
        addColumn("found_item", "latitude", "DOUBLE");
        addColumn("found_item", "longitude", "DOUBLE");
        addColumn("found_item", "point_id", "BIGINT");
        addColumn("found_item", "images", "TEXT");

        // Add columns for claim and audit details
        addColumn("lost_item", "claim_name", "VARCHAR(64)");
        addColumn("lost_item", "claim_phone", "VARCHAR(20)");
        addColumn("lost_item", "claim_audit_reply", "TEXT");
        addColumn("lost_item", "audit_reply", "TEXT");

        addColumn("found_item", "claim_name", "VARCHAR(64)");
        addColumn("found_item", "claim_phone", "VARCHAR(20)");
        addColumn("found_item", "claim_audit_reply", "TEXT");
        addColumn("found_item", "audit_reply", "TEXT");
        
        // Cleanup zombie data (Fix for items that were not correctly deleted or have missing images)
        try {
            jdbcTemplate.update("UPDATE found_item SET deleted = 1 WHERE title IN ('白云', 'Found Wallet')");
            jdbcTemplate.update("UPDATE found_item SET deleted = 1 WHERE user_id IS NULL");
            System.out.println("Cleaned up zombie found items.");
        } catch (Exception e) {
            System.out.println("Cleanup failed: " + e.getMessage());
        }

        System.out.println("Database schema update finished.");
    }

    private void addColumn(String table, String column, String type) {
        try {
            String sql = "ALTER TABLE " + table + " ADD COLUMN " + column + " " + type;
            jdbcTemplate.execute(sql);
            System.out.println("Added column " + column + " to " + table);
        } catch (Exception e) {
            if (e.getMessage().contains("Duplicate column name")) {
                System.out.println("Column " + column + " already exists in " + table);
            } else {
                System.out.println("Failed to add column " + column + " to " + table + ": " + e.getMessage());
            }
        }
    }
}
