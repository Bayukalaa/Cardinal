package dev.onlydarkness.utils.modules;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import dev.onlydarkness.utils.commands.WebServerCommand;
import dev.onlydarkness.utils.core.IModule;
import dev.onlydarkness.utils.core.ModuleContext;
import dev.onlydarkness.utils.modules.events.LogEvent;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.util.concurrent.Executors;

public class WebServerModule implements IModule {

    private HttpServer server;
    private ModuleContext context;
    private boolean isRunning = false;
    private String webRoot;

    @Override
    public String getName() {
        return "WebServer";
    }

    @Override
    public void onEnable(ModuleContext context) {
        this.context = context;

        // Komutu kaydet
        context.registerCommand(new WebServerCommand(this));

        // --- AUTO-RUN KONTROLÜ ---
        // Config'den değeri oku, bulamazsa varsayılan olarak 'false' kabul et.
        boolean autoStart = context.getConfigBoolean("auto-run-webserver", false);

        if (autoStart) {
            System.out.println("[WebServer] Auto-run setting is enabled. Starting server...");
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

    // --- Sunucu Yönetimi ---

    public void startServer() {
        if (isRunning) return;

        // Config Okuma
        int port = context.getConfigInt("web-port", 8080);
        this.webRoot = context.getConfigString("web-root", "web");

        // Web klasör kontrolü
        File webDir = new File(webRoot);
        if (!webDir.exists()) {
            webDir.mkdirs();
            createDefaultIndexHtml(webDir);
        }

        try {
            server = HttpServer.create(new InetSocketAddress(port), 0);
            server.createContext("/", new StaticFileHandler());
            server.setExecutor(Executors.newCachedThreadPool());
            server.start();

            isRunning = true;
            context.publishEvent("SYSTEM_LOG", new LogEvent(
                    LogEvent.Level.INFO, getName(),
                    "Web Server started on port " + port
            ));

            // Konsola da bilgi verelim
            System.out.println("[WebServer] Server is running on port " + port);

        } catch (IOException e) {
            context.publishEvent("SYSTEM_LOG", new LogEvent(
                    LogEvent.Level.ERROR, getName(),
                    "Failed to start: " + e.getMessage()
            ));
            e.printStackTrace();
        }
    }

    public void stopServer() {
        if (server != null && isRunning) {
            server.stop(0);
            server = null;
            isRunning = false;

            context.publishEvent("SYSTEM_LOG", new LogEvent(
                    LogEvent.Level.WARN, getName(),
                    "Web Server stopped."
            ));
        }
    }

    // --- Dosya İşleyici (Handler) ---
    class StaticFileHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String requestPath = exchange.getRequestURI().getPath();
            if (requestPath.equals("/")) {
                requestPath = "/index.html";
            }

            File file = new File(webRoot + requestPath);

            if (file.exists() && !file.isDirectory()) {
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
            } else {
                String response = "<h1>404 - File Not Found</h1><p>Cardinal WebServer</p>";
                exchange.sendResponseHeaders(404, response.length());
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response.getBytes());
                }
            }
        }
    }

    private String getMimeType(String fileName) {
        if (fileName.endsWith(".html")) return "text/html";
        if (fileName.endsWith(".css")) return "text/css";
        if (fileName.endsWith(".js")) return "application/javascript";
        if (fileName.endsWith(".png")) return "image/png";
        if (fileName.endsWith(".jpg")) return "image/jpeg";
        return "text/plain";
    }

    private void createDefaultIndexHtml(File folder) {
        File index = new File(folder, "index.html");
        String content = """
                <!DOCTYPE html>
                <html>
                <head><title>Cardinal</title></head>
                <body><h1>WebServer is Running!</h1></body>
                </html>
                """;
        try { Files.writeString(index.toPath(), content); } catch (IOException ignored) {}
    }
}