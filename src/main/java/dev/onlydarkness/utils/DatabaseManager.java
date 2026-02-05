package dev.onlydarkness.utils;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DatabaseManager {
    private Connection connection;

    private final String host, dbName, user, pass;
    private final int port;

    public DatabaseManager(String host, String dbName, String user, String pass, int port) {
        this.host = host;
        this.dbName = dbName;
        this.user = user;
        this.pass = pass;
        this.port = port;

        connect();
        createDefaultTables();
    }

    private void connect() {
        try {
            String url = "jdbc:mysql://" + host + ":" + port + "/" + dbName + "?useSSL=false&autoReconnect=true&allowPublicKeyRetrieval=true";
            this.connection = DriverManager.getConnection(url, user, pass);
            System.out.println("[DatabaseManager] Connected to MySQL successfully.");

        } catch (SQLException e) {
            System.out.println("[DatabaseManager] Could not connect to MySQL.");
        }
    }

    private void checkConnection() {
        try {
            if (connection == null || connection.isClosed() || !connection.isValid(2)) {
                System.out.println("[DatabaseManager] Connection lost. Reconnecting...");
                connect();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void createDefaultTables() {
        // MySQL Syntax: AUTOINCREMENT yerine AUTO_INCREMENT kullanılır.
        String sql = "CREATE TABLE IF NOT EXISTS users (" +
                "id INT PRIMARY KEY AUTO_INCREMENT, " +
                "username VARCHAR(100) NOT NULL UNIQUE, " +
                "discord VARCHAR(100) NOT NULL UNIQUE, " +
                "email VARCHAR(100) NOT NULL UNIQUE, " +
                "phone VARCHAR(100) NOT NULL UNIQUE, " +
                "password VARCHAR(100) NOT NULL, " +
                "role VARCHAR(50) DEFAULT 'user', " +
                "coins INT DEFAULT 0" +
                ");";
        executeUpdate(sql);

        String productSql = "CREATE TABLE IF NOT EXISTS products (" +
                "id INT PRIMARY KEY AUTO_INCREMENT, " +
                "title VARCHAR(100) NOT NULL, " +
                "name VARCHAR(255) NOT NULL, " +
                "description TEXT, " +
                "ptext varchar(255) NOT NULL, " +
                "price VARCHAR(50), " +
                "image_url TEXT, " +
                "buy_link TEXT" +
                ");";

        executeUpdate(productSql);
    }

    public void addProduct(String title, String name, String description, String ptext, String price, String imageUrl, String buyLink) {
        String sql = "INSERT INTO products (title, name, description, ptext, price, image_url, buy_link) VALUES (?, ?, ?, ?, ?, ?, ?)";
        executeUpdate(sql, title, name, description, ptext, price, imageUrl, buyLink);
    }

    public void removeProduct(int name) {
        String sql = "DELETE FROM products WHERE id = ?";
        executeUpdate(sql, name);
    }

    public List<Map<String, Object>> getAllProducts() {
        return executeQuery("SELECT * FROM products");
    }

    public void executeUpdate(String sql, Object... args) {
        checkConnection();
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            for (int i = 0; i < args.length; i++) {
                stmt.setObject(i + 1, args[i]);
            }
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public List<Map<String, Object>> executeQuery(String sql, Object... args) {
        checkConnection();
        List<Map<String, Object>> results = new ArrayList<>();
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            for (int i = 0; i < args.length; i++) {
                stmt.setObject(i + 1, args[i]);
            }
            try (ResultSet rs = stmt.executeQuery()) {
                ResultSetMetaData metaData = rs.getMetaData();
                int columnCount = metaData.getColumnCount();

                while (rs.next()) {
                    Map<String, Object> row = new HashMap<>();
                    for (int i = 1; i <= columnCount; i++) {
                        row.put(metaData.getColumnName(i), rs.getObject(i));
                    }
                    results.add(row);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return results;
    }

    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void createUser(String username, String discord, String email, String phone, String password) {

        String sql = "INSERT IGNORE INTO users (username, discord, email, phone, password) VALUES (?, ?, ?, ?, ?)";


        executeUpdate(sql, username, discord, email, phone, password);
    }

    public Map<String, Object> getUserByDiscord(String discordName) {
        String sql = "SELECT * FROM users WHERE discord = ?";
        List<Map<String, Object>> results = executeQuery(sql, discordName);
        if (results.isEmpty()) return null;
        return results.get(0);
    }

    public Map<String, Object> getUser(String username) {
        String sql = "SELECT * FROM users WHERE username = ?";
        List<Map<String, Object>> results = executeQuery(sql, username);
        if (results.isEmpty()) return null;
        return results.get(0);
    }

    public void addCoins(String username, int amount) {
        String sql = "UPDATE users SET coins = coins + ? WHERE username = ?";
        executeUpdate(sql, amount, username);
    }
}
