package dev.onlydarkness.utils.commands;

import dev.onlydarkness.utils.core.ICommand;
import dev.onlydarkness.utils.modules.WebServerModule;

public class WebServerCommand implements ICommand {

    private final WebServerModule module;

    public WebServerCommand(WebServerModule module) {
        this.module = module;
    }

    @Override
    public String getName() {
        return "webserver";
    }

    @Override
    public String getDescription() {
        return "Controls the web server. Usage: webserver <start|stop|status>";
    }

    @Override
    public void execute(String[] args) {
        if (args.length == 0) {
            System.out.println("Usage: webserver <start|stop|status>");
            return;
        }

        String action = args[0].toLowerCase();

        switch (action) {
            case "start":
                if (module.isRunning()) {
                    System.out.println("Web Server is already running!");
                } else {
                    module.startServer();
                }
                break;

            case "stop":
                if (!module.isRunning()) {
                    System.out.println("Web Server is not running.");
                } else {
                    module.stopServer();
                }
                break;

            case "status":
                System.out.println("Web Server Status: " + (module.isRunning() ? "\u001B[32mRUNNING\u001B[0m" : "\u001B[31mSTOPPED\u001B[0m"));
                break;

            default:
                System.out.println("Unknown action: " + action);
                break;
        }
    }
}