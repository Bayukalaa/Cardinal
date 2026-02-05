package dev.onlydarkness.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

public class ConfigManager {

    private final Properties properties = new Properties();
    private final File configFile;

    public ConfigManager(String fileName) {
        this.configFile = new File(fileName);
        load();
    }

    private void load() {
        try {
            if (!configFile.exists()) {
                createDefaultConfig();
            }
            try (FileInputStream fis = new FileInputStream(configFile)) {
                properties.load(fis);
            }
        } catch (IOException e) {
            System.err.println("[ConfigManager] Failed to load config: " + e.getMessage());
        }
    }

    private void createDefaultConfig() {
        try {

            properties.setProperty("server-name", "Cardinal Core");
            properties.setProperty("debug-mode", "false");
            properties.setProperty("web-server-enabled", "true");
            properties.setProperty("auto-run-webserver", "true");
            properties.setProperty("web-port", "8080");
            properties.setProperty("web-root", "web");
            properties.setProperty("db-host", "localhost");
            properties.setProperty("db-port", "3306");
            properties.setProperty("db-name", "cardinal_db");
            properties.setProperty("db-user", "root");
            properties.setProperty("db-pass", "");
            properties.setProperty("discord-enabled", "false");
            properties.setProperty("discord-token", "TOKEN_YAZ");
            properties.setProperty("discord-guild-id", "SUNUCU_ID_YAZ");
            properties.setProperty("discord-channel-id", "KANAL_ID_YAZ");
            properties.setProperty("modules-folder", "modules");
            properties.setProperty("file-logging-enabled", "true");
            properties.setProperty("backup-enabled", "true");
            properties.setProperty("backup-path", "backups");

            properties.setProperty("files-to-backup", "modules,server.properties,logs");

            save("Cardinal System Configuration");
            System.out.println("[ConfigManager] Created default " + configFile.getName());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void save(String comments) {
        try (FileOutputStream fos = new FileOutputStream(configFile)) {
            properties.store(fos, comments);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void reloadConfig(){
        properties.clear();
        load();
        System.out.println("[ConfigManager] Reloaded " + configFile.getName());
    }



    public String getString(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }

    public int getInt(String key, int defaultValue) {
        try {
            return Integer.parseInt(properties.getProperty(key));
        } catch (NumberFormatException | NullPointerException e) {
            return defaultValue;
        }
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        String val = properties.getProperty(key);
        return val != null ? Boolean.parseBoolean(val) : defaultValue;
    }


    public String[] getList(String key) {
        String val = properties.getProperty(key);
        if (val == null || val.isEmpty()) return new String[0];
        return val.split(",");
    }
}