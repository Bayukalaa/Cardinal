package dev.onlydarkness.utils;

import dev.onlydarkness.utils.core.ICommand;
import java.util.HashMap;
import java.util.Map;

public class CommandManager {

    private final Map<String, ICommand> commands = new HashMap<>();

    public void register(ICommand command) {
        commands.put(command.getName().toLowerCase(), command);
    }

    public void dispatch(String input) {

        String[] parts = input.trim().split("\\s+");
        if (parts.length == 0) return;

        String commandName = parts[0].toLowerCase();


        String[] args = new String[parts.length - 1];
        System.arraycopy(parts, 1, args, 0, args.length);

        if (commands.containsKey(commandName)) {
            try {
                commands.get(commandName).execute(args);
            } catch (Exception e) {
                System.err.println("[CommandManager] Error executing '" + commandName + "': " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            System.out.println("Unknown command: '" + commandName + "'. Type 'help' for list.");
        }
    }

    public Map<String, ICommand> getCommands() {
        return commands;
    }


    public void showHelp() {
        System.out.println("--- Available Commands ---");
        for (ICommand cmd : commands.values()) {
            System.out.println(String.format(" %-10s : %s", cmd.getName(), cmd.getDescription()));
        }
        System.out.println("--------------------------");
    }
}