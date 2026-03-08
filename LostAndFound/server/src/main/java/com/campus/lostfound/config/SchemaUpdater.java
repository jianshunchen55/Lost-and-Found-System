package com.campus.lostfound.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;

@Component
public class SchemaUpdater implements CommandLineRunner {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) throws Exception {
        System.out.println("Checking database schema updates...");
        try {
            addColumn("friendship", "requester_alias", "VARCHAR(64)");
            addColumn("friendship", "addressee_alias", "VARCHAR(64)");
            System.out.println("Schema check completed.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void addColumn(String table, String column, String type) {
        try {
            jdbcTemplate.execute("ALTER TABLE " + table + " ADD COLUMN " + column + " " + type);
            System.out.println("Successfully added column " + column + " to " + table);
        } catch (Exception e) {
            // Check if error is "Duplicate column name"
            if (e.getMessage() != null && (e.getMessage().contains("Duplicate column") || e.getMessage().contains("exists"))) {
                System.out.println("Column " + column + " already exists in " + table);
            } else {
                System.err.println("Failed to add column " + column + ": " + e.getMessage());
                // e.printStackTrace(); // Optional
            }
        }
    }
}
