package edu.tongji.versiontracker;

import com.github.difflib.UnifiedDiffUtils;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.github.difflib.DiffUtils;
import com.github.difflib.patch.Patch;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationGroup;
import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

public class VersionManager {

    private static final Logger LOG = Logger.getInstance(VersionManager.class);
    private static final int INCREMENT_THRESHOLD = 10; // 增量保存次数阈值
    private static final Duration MIN_SAVE_INTERVAL = Duration.ofSeconds(5); // 最小保存间隔

    private final Project project;
    private final Path versionTrackerPath; // .version_tracker 目录路径
    private final Map<String, Integer> fileVersionNumbers; // 文件路径到版本号的映射
    private final Map<String, Integer> fileIncrementCounts; // 文件路径到增量计数的映射
    private final Map<String, List<Increment>> fileIncrements; // 文件路径到增量列表的映射
    private final Map<String, LocalDateTime> lastSaveTime; // 文件路径到上次保存时间的映射
    private final Map<String, String> lastFileContents; // 文件路径到上次内容的映射

    // 获取通知组的实例
    private static final NotificationGroup NOTIFICATION_GROUP = NotificationGroupManager.getInstance()
        .getNotificationGroup("VersionTracker Notifications");

    public VersionManager(Project project) {
        this.project = project;
        this.versionTrackerPath = Paths.get(project.getBasePath(), ".version_tracker");
        this.fileVersionNumbers = new HashMap<>();
        this.fileIncrementCounts = new HashMap<>();
        this.fileIncrements = new HashMap<>();
        this.lastSaveTime = new HashMap<>();
        this.lastFileContents = new HashMap<>();
    }

    // 初始化方法，创建 .version_tracker 文件夹
    public void initialize() {
        try {
            if (!Files.exists(versionTrackerPath)) {
                Files.createDirectories(versionTrackerPath);
                LOG.info(".version_tracker directory created at: " + versionTrackerPath);
            } else {
                LOG.info(".version_tracker directory already exists at: " + versionTrackerPath);
            }
        } catch (IOException e) {
            LOG.error("Failed to create .version_tracker directory", e);
            notifyUser("Failed to create .version_tracker directory: " + e.getMessage());
        }
    }

    // 保存所有打开文件的完整版本（在项目关闭时调用）
    public void saveAllOpenFilesVersion() {
        // 获取所有打开的文件
        VirtualFile[] openFiles = project.getComponent(com.intellij.openapi.fileEditor.FileEditorManager.class).getOpenFiles();
        for (VirtualFile file : openFiles) {
            saveVersion(file);
        }
    }

    // 处理 PsiTree 事件（文件修改、创建、删除）
    public void handlePsiEvent(VirtualFile file) {
        handleEditEvent(file);
    }

    // 处理编辑事件，保存增量
    public void handleEditEvent(VirtualFile file) {
        String filePath = getRelativePath(file);
        LocalDateTime now = LocalDateTime.now();

        // Skip files in .version_tracker
        if (filePath == null) {
            return;
        }

        // 检查最小保存间隔
        if (lastSaveTime.containsKey(filePath)) {
            Duration duration = Duration.between(lastSaveTime.get(filePath), now);
            if (duration.compareTo(MIN_SAVE_INTERVAL) < 0) {
                // 合并增量，不立即保存
                LOG.info("Minimum save interval not reached for file: " + filePath);
                return;
            }
        }

        // 获取当前文件内容
        String currentContent = getFileContent(file);
        if (currentContent == null) {
            LOG.warn("Failed to get content for file: " + filePath);
            return;
        }

        // 获取上一次保存的内容，如果没有，则认为是空字符串
        String lastContent = lastFileContents.getOrDefault(filePath, "");

        // 计算差异
        List<String> originalLines = Arrays.asList(lastContent.split("\n"));
        List<String> revisedLines = Arrays.asList(currentContent.split("\n"));

        Patch<String> patch = DiffUtils.diff(originalLines, revisedLines);

        // 如果没有差异，不保存增量
        if (patch.getDeltas().isEmpty()) {
            LOG.info("No changes detected for file: " + filePath);
            return;
        }

        // 生成统一格式的差异（unified diff）
        List<String> diffLines = UnifiedDiffUtils.generateUnifiedDiff(
            filePath, // 原始文件路径
            filePath, // 修改后的文件路径
            originalLines, // 原始文件内容
            patch, // 差异补丁
            0); // 上下文行数，可以设置为 0 或其他值

        // 将差异行合并为一个字符串
        String diffContent = String.join("\n", diffLines);

        // 创建增量
        Increment increment = new Increment(filePath, diffContent, "修改");

        // 添加到增量列表
        fileIncrements.computeIfAbsent(filePath, k -> new ArrayList<>()).add(increment);

        // 更新上次保存时间和内容
        lastSaveTime.put(filePath, now);
        lastFileContents.put(filePath, currentContent);

        // 更新增量计数
        int incrementCount = fileIncrementCounts.getOrDefault(filePath, 0) + 1;
        fileIncrementCounts.put(filePath, incrementCount);

        LOG.info("Increment saved for file: " + filePath + " (Count: " + incrementCount + ")");

        // 检查增量数量是否达到阈值
        if (incrementCount >= INCREMENT_THRESHOLD) {
            LOG.info("Increment threshold reached for file: " + filePath + ". Saving full version.");
            saveVersion(file);
        } else {
            // 保存增量到文件
            saveIncrement(increment, file);
        }
    }

