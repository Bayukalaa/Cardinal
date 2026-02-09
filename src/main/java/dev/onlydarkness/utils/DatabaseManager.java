package dev.onlydarkness.utils;

import java.sql.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;


public class DatabaseManager {
    private static final Logger LOGGER = Logger.getLogger(DatabaseManager.class.getName());
    private Connection connection;

    private final String host, dbName, user, pass;
    private final int port;

    public DatabaseManager(String host, String dbName, String user, String pass, int port) {
        this.host = host;
        this.dbName = dbName;
        this.user = user;
        this.pass = pass;
        this.port = port;

        initializeDatabase();
    }


    private void initializeDatabase() {
        connect();
        createSchema();
    }

    private synchronized void connect() {
        try {

            String url = String.format("jdbc:mysql://%s:%d/%s?useSSL=false&autoReconnect=true" +
                            "&allowPublicKeyRetrieval=true&serverTimezone=UTC&characterEncoding=UTF-8",
                    host, port, dbName);

            this.connection = DriverManager.getConnection(url, user, pass);
            LOGGER.info("[CORE-DB] Successfully linked to the neural database.");
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "[CORE-DB] Connection failure! System may be offline.", e);
        }
    }


    private void ensureConnection() {
        try {
            if (connection == null || connection.isClosed() || !connection.isValid(2)) {
                LOGGER.warning("[CORE-DB] Link lost. Attempting emergency reconnection...");
                connect();
            }
        } catch (SQLException e) {
            LOGGER.severe("[CORE-DB] Critical error during link validation.");
        }
    }

    private void createSchema() {

        String usersTable = "CREATE TABLE IF NOT EXISTS users (" +
                "id INT PRIMARY KEY AUTO_INCREMENT, " +
                "first_name VARCHAR(64) NOT NULL, " +
                "last_name VARCHAR(64) NOT NULL, " +
                "email VARCHAR(128) NOT NULL UNIQUE, " +
                "username VARCHAR(64) NOT NULL UNIQUE, " +
                "password CHAR(64) NOT NULL, " +
                "clearance_level VARCHAR(32) DEFAULT 'L1-USER', " +
                "is_active BOOLEAN DEFAULT TRUE, " +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;";


        String productsTable = "CREATE TABLE IF NOT EXISTS products (" +
                "id INT PRIMARY KEY AUTO_INCREMENT, " +
                "title VARCHAR(128) NOT NULL, " +
                "name VARCHAR(255) NOT NULL, " +
                "description TEXT, " +
                "price DECIMAL(10,2) DEFAULT 0.00, " +
                "image_url VARCHAR(512), " +
                "buy_link VARCHAR(512)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;";

        executeInternalUpdate(usersTable);
        executeInternalUpdate(productsTable);
    }


    public boolean registerOperator(String fName, String lName, String email, String username, String hashedPass) {
        String sql = "INSERT INTO users (first_name, last_name, email, username, password) VALUES (?, ?, ?, ?, ?)";
        return executeUpdate(sql, fName, lName, email, username, hashedPass) > 0;
    }


    public Map<String, Object> authenticate(String operatorId, String hashedPass) {
        String sql = "SELECT id, username, first_name, last_name, clearance_level FROM users " +
                "WHERE username = ? AND password = ? AND is_active = TRUE";
        List<Map<String, Object>> result = executeQuery(sql, operatorId, hashedPass);
        return result.isEmpty() ? null : result.get(0);
    }

    public Map<String, Object> getUserData(String username) {
        String sql = "SELECT * FROM users WHERE username = ?";
        List<Map<String, Object>> results = executeQuery(sql, username);
        return results.isEmpty() ? null : results.get(0);
    }



    public void addProduct(String title, String name, String description, String text, String price, String imageUrl, String buyLink) {
        String sql = "INSERT INTO products (title, name, description, price, image_url, buy_link) VALUES (?, ?, ?, ?, ?, ?)";
        executeUpdate(sql, title, name, description, price, imageUrl, buyLink);
    }

    public void removeProduct(int id) {
        String sql = "DELETE FROM products WHERE id = ?";
        executeUpdate(sql, id);
    }

    public List<Map<String, Object>> getAllProducts() {
        return executeQuery("SELECT * FROM products");
    }

    public int executeUpdate(String sql, Object... params) {
        ensureConnection();
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            bindParameters(stmt, params);
            return stmt.executeUpdate();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Update Execution Failed: " + sql, e);
            return -1;
        }
    }


    public List<Map<String, Object>> executeQuery(String sql, Object... params) {
        ensureConnection();
        List<Map<String, Object>> rows = new ArrayList<>();
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            bindParameters(stmt, params);
            try (ResultSet rs = stmt.executeQuery()) {
                ResultSetMetaData meta = rs.getMetaData();
                int colCount = meta.getColumnCount();
                while (rs.next()) {
                    Map<String, Object> columns = new HashMap<>();
                    for (int i = 1; i <= colCount; i++) {
                        columns.put(meta.getColumnLabel(i), rs.getObject(i));
                    }
                    rows.add(columns);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Query Execution Failed: " + sql, e);
        }
        return rows;
    }

    private void bindParameters(PreparedStatement stmt, Object... params) throws SQLException {
        for (int i = 0; i < params.length; i++) {
            stmt.setObject(i + 1, params[i]);
        }
    }

    private void executeInternalUpdate(String sql) {
        try (Statement stmt = connection.createStatement()) {
            stmt.executeUpdate(sql);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Schema Initialization Failed", e);
        }
    }

    public void shutdown() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                LOGGER.info("[CORE-DB] Neural link safely terminated.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}