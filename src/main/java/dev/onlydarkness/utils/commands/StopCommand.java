package dev.onlydarkness.utils.commands;

import dev.onlydarkness.utils.core.ICommand;

public class StopCommand implements ICommand {

    @Override
    public String getName() {
        return "stop";
    }

    @Override
    public String getDescription() {
        return "Stops the system.";
    }

    @Override
    public void execute(String[] args) {
        System.out.println("[CARDINAL] Stopping system via command...");
        System.exit(0);
    }
}