package edu.tongji.versiontracker;

import com.github.difflib.UnifiedDiffUtils;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.github.difflib.DiffUtils;
import com.github.difflib.UnifiedDiffUtils;
import com.github.difflib.patch.Patch;


import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

public class VersionManager {

    private static final int INCREMENT_THRESHOLD = 10; // 增量保存次数阈值
    private static final Duration MIN_SAVE_INTERVAL = Duration.ofSeconds(5); // 最小保存间隔

    private final Project project;
    private final Path versionTrackerPath; // .version_tracker 目录路径
    private int currentVersionNumber;
    private final Map<String, List<Increment>> fileIncrements; // 文件名到增量列表的映射
    private final Map<String, LocalDateTime> lastSaveTime; // 文件名到上次保存时间的映射
    private final Map<String, String> lastFileContents; // 文件名到上次内容的映射

    public VersionManager(Project project) {
        this.project = project;
        this.versionTrackerPath = Paths.get(project.getBasePath(), ".version_tracker");
        this.currentVersionNumber = 0;
        this.fileIncrements = new HashMap<>();
        this.lastSaveTime = new HashMap<>();
        this.lastFileContents = new HashMap<>();
    }

    // 初始化方法，创建 .version_tracker 文件夹
    public void initialize() {
        try {
            if (!Files.exists(versionTrackerPath)) {
                Files.createDirectories(versionTrackerPath);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // 保存所有打开文件的完整版本（在项目关闭时调用）
    public void saveAllOpenFilesVersion() {
        // 获取所有打开的文件
        VirtualFile[] openFiles = project.getComponent(com.intellij.openapi.fileEditor.FileEditorManager.class).getOpenFiles();
        saveVersion(Arrays.asList(openFiles));
    }

    // 处理 PsiTree 事件（文件修改、创建、删除）
    public void handlePsiEvent(VirtualFile file) {
        handleEditEvent(file);
    }

    // 处理编辑事件，保存增量
    public void handleEditEvent(VirtualFile file) {
        String fileName = file.getName();
        LocalDateTime now = LocalDateTime.now();

        // 检查最小保存间隔
        if (lastSaveTime.containsKey(fileName)) {
            Duration duration = Duration.between(lastSaveTime.get(fileName), now);
            if (duration.compareTo(MIN_SAVE_INTERVAL) < 0) {
                // 合并增量，不立即保存
                return;
            }
        }

        // 获取当前文件内容
        String currentContent = getFileContent(file);

        // 获取上一次保存的内容，如果没有，则认为是空字符串
        String lastContent = lastFileContents.getOrDefault(fileName, "");

        // 计算差异
        List<String> originalLines = Arrays.asList(lastContent.split("\n"));
        List<String> revisedLines = Arrays.asList(currentContent.split("\n"));

        Patch<String> patch = DiffUtils.diff(originalLines, revisedLines);

        // 如果没有差异，不保存增量
        if (patch.getDeltas().isEmpty()) {
            return;
        }

        // 生成统一格式的差异（unified diff）
        List<String> diffLines = UnifiedDiffUtils.generateUnifiedDiff(
            fileName, // 原始文件名
            fileName, // 修改后的文件名
            originalLines, // 原始文件内容
            patch, // 差异补丁
            0); // 上下文行数，可以设置为 0 或其他值

        // 将差异行合并为一个字符串
        String diffContent = String.join("\n", diffLines);
        // 创建增量
        Increment increment = new Increment(fileName, diffContent, "修改");

        // 添加到增量列表
        fileIncrements.computeIfAbsent(fileName, k -> new ArrayList<>()).add(increment);

        // 更新上次保存时间和内容
        lastSaveTime.put(fileName, now);
        lastFileContents.put(fileName, currentContent);

        // 检查增量数量是否达到阈值
        if (fileIncrements.get(fileName).size() >= INCREMENT_THRESHOLD) {
            saveVersion(List.of(file));
        } else {
            // 保存增量到文件
            saveIncrement(increment);
        }
    }

    // 保存完整版本
    public void saveVersion(List<VirtualFile> files) {
        currentVersionNumber++;
        Version version = new Version(currentVersionNumber);

        for (VirtualFile file : files) {
            String fileName = file.getName();
            String content = getFileContent(file);

            // 添加快照
            version.addSnapshot(fileName, content);

            // 清理增量列表
            fileIncrements.put(fileName, new ArrayList<>());

            // 更新上次内容
            lastFileContents.put(fileName, content);

            // 清理增量文件
            cleanIncrements(fileName);
        }

        // 保存版本到磁盘
        saveVersionToDisk(version);
    }

    // 保存增量到磁盘
    private void saveIncrement(Increment increment) {
        try {
            String versionDirName = "version_" + String.format("%03d", currentVersionNumber);
            Path incrementPath = versionTrackerPath.resolve(versionDirName).resolve("increment");
            if (!Files.exists(incrementPath)) {
                Files.createDirectories(incrementPath);
            }
            String timeStamp = String.valueOf(System.currentTimeMillis());
            String fileName = increment.getFileName() + "_increment_" + timeStamp + ".diff";
            Path filePath = incrementPath.resolve(fileName);

            String content = "Timestamp: " + increment.getTimestamp() + "\n" +
                "Operation: " + increment.getOperationType() + "\n" +
                "Diff Content:\n" + increment.getDiffContent();

            Files.write(filePath, content.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // 保存版本到磁盘
    private void saveVersionToDisk(Version version) {
        try {
            String versionDirName = "version_" + String.format("%03d", version.getVersionNumber());
            Path versionPath = versionTrackerPath.resolve(versionDirName);

            // 创建 snapshot 目录
            Path snapshotPath = versionPath.resolve("snapshot");
            Files.createDirectories(snapshotPath);

            // 保存快照
            for (Map.Entry<String, String> entry : version.getSnapshots().entrySet()) {
                String fileName = entry.getKey();
                String content = entry.getValue();
                Path filePath = snapshotPath.resolve(fileName);
                Files.write(filePath, content.getBytes(StandardCharsets.UTF_8));
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // 清理增量文件
    private void cleanIncrements(String fileName) {
        try {
            String versionDirName = "version_" + String.format("%03d", currentVersionNumber);
            Path incrementPath = versionTrackerPath.resolve(versionDirName).resolve("increment");
            if (Files.exists(incrementPath)) {
                Files.walk(incrementPath)
                    .filter(path -> path.getFileName().toString().startsWith(fileName + "_increment_"))
                    .forEach(file -> {
                        try {
                            Files.deleteIfExists(file);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    });
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // 获取所有版本列表
    public List<Version> getAllVersions() {
        List<Version> versions = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(versionTrackerPath)) {
            for (Path entry : stream) {
                if (Files.isDirectory(entry) && entry.getFileName().toString().startsWith("version_")) {
                    int versionNumber = Integer.parseInt(entry.getFileName().toString().substring(8));
                    Version version = loadVersion(versionNumber);
                    if (version != null) {
                        versions.add(version);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        // 按版本号排序
        versions.sort(Comparator.comparingInt(Version::getVersionNumber));
        return versions;
    }

    // 加载指定版本
    private Version loadVersion(int versionNumber) {
        String versionDirName = "version_" + String.format("%03d", versionNumber);
        Path versionPath = versionTrackerPath.resolve(versionDirName);
        Path snapshotPath = versionPath.resolve("snapshot");

        Version version = new Version(versionNumber);

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(snapshotPath)) {
            for (Path entry : stream) {
                if (Files.isRegularFile(entry)) {
                    String fileName = entry.getFileName().toString();
                    String content = Files.readString(entry);
                    version.addSnapshot(fileName, content);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        return version;
    }

    // 获取文件内容
    String getFileContent(VirtualFile file) {
        try {
            return new String(file.contentsToByteArray(), file.getCharset());
        } catch (IOException e) {
            e.printStackTrace();
            return "";
        }
    }
}
