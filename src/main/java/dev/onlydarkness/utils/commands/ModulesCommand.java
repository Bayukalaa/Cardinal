package dev.onlydarkness.utils.commands;

import dev.onlydarkness.utils.ModuleManager;
import dev.onlydarkness.utils.core.ICommand;
import dev.onlydarkness.utils.core.IModule;

import java.util.Map;

public class ModulesCommand implements ICommand {

    private final ModuleManager moduleManager;


    private static final String GREEN = "\u001B[32m";
    private static final String RED = "\u001B[31m";
    private static final String RESET = "\u001B[0m";
    private static final String CYAN = "\u001B[36m";

    public ModulesCommand(ModuleManager moduleManager) {
        this.moduleManager = moduleManager;
    }

    @Override
    public String getName() {
        return "modules";
    }

    @Override
    public String getDescription() {
        return "Lists all active and failed modules.";
    }

    @Override
    public void execute(String[] args) {
        System.out.println(CYAN + "--- Module Status Report ---" + RESET);

        var loaded = moduleManager.getLoadedModules();
        var failed = moduleManager.getFailedModules();

        if (loaded.isEmpty() && failed.isEmpty()) {
            System.out.println("No modules found.");
        } else {

            for (IModule module : loaded) {
                System.out.println(GREEN + "[+] " + module.getName() + " (Active)" + RESET);
            }

            for (Map.Entry<String, String> entry : failed.entrySet()) {
                System.out.println(RED + "[-] " + entry.getKey() + " (Failed: " + entry.getValue() + ")" + RESET);
            }
        }

        System.out.println(CYAN + "----------------------------" + RESET);
        System.out.println("Total Active: " + loaded.size() + " | Total Failed: " + failed.size());
    }
}