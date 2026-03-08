package com.campus.lostfound.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

@Component
public class SchemaMigration implements ApplicationRunner {
  private final DataSource dataSource;
  public SchemaMigration(DataSource dataSource) {
    this.dataSource = dataSource;
  }
  @Override
  public void run(ApplicationArguments args) throws Exception {
    try (Connection conn = dataSource.getConnection()) {
      ensureColumn(conn, "lost_item", "audit_status", "ALTER TABLE lost_item ADD COLUMN audit_status TINYINT DEFAULT 0");
      ensureColumn(conn, "lost_item", "claim_status", "ALTER TABLE lost_item ADD COLUMN claim_status TINYINT DEFAULT 0");
      ensureColumn(conn, "lost_item", "images", "ALTER TABLE lost_item ADD COLUMN images TEXT");
      ensureColumn(conn, "found_item", "audit_status", "ALTER TABLE found_item ADD COLUMN audit_status TINYINT DEFAULT 0");
      ensureColumn(conn, "found_item", "claim_status", "ALTER TABLE found_item ADD COLUMN claim_status TINYINT DEFAULT 0");
      ensureColumn(conn, "found_item", "images", "ALTER TABLE found_item ADD COLUMN images TEXT");
    }
  }
  private void ensureColumn(Connection conn, String table, String column, String ddl) throws Exception {
    String sql = "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = database() AND table_name = ? AND column_name = ?";
    try (PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, table);
      ps.setString(2, column);
      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next() && rs.getInt(1) == 0) {
          try (Statement st = conn.createStatement()) {
            st.executeUpdate(ddl);
          }
        }
      }
    }
  }
}