    // 保存完整版本
    public void saveVersion(VirtualFile file) {
        String filePath = getRelativePath(file);
        LocalDateTime now = LocalDateTime.now();

        // 更新文件版本号
        int versionNumber = fileVersionNumbers.getOrDefault(filePath, 0) + 1;
        fileVersionNumbers.put(filePath, versionNumber);

        Version version = new Version(versionNumber);

        String content = getFileContent(file);
        if (content == null) {
            LOG.warn("Failed to get content for file: " + filePath + ". Skipping version save.");
            return;
        }

        // 添加快照
        version.addSnapshot(filePath, content);

        // 清理增量列表
        fileIncrements.put(filePath, new ArrayList<>());

        // 重置增量计数
        fileIncrementCounts.put(filePath, 0);

        // 更新上次内容
        lastFileContents.put(filePath, content);

        // 清理增量文件
        cleanIncrements(filePath, versionNumber);

        // 保存版本到磁盘
        saveVersionToDisk(version, filePath, versionNumber);

        LOG.info("Full version " + versionNumber + " saved for file: " + filePath);
    }

    // 保存增量到磁盘
    private void saveIncrement(Increment increment, VirtualFile file) {
        try {
            String filePath = getRelativePath(file);
            int versionNumber = fileVersionNumbers.getOrDefault(filePath, 1);

            if (versionNumber == 0) {
                versionNumber = 1;
                fileVersionNumbers.put(filePath, versionNumber);
            }

            String versionDirName = "version_" + String.format("%03d", versionNumber);
            Path incrementPath = versionTrackerPath.resolve(filePath).resolve(versionDirName).resolve("increment");
            if (!Files.exists(incrementPath)) {
                Files.createDirectories(incrementPath);
            }

            String timeStamp = String.valueOf(System.currentTimeMillis());
            String fileName = "increment_" + timeStamp + ".diff";
            Path fileSavePath = incrementPath.resolve(fileName);

            String content = "Timestamp: " + increment.getTimestamp() + "\n" +
                "Operation: " + increment.getOperationType() + "\n" +
                "Diff Content:\n" + increment.getDiffContent();

            // 确保父目录存在
            Files.createDirectories(fileSavePath.getParent());

            // 写入增量内容
            Files.write(fileSavePath, content.getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

            LOG.info("Increment file saved at: " + fileSavePath.toString());
        } catch (IOException e) {
            LOG.error("Failed to save increment for file: " + getRelativePath(file), e);
            notifyUser("Failed to save increment for file: " + getRelativePath(file) + " - " + e.getMessage());
        }
    }

    // 保存版本到磁盘
    private void saveVersionToDisk(Version version, String filePath, int versionNumber) {
        try {
            String versionDirName = "version_" + String.format("%03d", versionNumber);
            Path versionPath = versionTrackerPath.resolve(filePath).resolve(versionDirName);

            // 创建 snapshot 目录
            Path snapshotPath = versionPath.resolve("snapshot");
            Files.createDirectories(snapshotPath);

            // 解析文件的相对路径，确保目录结构
            Path relativePath = Paths.get(filePath);
            Path fileName = relativePath.getFileName();

            // 构建快照文件路径
            Path fileSavePath = snapshotPath.resolve(fileName);

            // 确保父目录存在
            Files.createDirectories(fileSavePath.getParent());

            // 写入快照内容
            Files.write(fileSavePath, version.getSnapshots().get(filePath).getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

            LOG.info("Snapshot saved at: " + fileSavePath.toString());
        } catch (IOException e) {
            LOG.error("Failed to save version to disk for file: " + filePath, e);
            notifyUser("Failed to save version for file: " + filePath + " - " + e.getMessage());
        }
    }

    // 清理增量文件
    private void cleanIncrements(String filePath, int versionNumber) {
        try {
            String versionDirName = "version_" + String.format("%03d", versionNumber);
            Path incrementPath = versionTrackerPath.resolve(filePath).resolve(versionDirName).resolve("increment");
            if (Files.exists(incrementPath)) {
                Files.walk(incrementPath)
                    .filter(Files::isRegularFile)
                    .forEach(file -> {
                        try {
                            Files.deleteIfExists(file);
                            LOG.info("Deleted increment file: " + file.toString());
                        } catch (IOException e) {
                            LOG.error("Failed to delete increment file: " + file.toString(), e);
                        }
                    });
            }
        } catch (IOException e) {
            LOG.error("Failed to clean increments for file: " + filePath, e);
            notifyUser("Failed to clean increments for file: " + filePath + " - " + e.getMessage());
        }
    }

    // 获取所有版本列表
    public List<Version> getAllVersions() {
        List<Version> versions = new ArrayList<>();
        try {
            Files.walk(versionTrackerPath)
                .filter(Files::isDirectory)
                .filter(path -> path.getFileName().toString().startsWith("version_"))
                .forEach(versionDir -> {
                    String versionDirName = versionDir.getFileName().toString();
                    int versionNumber = Integer.parseInt(versionDirName.substring(8));
                    String filePath = versionTrackerPath.relativize(versionDir.getParent()).toString().replace("\\", "/");
                    Version version = loadVersion(versionNumber, filePath);
                    if (version != null) {
                        versions.add(version);
                    }
                });
        } catch (IOException e) {
            LOG.error("Failed to get all versions", e);
            notifyUser("Failed to get all versions: " + e.getMessage());
        }
        // 按版本号排序
        versions.sort(Comparator.comparingInt(Version::getVersionNumber));
        return versions;
    }

    // 加载指定版本
    private Version loadVersion(int versionNumber, String filePath) {
        String versionDirName = "version_" + String.format("%03d", versionNumber);
        Path versionPath = versionTrackerPath.resolve(filePath).resolve(versionDirName);
        Path snapshotPath = versionPath.resolve("snapshot");

        Version version = new Version(versionNumber);

        try {
            if (Files.exists(snapshotPath)) {
                Files.walk(snapshotPath)
                    .filter(Files::isRegularFile)
                    .forEach(snapshotFile -> {
                        try {
                            String relativeSnapshotPath = versionTrackerPath.relativize(snapshotFile).toString().replace("\\", "/");
                            String content = Files.readString(snapshotFile, StandardCharsets.UTF_8);
                            version.addSnapshot(filePath, content);
                        } catch (IOException e) {
                            LOG.error("Failed to read snapshot file: " + snapshotFile.toString(), e);
                        }
                    });
            }
        } catch (IOException e) {
            LOG.error("Failed to load version for file: " + filePath, e);
            notifyUser("Failed to load version for file: " + filePath + " - " + e.getMessage());
            return null;
        }

        return version;
    }

    // 获取文件内容
    String getFileContent(VirtualFile file) {
        try {
            return new String(file.contentsToByteArray(), file.getCharset());
        } catch (IOException e) {
            LOG.error("Failed to get content for file: " + getRelativePath(file), e);
            notifyUser("Failed to get content for file: " + getRelativePath(file) + " - " + e.getMessage());
            return null;
        }
    }

    // 获取文件相对于项目的路径
    private String getRelativePath(VirtualFile file) {
        String projectPath = project.getBasePath();
        String filePath = file.getPath();

        // Exclude .version_tracker directory
        if (filePath.contains("/.version_tracker/") || filePath.endsWith("/.version_tracker")) {
            return null; // or handle accordingly
        }

        if (filePath.startsWith(projectPath)) {
            return filePath.substring(projectPath.length() + 1).replace("\\", "/");
        } else {
            return file.getName();
        }
    }


    // 通知用户
    private void notifyUser(String message) {
        Notification notification = NOTIFICATION_GROUP.createNotification("VersionTracker", message, NotificationType.ERROR);
        Notifications.Bus.notify(notification, project);
    }
}
