package dev.onlydarkness.utils.commands;

import dev.onlydarkness.utils.ModuleManager;
import dev.onlydarkness.utils.core.ICommand;

public class ReloadCommand implements ICommand {

    private final ModuleManager moduleManager;

    public ReloadCommand(ModuleManager moduleManager) {
        this.moduleManager = moduleManager;
    }

    @Override
    public String getName() {
        return "reload";
    }

    @Override
    public String getDescription() {
        return "Reloads server.properties without restarting modules";
    }

    @Override
    public void execute(String[] args) {
        System.out.println("[Command] Reloading server properties...");
        moduleManager.reloadConfiguration();
    }
}
