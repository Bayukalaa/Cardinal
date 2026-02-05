package dev.onlydarkness.utils;

import dev.onlydarkness.utils.ai.AIService;
import oshi.json.SystemInfo;
import oshi.json.hardware.CentralProcessor;
import oshi.json.hardware.GlobalMemory;
import oshi.json.hardware.HardwareAbstractionLayer;

import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class SystemMonitor extends Thread {
    private boolean running = true;
    private final SystemInfo systemInfo = new SystemInfo();
    private final HardwareAbstractionLayer hardware = systemInfo.getHardware();


    private long[] oldTicks;

    private static final int UI_PORT = 5000;

    public SystemMonitor() {

        this.oldTicks = hardware.getProcessor().getSystemCpuLoadTicks();
    }

    @Override
    public void run() {
        System.out.println("[MONITOR] Sistem izleme ve AI servisi başlatıldı (Port: " + UI_PORT + ")");


        try (ServerSocket serverSocket = new ServerSocket(UI_PORT)) {
            while (running) {
                try {

                    Socket client = serverSocket.accept();
                    handleClient(client);
                } catch (Exception e) {
                    if (running) System.err.println("[MONITOR] Bağlantı hatası: " + e.getMessage());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleClient(Socket client) {
        try (PrintWriter out = new PrintWriter(client.getOutputStream(), true)) {

            CentralProcessor cpu = systemInfo.getHardware().getProcessor();
            GlobalMemory mem = systemInfo.getHardware().getMemory();

            double cpuLoad = cpu.getSystemCpuLoad() * 100;
            double ramUsage = (1 - (double) mem.getAvailable() / mem.getTotal()) * 100;
            double diskUsage = 50.0;
            double networkUsage = 100.0;
            int activeUsers = 150;


            int aiResult = AIService.getInstance().analyzeSystem(activeUsers, ramUsage, cpuLoad, diskUsage, networkUsage);

            String statusText = switch (aiResult) {
                case 0 -> "STABIL";
                case 1 -> "UYARI";
                case 2 -> "KRITIK";
                default -> "BILINMIYOR";
            };


            String response = String.format("%.1f|%.1f|%d|%s", cpuLoad, ramUsage, activeUsers, statusText);
            out.println(response);

            client.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void shutdown() {
        this.running = false;

    }
}
