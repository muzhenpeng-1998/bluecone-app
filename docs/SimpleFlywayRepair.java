import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

/**
 * Simple Flyway Repair Script
 * Removes failed migration records from flyway_schema_history
 * 
 * Usage: java SimpleFlywayRepair.java
 */
public class SimpleFlywayRepair {
    public static void main(String[] args) {
        String url = "jdbc:mysql://localhost:3306/bluecone?useSSL=false&serverTimezone=Asia/Shanghai";
        String username = "root";
        String password = "";
        String version = "20251222001";
        
        System.out.println("=================================================================");
        System.out.println("Simple Flyway Repair Tool");
        System.out.println("=================================================================");
        System.out.println("Database: " + url);
        System.out.println("Version to repair: " + version);
        System.out.println();
        
        try (Connection conn = DriverManager.getConnection(url, username, password);
             Statement stmt = conn.createStatement()) {
            
            // Delete failed migration record
            String sql = "DELETE FROM flyway_schema_history WHERE version = '" + version + "' AND success = 0";
            int deleted = stmt.executeUpdate(sql);
            
            System.out.println("✓ Deleted " + deleted + " failed migration record(s)");
            System.out.println();
            System.out.println("=================================================================");
            System.out.println("Repair completed successfully!");
            System.out.println("=================================================================");
            System.out.println();
            System.out.println("Next steps:");
            System.out.println("1. Restart your Spring Boot application");
            System.out.println("2. Flyway will automatically re-run the migration");
            System.out.println("3. The migration should complete successfully with IF NOT EXISTS");
            System.out.println();
            
        } catch (Exception e) {
            System.err.println("✗ Error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}

