package dev.onlydarkness.utils;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import dev.onlydarkness.utils.ai.AIService;
import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.GlobalMemory;
import oshi.software.os.OperatingSystem;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class ApiHandler implements HttpHandler {

    private final SystemInfo systemInfo = new SystemInfo();

    private long[] oldTicks;

    public ApiHandler() {

        this.oldTicks = systemInfo.getHardware().getProcessor().getSystemCpuLoadTicks();
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {

        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");

        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();

        String response = "{}";

        try {
            if ("GET".equals(method)) {
                if (path.equals("/api/system")) {
                    response = getSystemStats();
                } else if (path.equals("/api/ai")) {
                    response = getAiPrediction();
                } else {
                    response = "{\"error\": \"Endpoint not found\"}";
                }
            }
            sendResponse(exchange, 200, response);
        } catch (Exception e) {
            e.printStackTrace();
            sendResponse(exchange, 500, "{\"error\": \"" + e.getMessage() + "\"}");
        }
    }

    private void sendResponse(HttpExchange exchange, int status, String response) throws IOException {
        byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private String getSystemStats() {
        CentralProcessor cpu = systemInfo.getHardware().getProcessor();
        GlobalMemory mem = systemInfo.getHardware().getMemory();
        OperatingSystem os = systemInfo.getOperatingSystem();


        double cpuLoad = cpu.getSystemCpuLoadBetweenTicks(oldTicks) * 100;


        oldTicks = cpu.getSystemCpuLoadTicks();


        double ramUsage = (1 - (double) mem.getAvailable() / mem.getTotal()) * 100;




        long seconds = os.getSystemUptime();
        long days = seconds / 86400;
        long hours = (seconds % 86400) / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;

        String uptimeFormatted = String.format("%dD/%dH/%dM/%ds", days, hours, minutes, secs);

        return String.format(
                "{\"cpu\": %.2f, \"ram\": %.2f, \"uptime\": %d}",
                cpuLoad, ramUsage, uptimeFormatted
        );
    }

    private String getAiPrediction() {
        CentralProcessor cpu = systemInfo.getHardware().getProcessor();
        GlobalMemory mem = systemInfo.getHardware().getMemory();


        double cpuLoad = cpu.getSystemCpuLoadBetweenTicks(oldTicks) * 100;
        double ramUsage = (1 - (double) mem.getAvailable() / mem.getTotal()) * 100;


        int prediction = AIService.getInstance().analyzeSystem(150, ramUsage, cpuLoad, 50, 100);

        String status = switch (prediction) {
            case 0 -> "STABIL";
            case 1 -> "UYARI";
            case 2 -> "KRITIK";
            default -> "UNKNOWN";
        };

        return String.format("{\"status\": \"%s\", \"code\": %d}", status, prediction);
    }
}