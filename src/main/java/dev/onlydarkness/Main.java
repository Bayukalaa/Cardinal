package dev.onlydarkness;

import dev.onlydarkness.utils.ModuleManager;
import dev.onlydarkness.utils.SystemMonitor;
import dev.onlydarkness.utils.ai.AIService;
import org.fusesource.jansi.AnsiConsole;

import java.util.Scanner;

public class Main {

    private static volatile boolean isRunning = true;
    private static SystemMonitor systemMonitor;

    public static void main(String[] args) {

        AnsiConsole.systemInstall();
        System.out.println("[CARDINAL] System initializing...");


        AIService.getInstance();

        ModuleManager moduleManager = new ModuleManager();
        moduleManager.load();

        systemMonitor = new SystemMonitor();
        systemMonitor.start();


        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            isRunning = false;
            if (systemMonitor != null) systemMonitor.shutdown();
            moduleManager.unload();
            AnsiConsole.systemUninstall();
            System.out.println("\n[CARDINAL] System shutdown complete.");
        }));

        System.out.println("[CARDINAL] System loaded. Type 'help' for commands.");


        try (Scanner scanner = new Scanner(System.in)) {
            while (isRunning) {
                System.out.print("> ");
                if (scanner.hasNextLine()) {
                    String input = scanner.nextLine().trim();
                    if (input.isEmpty()) continue;


                    if (input.equalsIgnoreCase("ai-status")) {
                        System.out.println("AI Modeli Aktif mi: " + (AIService.getInstance() != null));

                        int result = AIService.getInstance().analyzeSystem(100, 50, 50, 50, 50);
                        System.out.println("Test Analiz Sonucu (Kod): " + result);
                    }

                    else if (input.equalsIgnoreCase("stop") || input.equalsIgnoreCase("exit")) {
                        System.exit(0);
                    }
                    else {
                        moduleManager.dispatchCommand(input);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}