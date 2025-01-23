import java.sql.*;

public class SQLiteExample {

    public static void main(String[] args) {
        try {
            // Load the SQLite JDBC driver (assuming it's in the classpath)
            Class.forName("org.sqlite.JDBC");

            // Connect to the database
            Connection conn = DriverManager.getConnection("\"C:\\sqlite\\xerial-sqlite-jdbc-5d22aab\\src\\test\\resources\\org\\sqlite\\currency_converter.db.db\"");
            // ... perform database operations ...

            // Close the connection
            conn.close();
        } catch (ClassNotFoundException e) {
            System.err.println("SQLite JDBC driver not found: " + e.getMessage());
        } catch (SQLException e) {
            System.err.println("Database error: " + e.getMessage());
        }
    }
}