package edu.tongji.versiontracker;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VirtualFile;

/**
 * 插件的核心服务类，负责注册和移除插件相关监听器，并且可以维护插件的状态信息。
 * 这里使用轻服务类，因此未在 plugin.xml 中注册。
 */
@Service
public final class VersionTrackerService {
    private final Project project;
    private final Path versionDirectory;
    private final List<VersionInfo> versionHistory = new ArrayList<>();

    public VersionTrackerService(Project project) {
        this.project = project;
        this.versionDirectory = Paths.get(project.getBasePath(), ".version_tracker");
        createVersionDirectory();
    }

    private void createVersionDirectory() {
        ApplicationManager.getApplication().runWriteAction(() -> {
            try {
                Files.createDirectories(versionDirectory);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    public void trackChange(VirtualFile file) {
        if (file == null || !file.isValid() || file.isDirectory()) return;

        ApplicationManager.getApplication().runReadAction(() -> {
            try {
                String content = new String(file.contentsToByteArray());
                saveVersion(file.getPath(), content);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    private void saveVersion(String filePath, String content) throws IOException {
        String relativePath = FileUtil.getRelativePath(project.getBasePath(), filePath, File.separatorChar);
        if (relativePath == null) return;

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String safeFileName = timestamp + "_" + relativePath.replace(File.separatorChar, '_').replaceAll("[^a-zA-Z0-9.-]", "_");
        Path versionFile = versionDirectory.resolve(safeFileName);

        Files.write(versionFile, content.getBytes());
        versionHistory.add(new VersionInfo(timestamp, relativePath, versionFile.toString()));
    }

    public List<VersionInfo> getVersionHistory() {
        return new ArrayList<>(versionHistory);
    }

    public static class VersionInfo {
        public final String timestamp;
        public final String filePath;
        public final String versionFilePath;

        VersionInfo(String timestamp, String filePath, String versionFilePath) {
            this.timestamp = timestamp;
            this.filePath = filePath;
            this.versionFilePath = versionFilePath;
        }
    }
}
