package dev.onlydarkness.utils.modules;

import dev.onlydarkness.utils.core.IModule;
import dev.onlydarkness.utils.core.ModuleContext;
import dev.onlydarkness.utils.modules.events.LogEvent;

public class ConsoleLoggerModule implements IModule {

    @Override
    public String getName() {
        return "ConsoleLogger";
    }

    @Override
    public void onEnable(ModuleContext ctx) {
        System.out.println("ConsoleLoggerModule started -> Listening loggerModule");

        ctx.subscribe("SYSTEM_LOG", LogEvent.class, this::handleLog);
    }

    @Override
    public void onDisable() {
        System.out.println("ConsoleLoggerModule stopped");
    }

    private void handleLog(LogEvent event) {
        String color = "";
        String reset = "\u001B[0m";

        switch (event.getLevel()) {
            case ERROR: color = "\u001B[31m"; break; // Kırmızı
            case WARN:  color = "\u001B[33m"; break; // Sarı
            case INFO:  color = "\u001B[32m"; break; // Yeşil
            case DEBUG: color = "\u001B[36m"; break; // Mavi
            case FATAL: color = "\u001B[35m"; break;
        }

        System.out.println(color + event.toString() + reset);
    }
}
