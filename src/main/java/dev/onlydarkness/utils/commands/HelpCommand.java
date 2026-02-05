package dev.onlydarkness.utils.commands;

import dev.onlydarkness.utils.CommandManager;
import dev.onlydarkness.utils.core.ICommand;

public class HelpCommand implements ICommand {

    private final CommandManager commandManager;

    public HelpCommand(CommandManager commandManager) {
        this.commandManager = commandManager;
    }

    @Override
    public String getName() {
        return "help";
    }

    @Override
    public String getDescription() {
        return "Shows available commands.";
    }

    @Override
    public void execute(String[] args) {
        System.out.println("--- Available Commands ---");
        for (ICommand cmd : commandManager.getCommands().values()) {
            System.out.println(String.format(" %-10s : %s", cmd.getName(), cmd.getDescription()));
        }
        System.out.println("--------------------------");
    }
}