package dev.onlydarkness.utils.commands;

import dev.onlydarkness.utils.ModuleManager;
import dev.onlydarkness.utils.core.ICommand;

public class HardReloadCommand implements ICommand {

    private final ModuleManager moduleManager;

    public HardReloadCommand(ModuleManager moduleManager) {
        this.moduleManager = moduleManager;
    }

    @Override
    public String getName() {
        return "hardreload";
    }

    @Override
    public String getDescription() {
        return "Unloads and reloads all modules and config files (Full Restart).";
    }

    @Override
    public void execute(String[] args) {
        System.out.println("[Command] Initiating hard reload...");

        moduleManager.performHardReload();
    }
}
