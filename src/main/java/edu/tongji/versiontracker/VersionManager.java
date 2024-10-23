package edu.tongji.versiontracker;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.difflib.UnifiedDiffUtils;
import com.github.difflib.patch.ChangeDelta;
import com.github.difflib.patch.PatchFailedException;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.github.difflib.DiffUtils;
import com.github.difflib.patch.Patch;
import com.intellij.notification.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.io.*;
import java.nio.file.*;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;

import static com.intellij.ide.actions.OpenProjectFileChooserDescriptor.isProjectFile;

public class VersionManager {

    private static final Logger LOG = Logger.getInstance(VersionManager.class);
    private static final int INCREMENT_THRESHOLD = 5; // 增量保存次数阈值
    private static final Duration MIN_SAVE_INTERVAL = Duration.ofSeconds(3); // 最小保存间隔
    private static final String TRACKER_BRANCH = "VersionTracker";

    private final Project project;
    private final Path versionTrackerPath; // .version_tracker 目录路径
    private final Map<String, Integer> fileVersionNumbers; // 文件路径到版本号的映射
    private final Map<String, Integer> fileIncrementCounts; // 文件路径到增量计数的映射

    private final Map<String, LocalDateTime> lastIncrementSaveTime; // 文件路径到上次增量保存时间的映射
    private final Map<String, String> lastIncrementContent; // 文件路径到上次增量保存内容的映射
    private final Map<String, String> latestContent; // 文件路径到最新内容的映射

    private final GitManager gitManager;
    private String originBranch;
    private boolean isInTrackerBranch;

    // 获取通知组的实例
    private static final NotificationGroup NOTIFICATION_GROUP = NotificationGroupManager.getInstance()
        .getNotificationGroup("VersionTracker Notifications");

    public VersionManager(@NotNull Project project) {
        this.project = project;
        this.versionTrackerPath = Paths.get(project.getBasePath(), ".version_tracker");
        this.fileVersionNumbers = new HashMap<>();
        this.fileIncrementCounts = new HashMap<>();

        this.lastIncrementSaveTime = new HashMap<>();
        this.lastIncrementContent = new HashMap<>();
        this.latestContent = new HashMap<>();

        try {
            this.gitManager = new GitManager(project.getBasePath());
            this.isInTrackerBranch = gitManager.getCurrentBranch().equals(TRACKER_BRANCH);
            this.originBranch = isInTrackerBranch ? "main" : gitManager.getCurrentBranch();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
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
            // 加载增量计数器
            loadIncrementCounts();
            // 加载文件版本号
            loadFileVersionNumbers();
        } catch (IOException e) {
            LOG.error("Failed to create .version_tracker directory", e);
            notifyUser("Failed to create .version_tracker directory: " + e.getMessage());
        }
    }

    // 加载文件版本号
    private void loadFileVersionNumbers() {
        try {
            if (!Files.exists(versionTrackerPath)) {
                return;
            }

            Files.walk(versionTrackerPath)
                .filter(Files::isDirectory)
                .forEach(path -> {
                    Path relativePath = versionTrackerPath.relativize(path);
                    String dirName = path.getFileName().toString(); // 获取当前目录的名称

                    // 检查目录是否符合 "version_*" 格式
                    if (dirName.startsWith("version_")) {
                        try {
                            // 提取版本号部分
                            String versionStr = dirName.substring("version_".length());
                            if (!versionStr.isEmpty()) {
                                int versionNum = Integer.parseInt(versionStr);
                                // 获取文件相对路径
                                Path parentPath = relativePath.getParent();
                                if (parentPath != null) {
                                    String filePath = parentPath.toString().replace("\\", "/");
                                    int currentMaxVersion = fileVersionNumbers.getOrDefault(filePath, 0);
                                    if (versionNum > currentMaxVersion) {
                                        fileVersionNumbers.put(filePath, versionNum);
                                    }
                                }
                            }
                        } catch (NumberFormatException e) {
                            LOG.error("Error parsing version number from directory: " + dirName, e);
                        }
                    }
                });
        } catch (IOException e) {
            LOG.error("Failed to load file version numbers", e);
        }
    }


