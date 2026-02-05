package dev.onlydarkness.utils.modules;

import dev.onlydarkness.utils.core.IModule;
import dev.onlydarkness.utils.core.ModuleContext;
import dev.onlydarkness.utils.modules.events.LogEvent;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class DailyBackupModule implements IModule {

    // NOT: Artık "static final" ayarlar yok. Hepsi Config'den okunacak.

    @Override
    public String getName() {
        return "DailyBackup";
    }

    @Override
    public void onEnable(ModuleContext context)  {


        boolean isEnabled = context.getConfigBoolean("backup-enabled", true);
        if (!isEnabled) {
            context.publishEvent("SYSTEM_LOG", new LogEvent(LogEvent.Level.INFO, getName(), "Module is disabled in configuration."));
            return;
        }

        String backupFolderName = context.getConfigString("backup-path", "backups");


        String filesConfig = context.getConfigString("files-to-backup", "modules,server.properties,logs");
        List<String> filesToBackup = Arrays.stream(filesConfig.split(","))
                .map(String::trim) // Boşlukları sil (örn: " logs" -> "logs")
                .collect(Collectors.toList());
        // ------------------------------------------------------------

        // Tarih Formatı
        String todayDate = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
        String zipFileName = "backup-" + todayDate + ".zip";

        // Klasör ve Dosya Hazırlığı
        File backupDir = new File(backupFolderName);
        if (!backupDir.exists()) backupDir.mkdirs();

        File targetFile = new File(backupDir, zipFileName);

        // Kontrol: Bugün yedek alındı mı?
        if (targetFile.exists()) {
            context.publishEvent("SYSTEM_LOG", new LogEvent(LogEvent.Level.INFO, getName(), "Backup already exists for today. Skipping."));
            return;
        }

        // Yedeklemeyi Başlat (Parametreleri gönderiyoruz)
        startAsyncBackup(context, targetFile, backupFolderName, filesToBackup);
    }

    @Override
    public void onDisable() {}

    private void startAsyncBackup(ModuleContext context, File targetZip, String backupFolderName, List<String> filesToBackup) {
        new Thread(() -> {
            context.publishEvent("SYSTEM_LOG", new LogEvent(LogEvent.Level.WARN, getName(), "Starting backup process..."));

            // Backup klasörünün tam yolunu al (Kendini yedeklememek için filtrede kullanacağız)
            Path backupPathAbs = new File(backupFolderName).toPath().toAbsolutePath();

            try (FileOutputStream fos = new FileOutputStream(targetZip);
                 ZipOutputStream zos = new ZipOutputStream(fos)) {

                for (String pathName : filesToBackup) {
                    if (pathName.isEmpty()) continue; // Boş virgül varsa atla

                    File sourceFile = new File(pathName);

                    if (!sourceFile.exists()) {
                        System.out.println("[BackupDebug] Skipped (Not Found): " + pathName);
                        continue;
                    }

                    if (sourceFile.isDirectory()) {
                        zipDirectory(sourceFile, zos, backupPathAbs);
                    } else {
                        zipSingleFile(sourceFile, zos);
                    }
                }

                context.publishEvent("SYSTEM_LOG", new LogEvent(LogEvent.Level.INFO, getName(), "Backup completed: " + targetZip.getName()));

            } catch (Exception e) {
                context.publishEvent("SYSTEM_LOG", new LogEvent(LogEvent.Level.ERROR, getName(), "Backup Failed: " + e.getMessage()));
                e.printStackTrace();
            }
        }).start();
    }

    private void zipDirectory(File folderToZip, ZipOutputStream zos, Path backupPathAbs) throws IOException {
        Path sourcePath = folderToZip.toPath();
        Path parentPath = sourcePath.getParent() == null ? Paths.get(".") : sourcePath.getParent();

        Files.walkFileTree(sourcePath, new SimpleFileVisitor<Path>() {

            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                // Filtreleme: Backup klasörünün kendisi veya target klasörü
                if (dir.toAbsolutePath().startsWith(backupPathAbs) || dir.toString().contains("target")) {
                    return FileVisitResult.SKIP_SUBTREE;
                }

                System.out.println("[BackupDebug] Entering folder: " + dir.getFileName());

                // Klasör yapısını korumak için boş klasör girdisi ekle
                String zipEntryPath = parentPath.relativize(dir).toString().replace("\\", "/") + "/";

                if (!zipEntryPath.equals("./")) {
                    try {
                        zos.putNextEntry(new ZipEntry(zipEntryPath));
                        zos.closeEntry();
                    } catch (Exception e) {
                        // Root hatası olursa yoksay
                    }
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {

                if (file.toAbsolutePath().startsWith(backupPathAbs)) return FileVisitResult.CONTINUE;
                String fileName = file.getFileName().toString();
                if (file.toString().contains("target") || fileName.startsWith(".")) return FileVisitResult.CONTINUE;

                try {
                    String zipEntryPath = parentPath.relativize(file).toString().replace("\\", "/");
                    System.out.println("[BackupDebug] Zipping: " + zipEntryPath);

                    zos.putNextEntry(new ZipEntry(zipEntryPath));


                    try (FileInputStream fis = new FileInputStream(file.toFile())) {
                        byte[] buffer = new byte[1024];
                        int len;
                        while ((len = fis.read(buffer)) > 0) {
                            zos.write(buffer, 0, len);
                        }
                    }
                    zos.closeEntry();
                } catch (Exception e) {
                    System.err.println("[BackupDebug] Error zipping file " + file + ": " + e.getMessage());
                }

                return FileVisitResult.CONTINUE;
            }
        });
    }

    private void zipSingleFile(File fileToZip, ZipOutputStream zos) throws IOException {
        System.out.println("[BackupDebug] Zipping File: " + fileToZip.getName());
        ZipEntry zipEntry = new ZipEntry(fileToZip.getName());
        zos.putNextEntry(zipEntry);

        try (FileInputStream fis = new FileInputStream(fileToZip)) {
            byte[] buffer = new byte[1024];
            int length;
            while ((length = fis.read(buffer)) >= 0) {
                zos.write(buffer, 0, length);
            }
        }
        zos.closeEntry();
    }
}