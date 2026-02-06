package dev.onlydarkness.utils.modules;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import dev.onlydarkness.utils.ApiHandler;
import dev.onlydarkness.utils.commands.WebServerCommand;
import dev.onlydarkness.utils.core.IModule;
import dev.onlydarkness.utils.core.ModuleContext;
import dev.onlydarkness.utils.modules.events.LogEvent;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

public class WebServerModule implements IModule {

    private HttpServer server;
    private ModuleContext context;
    private boolean isRunning = false;
    private String webRoot;
    private String phpCgiPath;

    @Override
    public String getName() {
        return "WebServer";
    }

    @Override
    public void onEnable(ModuleContext context) {
        this.context = context;
        context.registerCommand(new WebServerCommand(this));

        boolean autoStart = context.getConfigBoolean("auto-run-webserver", false);


        this.phpCgiPath = context.getConfigString("php-cgi-path", "php-cgi");

        if (autoStart) {
            startServer();
        } else {
            System.out.println("[WebServer] Module loaded. Type 'webserver start' to launch.");
        }
    }

    @Override
    public void onDisable() {
        stopServer();
    }

    public boolean isRunning() {
        return isRunning;
    }

    public void startServer() {
        if (isRunning) return;

        int port = context.getConfigInt("web-port", 8080);
        this.webRoot = context.getConfigString("web-root", "web");

        File webDir = new File(webRoot);
        if (!webDir.exists()) {
            webDir.mkdirs();
            createDefaultIndexHtml(webDir);
        }

        try {
            server = HttpServer.create(new InetSocketAddress(port), 0);

            server.createContext("/", new DynamicFileHandler());
            server.setExecutor(Executors.newCachedThreadPool());
            server.start();

            isRunning = true;
            context.publishEvent("SYSTEM_LOG", new LogEvent(LogEvent.Level.INFO, getName(), "Web Server started on port " + port));
            server.createContext("/api", new ApiHandler());
            System.out.println("[WebServer] Server is running on port " + port);

        } catch (IOException e) {
            context.publishEvent("SYSTEM_LOG", new LogEvent(LogEvent.Level.ERROR, getName(), "Failed to start: " + e.getMessage()));
            e.printStackTrace();
        }
    }

    public void stopServer() {
        if (server != null && isRunning) {
            server.stop(0);
            server = null;
            isRunning = false;
            context.publishEvent("SYSTEM_LOG", new LogEvent(LogEvent.Level.WARN, getName(), "Web Server stopped."));
        }
    }


    class DynamicFileHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String requestPath = exchange.getRequestURI().getPath();

            if (requestPath.endsWith("/")) {
                File indexPhp = new File(webRoot + requestPath + "index.php");
                File indexHtml = new File(webRoot + requestPath + "index.html");

                if (indexPhp.exists()) requestPath += "index.php";
                else if (indexHtml.exists()) requestPath += "index.html";
                else requestPath += "index.html";
            }

            File file = new File(webRoot + requestPath);

            if (!file.exists() || file.isDirectory()) {
                send404(exchange);
                return;
            }


            if (file.getName().endsWith(".php")) {
                handlePhpRequest(exchange, file);
            } else {

                serveStaticFile(exchange, file);
            }
        }
    }



    private void handlePhpRequest(HttpExchange exchange, File phpFile) throws IOException {
        try {
            ProcessBuilder pb = new ProcessBuilder(phpCgiPath);
            Map<String, String> env = pb.environment();

            String query = exchange.getRequestURI().getRawQuery();
            String contentType = exchange.getRequestHeaders().getFirst("Content-Type");
            String contentLength = String.valueOf(exchange.getRequestBody().available());

            env.put("GATEWAY_INTERFACE", "CGI/1.1");
            env.put("SERVER_PROTOCOL", "HTTP/1.1");
            env.put("REDIRECT_STATUS", "200");
            env.put("REQUEST_METHOD", exchange.getRequestMethod());
            env.put("SCRIPT_FILENAME", phpFile.getAbsolutePath());


            env.put("QUERY_STRING", query != null ? query : "");
            env.put("CONTENT_TYPE", contentType != null ? contentType : "");
            env.put("CONTENT_LENGTH", contentLength != null ? contentLength : "0");

            Process process = pb.start();


            if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                try (OutputStream os = process.getOutputStream()) {
                    exchange.getRequestBody().transferTo(os);
                }
            }


            ByteArrayOutputStream outputBuffer = new ByteArrayOutputStream();
            try (InputStream is = process.getInputStream()) {
                is.transferTo(outputBuffer);
            }

            byte[] outputBytes = outputBuffer.toByteArray();


            int splitIndex = findHeaderEnd(outputBytes);

            if (splitIndex == -1) {

                exchange.sendResponseHeaders(200, outputBytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(outputBytes);
                }
            } else {

                String headerSection = new String(outputBytes, 0, splitIndex, StandardCharsets.UTF_8);

                byte[] bodySection = new byte[outputBytes.length - splitIndex - 4];
                System.arraycopy(outputBytes, splitIndex + 4, bodySection, 0, bodySection.length);


                String[] lines = headerSection.split("\r\n");
                for (String line : lines) {
                    if (line.contains(":")) {
                        String[] parts = line.split(":", 2);
                        exchange.getResponseHeaders().add(parts[0].trim(), parts[1].trim());
                    }
                }

                exchange.sendResponseHeaders(200, bodySection.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(bodySection);
                }
            }

        } catch (Exception e) {

            String errorMsg = "<h1>500 - PHP Execution Error</h1><pre>" + e.toString() + "</pre>";
            exchange.sendResponseHeaders(500, errorMsg.length());
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(errorMsg.getBytes());
            }
            e.printStackTrace();
        }
    }

    private int findHeaderEnd(byte[] bytes) {
        for (int i = 0; i < bytes.length - 3; i++) {
            if (bytes[i] == '\r' && bytes[i+1] == '\n' && bytes[i+2] == '\r' && bytes[i+3] == '\n') {
                return i;
            }
        }
        return -1;
    }

    private void serveStaticFile(HttpExchange exchange, File file) throws IOException {
        String mimeType = getMimeType(file.getName());
        exchange.getResponseHeaders().set("Content-Type", mimeType);
        exchange.sendResponseHeaders(200, file.length());

        try (OutputStream os = exchange.getResponseBody();
             FileInputStream fs = new FileInputStream(file)) {
            byte[] buffer = new byte[1024];
            int count;
            while ((count = fs.read(buffer)) >= 0) {
                os.write(buffer, 0, count);
            }
        }
    }

    private void send404(HttpExchange exchange) throws IOException {
        String response = "<h1>404 - File Not Found</h1><p>Cardinal WebServer</p>";
        exchange.sendResponseHeaders(404, response.length());
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(response.getBytes());
        }
    }

    private String getMimeType(String fileName) {
        if (fileName.endsWith(".html")) return "text/html";
        if (fileName.endsWith(".css")) return "text/css";
        if (fileName.endsWith(".js")) return "application/javascript";
        if (fileName.endsWith(".png")) return "image/png";
        if (fileName.endsWith(".jpg")) return "image/jpeg";
        if (fileName.endsWith(".php")) return "text/html";
        return "text/plain";
    }

    private void createDefaultIndexHtml(File folder) {
        File index = new File(folder, "index.php");
        String content = """
                <?php
                echo "<h1>Cardinal WebServer</h1>";
                echo "<p>PHP is working! Date: " . date('Y-m-d H:i:s') . "</p>";
                ?>
                """;
        try { Files.writeString(index.toPath(), content); } catch (IOException ignored) {}
    }
}