package dev.onlydarkness.utils.modules;

import dev.onlydarkness.utils.DatabaseManager;
import dev.onlydarkness.utils.core.IModule;
import dev.onlydarkness.utils.core.ModuleContext;
import dev.onlydarkness.utils.modules.events.LogEvent;

public class DatabaseModule implements IModule {
    private DatabaseManager databaseManager;

    @Override
    public String getName() {
        return "DatabaseModule";
    }

    @Override
    public void onEnable(ModuleContext context) {
        String host = context.getConfigString("db-host", "localhost");
        int port = context.getConfigInt("db-port", 3306);
        String dbName = context.getConfigString("db-name", "cardinal_db");
        String user = context.getConfigString("db-user", "root");
        String pass = context.getConfigString("db-pass", "");

        try {
            databaseManager = new DatabaseManager(host, dbName, user, pass, port);

            context.registerService(DatabaseManager.class, databaseManager);
            context.publishEvent("SYSTEM_LOG", new LogEvent(LogEvent.Level.INFO, getName(), "MySQL Service started."));
        }catch (Exception e) {
            context.publishEvent("SYSTEM_LOG", new LogEvent(LogEvent.Level.ERROR, getName(), "Failed to start MySQL: " + e.getMessage()));
            e.printStackTrace();
        }
    }

    @Override
    public void onDisable() {
        if (databaseManager != null) {
            databaseManager.shutdown();
            System.out.println("[Database] MySQL Connection closed.");
        }
    }
}
