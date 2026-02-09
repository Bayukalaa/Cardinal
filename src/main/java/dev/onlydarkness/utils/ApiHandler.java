package dev.onlydarkness.utils;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import dev.onlydarkness.utils.ai.AIService;
import org.json.JSONObject;
import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.GlobalMemory;
import oshi.software.os.OperatingSystem;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class ApiHandler implements HttpHandler {

    private final SystemInfo systemInfo = new SystemInfo();
    private final DatabaseManager dbManager;
    private long[] oldTicks;

    public ApiHandler(DatabaseManager dbManager) {
        this.dbManager = dbManager;
        this.oldTicks = systemInfo.getHardware().getProcessor().getSystemCpuLoadTicks();
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {

        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");

        if ("OPTIONS".equals(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }

        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();

        try {
            if ("GET".equalsIgnoreCase(method)) {
                routeGetRequests(exchange, path);
            } else if ("POST".equalsIgnoreCase(method)) {
                routePostRequests(exchange, path);
            } else {
                sendResponse(exchange, 405, "{\"error\": \"Method Not Allowed\"}");
            }
        } catch (Exception e) {
            e.printStackTrace();
            sendResponse(exchange, 500, "{\"error\": \"Internal Core Failure: " + e.getMessage() + "\"}");
        }
    }

    private void routeGetRequests(HttpExchange exchange, String path) throws IOException {
        if (path.endsWith("/system")) {
            sendResponse(exchange, 200, getSystemStats());
        } else if (path.endsWith("/ai")) {
            sendResponse(exchange, 200, getAiPrediction());
        } else {
            sendResponse(exchange, 404, "{\"error\": \"Endpoint Not Found\", \"path\": \"" + path + "\"}");
        }
    }

    private void routePostRequests(HttpExchange exchange, String path) throws IOException {
        if (path.endsWith("/auth/login")) {
            handleLogin(exchange);
        } else if (path.endsWith("/auth/register")) {
            handleRegister(exchange);
        } else {
            sendResponse(exchange, 404, "{\"error\": \"Post Endpoint Not Found\", \"path\": \"" + path + "\"}");
        }
    }

    private void handleLogin(HttpExchange exchange) throws IOException {
        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        JSONObject json = new JSONObject(body);

        String username = json.optString("username");
        String password = json.optString("password");

        Map<String, Object> operator = dbManager.authenticate(username, password);

        if (operator != null) {
            JSONObject response = new JSONObject();
            response.put("success", true);
            response.put("level", operator.get("clearance_level"));
            response.put("operator", operator.get("first_name") + " " + operator.get("last_name"));
            sendResponse(exchange, 200, response.toString());
        } else {
            sendResponse(exchange, 401, "{\"success\": false, \"message\": \"Invalid clearance credentials.\"}");
        }
    }

    private void handleRegister(HttpExchange exchange) throws IOException {
        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        JSONObject json = new JSONObject(body);

        boolean registered = dbManager.registerOperator(
                json.optString("fname"),
                json.optString("lname"),
                json.optString("email"),
                json.optString("username"),
                json.optString("password")
        );

        JSONObject response = new JSONObject();
        response.put("success", registered);
        response.put("message", registered ? "Registration authorized." : "Registration denied - Identity exists.");
        sendResponse(exchange, registered ? 200 : 400, response.toString());
    }

    private String getSystemStats() {
        CentralProcessor cpu = systemInfo.getHardware().getProcessor();
        GlobalMemory mem = systemInfo.getHardware().getMemory();
        OperatingSystem os = systemInfo.getOperatingSystem();

        double cpuLoad = cpu.getSystemCpuLoadBetweenTicks(oldTicks) * 100;
        oldTicks = cpu.getSystemCpuLoadTicks();
        double ramUsage = (1 - (double) mem.getAvailable() / mem.getTotal()) * 100;

        long seconds = os.getSystemUptime();
        String uptime = String.format("%dD/%dH/%dM/%ds",
                seconds / 86400, (seconds % 86400) / 3600, (seconds % 3600) / 60, seconds % 60);

        return String.format("{\"cpu\": %.2f, \"ram\": %.2f, \"uptime\": \"%s\"}", cpuLoad, ramUsage, uptime);
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

    private void sendResponse(HttpExchange exchange, int status, String response) throws IOException {
        byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
            os.flush();
        }
    }
}