    public void saveInitialVersion(VirtualFile file) {
        String filePath = getRelativePath(file);

        if (filePath == null) {
            // 跳过 .version_tracker 目录
            return;
        }

        // 检查是否已有版本存在
        if (fileVersionNumbers.containsKey(filePath) && fileVersionNumbers.get(filePath) >= 1) {
            // 已有版本，跳过初始版本保存
            return;
        }

        // 检查版本目录是否存在
        Path versionDirPath = versionTrackerPath.resolve(filePath).resolve("version_001");
        if (Files.exists(versionDirPath)) {
            // 初始版本目录已存在，更新文件版本号
            fileVersionNumbers.put(filePath, 1);
            return;
        }

        // 设置初始版本号为1
        int versionNumber = 1;
        Version version = new Version(versionNumber);

        String content = getFileContent(file);
        if (content == null) {
            LOG.warn("Failed to get content for file: " + filePath + ". Skipping initial version save.");
            return;
        }

        // 添加快照
        version.addSnapshot(filePath, content);

        // 保存版本到磁盘
        saveVersionToDisk(version, filePath, versionNumber);

        // 更新文件版本号
        fileVersionNumbers.put(filePath, versionNumber);

        LOG.info("Initial version " + versionNumber + " saved for file: " + filePath);
    }

