package dev.onlydarkness.utils.modules;

import dev.onlydarkness.utils.core.IModule;
import dev.onlydarkness.utils.core.ModuleContext;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class FileLoggerModule implements IModule {

    private static final String LOG_FOLDER = "logs";
    private static final String CURRENT_LOG_FILE = "latest.log";

    private PrintStream originalOut;
    private PrintStream originalErr;

    // BufferedWriter'ı sınıf seviyesinde tutuyoruz
    private BufferedWriter fileWriter;

    @Override
    public String getName() {
        return "FileLogger";
    }

    @Override
    public void onEnable(ModuleContext context) {
        if (!context.getConfigBoolean("file-logging-enabled", true)) return;

        File logDir = new File(LOG_FOLDER);
        if (!logDir.exists()) logDir.mkdirs();

        File latestLog = new File(logDir, CURRENT_LOG_FILE);

        if (latestLog.exists()) {
            archiveOldLog(latestLog);
        }

        try {
            fileWriter = new BufferedWriter(new FileWriter(latestLog, true));

            // Hook işlemi
            hookSystemStreams();

            // --- YENİ EKLENEN KISIM: LOG_INPUT dinleyicisi ---
            // Bu event geldiğinde System.out kullanmadan direkt dosyaya yazar.
            context.subscribe("LOG_INPUT", String.class, this::writeDirectlyToFile);
            // --------------------------------------------------

            System.out.println("[FileLogger] Mirroring console output to " + latestLog.getPath());

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onDisable() {
        if (originalOut != null) System.setOut(originalOut);
        if (originalErr != null) System.setErr(originalErr);

        try {
            if (fileWriter != null) {
                fileWriter.write("\n--- SYSTEM SHUTDOWN [" + LocalDateTime.now() + "] ---\n");
                fileWriter.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // --- YENİ METOD: Sadece Dosyaya Yazar ---
    private void writeDirectlyToFile(String message) {
        if (fileWriter == null) return;
        try {
            fileWriter.write(message);
            fileWriter.newLine();
            fileWriter.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void hookSystemStreams() {
        originalOut = System.out;
        originalErr = System.err;
        System.setOut(new DualStream(originalOut, fileWriter));
        System.setErr(new DualStream(originalErr, fileWriter));
    }

    private void archiveOldLog(File oldLogFile) {
        // (Eski kodun aynısı - değişiklik yok)
        if (oldLogFile.length() == 0) {
            oldLogFile.delete();
            return;
        }
        String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
        String archiveName = dateStr + "-log.zip";
        File archiveFile = new File(LOG_FOLDER, archiveName);

        System.out.println("[FileLogger] Archiving old logs -> " + archiveName);

        try (FileOutputStream fos = new FileOutputStream(archiveFile);
             ZipOutputStream zos = new ZipOutputStream(fos);
             FileInputStream fis = new FileInputStream(oldLogFile)) {

            zos.putNextEntry(new ZipEntry(dateStr + ".log"));
            byte[] buffer = new byte[1024];
            int len;
            while ((len = fis.read(buffer)) >= 0) {
                zos.write(buffer, 0, len);
            }
            zos.closeEntry();
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (archiveFile.exists()) oldLogFile.delete();
    }

    private static class DualStream extends PrintStream {
        private final BufferedWriter fileWriter;
        private static final Pattern ANSI_COLOR_PATTERN = Pattern.compile("\u001B\\[[;\\d]*m");

        public DualStream(OutputStream original, BufferedWriter fileWriter) {
            super(original, true);
            this.fileWriter = fileWriter;
        }

        @Override
        public void write(byte[] buf, int off, int len) {
            super.write(buf, off, len);
            try {
                String output = new String(buf, off, len);
                String cleanOutput = ANSI_COLOR_PATTERN.matcher(output).replaceAll("");
                fileWriter.write(cleanOutput);
                fileWriter.flush();
            } catch (IOException ignored) {}
        }

        @Override
        public void write(int b) {
            super.write(b);
            try {
                fileWriter.write(b);
                fileWriter.flush();
            } catch (IOException ignored) {}
        }
    }
}