    // 加载增量计数器
    private void loadIncrementCounts() {
        try {
            Path incrementCountsPath = versionTrackerPath.resolve("increment_counts.ser");
            if (Files.exists(incrementCountsPath)) {
                try (ObjectInputStream ois = new ObjectInputStream(Files.newInputStream(incrementCountsPath))) {
                    @SuppressWarnings("unchecked") // 抑制未检查的转换警告
                    Map<String, Integer> savedCounts = (Map<String, Integer>) ois.readObject();
                    fileIncrementCounts.putAll(savedCounts);
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            LOG.error("Failed to load increment counts", e);
        }
    }

    public void saveAllOpenFilesVersion() {
        // 保存所有文档
        FileDocumentManager.getInstance().saveAllDocuments();

        // 获取所有打开的文件
        VirtualFile[] openFiles = FileEditorManager.getInstance(project).getOpenFiles();
        for (VirtualFile file : openFiles) {
            if (!file.isInLocalFileSystem() || !isProjectFile(file)) {
                continue; // 使用 continue 跳过当前文件，继续下一个
            }

            // 获取相对路径
            String filePath = getRelativePath(file);

            // 获取当前文件的最大版本号
            int maxVersionNumber = fileVersionNumbers.getOrDefault(filePath, 0);
            if (maxVersionNumber == 0) {
                continue; // 如果没有版本号，跳过该文件
            }

            // 生成最新版本的目录路径
            String versionDirName = "version_" + String.format("%03d", maxVersionNumber);
            Path incrementPath = versionTrackerPath.resolve(filePath).resolve(versionDirName).resolve("increments");

            // 打印路径以帮助调试
            System.out.println("Saving version for file: " + filePath);
            System.out.println("Increment path: " + incrementPath);

            // 检查最新版本目录的 increments 目录下是否有文件
            try {
                if (Files.exists(incrementPath) && Files.list(incrementPath).anyMatch(Files::isRegularFile)) {
                    saveVersion(file); // 仅当目录中有文件时保存版本
                }
            } catch (IOException e) {
                LOG.error("Failed to check files in increments directory for file: " + filePath, e);
            }
        }
    }

    // 增量计数器持久化保存
    private void saveIncrementCounts() {
        try {
            Path incrementCountsPath = versionTrackerPath.resolve("increment_counts.ser");
            try (ObjectOutputStream oos = new ObjectOutputStream(Files.newOutputStream(incrementCountsPath))) {
                oos.writeObject(fileIncrementCounts);
            }
        } catch (IOException e) {
            LOG.error("Failed to save increment counts", e);
        }
    }

    // 处理编辑事件，保存增量
    public void handleEditEvent(VirtualFile file) {
        String filePath = getRelativePath(file);

        if (filePath == null) {
            // 跳过 .version_tracker 目录
            return;
        }

        // 获取当前文件内容
        String currentContent = getFileContent(file);
        if (currentContent == null) {
            LOG.warn("Failed to get content for file: " + filePath + ". Skipping increment save.");
            return;
        }

        // 更新最新内容
        latestContent.put(filePath, currentContent);

        LocalDateTime now = LocalDateTime.now();

        // 检查是否需要保存增量
        if (lastIncrementSaveTime.containsKey(filePath)) {
            Duration duration = Duration.between(lastIncrementSaveTime.get(filePath), now);
            if (duration.compareTo(MIN_SAVE_INTERVAL) >= 0) {
                // 达到最小保存间隔，保存增量
                saveIncrement(file);
                // 更新上次增量保存时间和内容
                lastIncrementSaveTime.put(filePath, now);
                lastIncrementContent.put(filePath, currentContent);
            } else {
                // 未达到最小保存间隔，暂不保存，等待下次
                LOG.info("Edit detected for file: " + filePath + ", but minimum save interval not reached.");
            }
        } else {
            // 首次编辑，保存增量
            saveIncrement(file);
            // 初始化上次增量保存时间和内容
            lastIncrementSaveTime.put(filePath, now);
            lastIncrementContent.put(filePath, currentContent);
        }
    }


    // 保存增量
    private void saveIncrement(VirtualFile file) {
        String filePath = getRelativePath(file);

        if (filePath == null) {
            // 跳过 .version_tracker 目录
            return;
        }

        String currentContent = latestContent.get(filePath);
        String lastContent = lastIncrementContent.getOrDefault(filePath, "");

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

        // 创建增量对象
        Increment increment = new Increment(filePath, diffContent, "修改");

        // 保存增量文件
        saveIncrementToDisk(increment, file);

        // 更新增量计数器
        int incrementCount = fileIncrementCounts.getOrDefault(filePath, 0) + 1;
        fileIncrementCounts.put(filePath, incrementCount);

        // 保存增量计数器
        saveIncrementCounts();

        LOG.info("Increment saved for file: " + filePath + " (Count: " + incrementCount + ")");

        // 检查增量计数器是否达到阈值
        if (incrementCount >= INCREMENT_THRESHOLD) {
            // 保存完整版本
            saveVersion(file);
            // 重置增量计数器
            fileIncrementCounts.put(filePath, 0);
            saveIncrementCounts();
        }
    }

    // 保存增量到磁盘
    private void saveIncrementToDisk(Increment increment, VirtualFile file) {
        try {
            String filePath = getRelativePath(file);
            int versionNumber = fileVersionNumbers.getOrDefault(filePath, 0) + 1; // 当前版本号，如果还没有版本则使用1

            String versionDirName = "version_" + String.format("%03d", versionNumber);
            Path incrementPath = versionTrackerPath.resolve(filePath).resolve(versionDirName).resolve("increments");
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

    // 保存完整版本
    public void saveVersion(VirtualFile file) {
        String filePath = getRelativePath(file);
        LocalDateTime now = LocalDateTime.now();

        if (filePath == null) {
            // 跳过 .version_tracker 目录或无法获取相对路径的文件
            return;
        }

        // 获取当前版本号
        int versionNumber = getNextVersionNumber(filePath);

        // 创建新版本对象
        Version version = new Version(versionNumber);

        // 获取文件内容
        String content = latestContent.get(filePath);
        if (content == null) {
            content = getFileContent(file);
            if (content == null) {
                LOG.warn("Failed to get content for file: " + filePath + ". Skipping version save.");
                return;
            }
        }

        // 添加快照
        version.addSnapshot(filePath, content);

        // 保存版本到磁盘
        saveVersionToDisk(version, filePath, versionNumber);

        // 更新文件版本号
        fileVersionNumbers.put(filePath, versionNumber);

        // 重置增量计数
        fileIncrementCounts.put(filePath, 0);

        // 更新上次增量内容
        lastIncrementContent.put(filePath, content);
        lastIncrementSaveTime.put(filePath, now);

        // 清理增量文件
        cleanIncrements(filePath, versionNumber);

        LOG.info("Full version " + versionNumber + " saved for file: " + filePath);
        // 显示通知
        notifyVersionCreated(project, filePath, versionNumber);

        if (isInTrackerBranch) {
            try {
                gitManager.commitChanges("Version " + versionNumber);
            } catch (Exception e) {
                System.err.println("Failed to commit changes for " + filePath);
            }
        }
    }

    // 获取下一个版本号
    private int getNextVersionNumber(String filePath) {
        // 检查已有版本号
        if (fileVersionNumbers.containsKey(filePath)) {
            return fileVersionNumbers.get(filePath) + 1;
        } else {
            // 如果没有记录，扫描版本目录获取最大版本号
            int maxVersionNumber = getMaxVersionNumber(filePath);
            return maxVersionNumber + 1;
        }
    }

    private int getMaxVersionNumber(String filePath) {
        Path fileVersionPath = versionTrackerPath.resolve(filePath);
        if (!Files.exists(fileVersionPath)) {
            return 0;
        }

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(fileVersionPath, "version_*")) {
            int maxVersion = 0;
            for (Path path : stream) {
                String dirName = path.getFileName().toString();
                if (dirName.startsWith("version_")) {
                    int versionNum = Integer.parseInt(dirName.substring(8));
                    if (versionNum > maxVersion) {
                        maxVersion = versionNum;
                    }
                }
            }
            return maxVersion;
        } catch (IOException e) {
            LOG.error("Failed to get max version number for file: " + filePath, e);
            return 0;
        }
    }

    // 保存版本到磁盘
    private void saveVersionToDisk(Version version, String filePath, int versionNumber) {
        try {
            String versionDirName = "version_" + String.format("%03d", versionNumber);
            Path versionPath = versionTrackerPath.resolve(filePath).resolve(versionDirName);

            // 创建 snapshot 目录
            Path snapshotPath = versionPath.resolve("snapshot");
            if (!Files.exists(snapshotPath)) {
                Files.createDirectories(snapshotPath);
            }

            // 创建 increments 目录
            Path incrementsPath = versionPath.resolve("increments");
            if (!Files.exists(incrementsPath)) {
                Files.createDirectories(incrementsPath);
            }

            String snapshotContent;

            // 判断是否是初始版本
            if (versionNumber == 1) {
                // 对于初始版本，直接使用当前内容作为快照内容
                snapshotContent = version.getSnapshots().get(filePath);
                if (snapshotContent == null) {
                    LOG.error("No snapshot content available for initial version of file: " + filePath);
                    notifyUser("No snapshot content available for initial version of file: " + filePath);
                    return;
                }
            } else {
                // 判断 increments 目录下是否有内容
                boolean hasIncrementFiles = Files.list(incrementsPath).anyMatch(Files::isRegularFile);
                if (hasIncrementFiles) {
                    // 如果 increments 目录下有文件，合并增量文件生成快照内容
                    snapshotContent = generateSnapshotContent(filePath, versionNumber);
                    if (snapshotContent == null) {
                        LOG.error("Failed to generate snapshot content for file: " + filePath);
                        notifyUser("Failed to generate snapshot content for file: " + filePath);
                        return;
                    }
                } else {
                    // 如果 increments 目录为空，直接使用当前内容作为快照内容
                    snapshotContent = version.getSnapshots().get(filePath);
                    if (snapshotContent == null) {
                        LOG.error("No snapshot content available for version of file: " + filePath);
                        notifyUser("No snapshot content available for version of file: " + filePath);
                        return;
                    }
                }
            }

            // 构建快照文件路径
            Path fileSavePath = snapshotPath.resolve(Paths.get(filePath).getFileName());

            // 确保父目录存在
            Files.createDirectories(fileSavePath.getParent());

            // 写入快照内容
            Files.write(fileSavePath, snapshotContent.getBytes(StandardCharsets.UTF_8),
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

            LOG.info("Snapshot saved at: " + fileSavePath.toString());

            // 保存元数据
            Path metadataPath = versionPath.resolve("metadata.ser");
            version.setSnapshots(null); // 为了节省空间，可以在序列化前清空快照内容
            saveVersionMetadata(version, metadataPath);

            LOG.info("Metadata saved at: " + metadataPath.toString());

        } catch (IOException e) {
            LOG.error("Failed to save version to disk for file: " + filePath, e);
            notifyUser("Failed to save version for file: " + filePath + " - " + e.getMessage());
        }
    }



    // 保存元数据
    private void saveVersionMetadata(Version version, Path metadataPath) throws IOException {
        try (ObjectOutputStream oos = new ObjectOutputStream(Files.newOutputStream(metadataPath))) {
            oos.writeObject(version);
        }
    }

    // 合成增量为版本
    private String generateSnapshotContent(String filePath, int versionNumber) {
        try {
            String versionDirName = "version_" + String.format("%03d", versionNumber);
            Path versionPath = versionTrackerPath.resolve(filePath).resolve(versionDirName);
            Path incrementPath = versionPath.resolve("increments");

            if (!Files.exists(incrementPath)) {
                LOG.warn("No increments found for version " + versionNumber + " of file: " + filePath);
                return null;
            }

            // 读取前一个版本的快照内容
            int previousVersionNumber = versionNumber - 1;
            String previousVersionDirName = "version_" + String.format("%03d", previousVersionNumber);
            Path previousSnapshotPath = versionTrackerPath.resolve(filePath)
                    .resolve(previousVersionDirName)
                    .resolve("snapshot")
                    .resolve(Paths.get(filePath).getFileName());

            if (!Files.exists(previousSnapshotPath)) {
                LOG.warn("Previous snapshot not found for version " + previousVersionNumber + " of file: " + filePath);
                return null;
            }

            String originalContent = Files.readString(previousSnapshotPath, StandardCharsets.UTF_8);

            // 读取并应用所有增量
            List<Path> incrementFiles = Files.list(incrementPath)
                    .filter(path -> path.toString().endsWith(".diff"))
                    .sorted()
                    .collect(Collectors.toList());

            String patchedContent = originalContent;

            for (Path incrementFile : incrementFiles) {
                String diffContent = Files.readString(incrementFile, StandardCharsets.UTF_8);
                patchedContent = applyDiff(patchedContent, diffContent);
                if (patchedContent == null) {
                    LOG.error("Failed to apply diff from increment file: " + incrementFile);
                    return null;
                }
            }

            return patchedContent;

        } catch (IOException e) {
            LOG.error("Failed to generate snapshot content for file: " + filePath, e);
            return null;
        }
    }


    private String applyDiff(String originalContent, String diffContent) {
        try {
            // 将原始内容按行拆分
            List<String> originalLines = Arrays.asList(originalContent.split("\n"));

            // 将 diff 内容按行拆分
            List<String> diffLines = Arrays.asList(diffContent.split("\n"));

            // 解析统一格式的 diff，生成补丁
            Patch<String> patch = UnifiedDiffUtils.parseUnifiedDiff(diffLines);

            // 应用补丁到原始内容
            List<String> patchedLines = DiffUtils.patch(originalLines, patch);

            // 将结果合并为一个字符串
            return String.join("\n", patchedLines);
        } catch (PatchFailedException e) {
            LOG.error("Failed to apply diff", e);
            return originalContent; // 如果应用失败，返回原始内容
        }
    }

    // 清理增量文件
    private void cleanIncrements(String filePath, int versionNumber) {
        try {
            String versionDirName = "version_" + String.format("%03d", versionNumber);
            Path incrementPath = versionTrackerPath.resolve(filePath).resolve(versionDirName).resolve("increments");
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
    String getRelativePath(VirtualFile file) {
        String projectPath = project.getBasePath();
        String filePath = file.getPath();

        // 忽略 .version_tracker 目录
        if (filePath.contains("/.version_tracker/") || filePath.endsWith("/.version_tracker")) {
            return null;
        }

        if (filePath.startsWith(projectPath)) {
            return filePath.substring(projectPath.length() + 1).replace("\\", "/");
        } else {
            return file.getName();
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

    // 加载元数据
    private Version loadVersionMetadata(Path metadataPath) throws IOException, ClassNotFoundException {
        try (ObjectInputStream ois = new ObjectInputStream(Files.newInputStream(metadataPath))) {
            return (Version) ois.readObject();
        }
    }


    private Version loadVersion(int versionNumber, String filePath) {
        String versionDirName = "version_" + String.format("%03d", versionNumber);
        Path versionPath = versionTrackerPath.resolve(filePath).resolve(versionDirName);
        Path snapshotPath = versionPath.resolve("snapshot");
        Path metadataPath = versionPath.resolve("metadata.ser");

        if (!Files.exists(metadataPath)) {
            LOG.warn("Metadata file does not exist for version: " + versionNumber + " of file: " + filePath);
            return null; // 或者根据需要创建一个默认的 Version 对象
        }

        try {
            // 反序列化版本元数据
            Version version = loadVersionMetadata(metadataPath);

            // 读取快照内容
            Path fileSavePath = snapshotPath.resolve(Paths.get(filePath).getFileName());
            if (Files.exists(fileSavePath)) {
                String snapshotContent = Files.readString(fileSavePath, StandardCharsets.UTF_8);
                version.addSnapshot(filePath, snapshotContent);
            }

            return version;
        } catch (IOException | ClassNotFoundException e) {
            LOG.error("Failed to load version for file: " + filePath, e);
            notifyUser("Failed to load version for file: " + filePath + " - " + e.getMessage());
            return null;
        }
    }

    // 检索特定文件的版本
    public List<Version> getVersionsForFile(String relativeFilePath) {
        List<Version> versions = new ArrayList<>();

        Path fileVersionPath = versionTrackerPath.resolve(relativeFilePath);
        if (!Files.exists(fileVersionPath)) {
            return versions;
        }

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(fileVersionPath, "version_*")) {
            for (Path versionDir : stream) {
                String dirName = versionDir.getFileName().toString();
                if (dirName.startsWith("version_")) {
                    int versionNum = Integer.parseInt(dirName.substring(8));
                    Version version = loadVersion(versionNum, relativeFilePath);
                    if (version != null) {
                        versions.add(version);
                    }
                }
            }
            // Sort versions by version number
            versions.sort(Comparator.comparingInt(Version::getVersionNumber));
        } catch (IOException e) {
            LOG.error("Failed to get versions for file: " + relativeFilePath, e);
        }
        return versions;
    }

    // 回溯方法
    public void replaceFileContent(VirtualFile file, String newContent) throws IOException {
        if (file.isWritable()) {
            ApplicationManager.getApplication().runWriteAction(() -> {
                try {
                    // 将内容转换为字节数组，使用文件的字符集
                    byte[] contentBytes = newContent.getBytes(file.getCharset());
                    file.setBinaryContent(contentBytes);
                } catch (IOException e) {
                    LOG.error("Failed to replace content for file: " + getRelativePath(file), e);
                }
            });
        } else {
            throw new IOException("File is not writable: " + getRelativePath(file));
        }
    }

    // 通知用户
    private void notifyUser(String message) {
        Notification notification = NOTIFICATION_GROUP.createNotification("VersionTracker", message, NotificationType.ERROR);
        Notifications.Bus.notify(notification, project);
    }

    public void toggleTrackerStatus() {
        if (isInTrackerBranch) {
            mergeChanges();
        } else {
            initializeBranch();
        }
    }

    public void notifyVersionCreated(Project project, String filePath, int versionNumber) {
        String title = "New Version";
        String content = "File " + filePath + " 'version' " + versionNumber + " successfully created.";

        Notification notification = NotificationGroupManager.getInstance()
            .getNotificationGroup("VersionTracker Notifications")
            .createNotification(title, content, NotificationType.INFORMATION);

        notification.notify(project);
    }

    // 初始化插件分支
    private void initializeBranch() {
        if (isInTrackerBranch) {
            // 已经在插件分支
            return;
        }
        try {
            // 保存项目原来所在分支，创建并切换至插件分支
            originBranch = gitManager.getCurrentBranch();
            gitManager.checkoutBranch(TRACKER_BRANCH);
            isInTrackerBranch = true;
            System.out.println("Checked out to tracker branch");
        } catch (Exception e) {
            System.err.println("Failed to create branch: " + e.getMessage());
        }
    }

    // 合并插件分支上的更改
    private void mergeChanges() {
        if (!isInTrackerBranch) {
            return;
        }
        try {
            gitManager.commitChanges("Version saved");
            gitManager.mergeBranch(TRACKER_BRANCH, originBranch);
            isInTrackerBranch = false;
            LOG.info("Merged to tracker branch");
        } catch (Exception e) {
            LOG.error("Failed to merge branch: " + e.getMessage());
        }
    }
}

