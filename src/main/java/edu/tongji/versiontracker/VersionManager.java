package edu.tongji.versiontracker;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.jetbrains.annotations.NotNull;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiTreeChangeEvent;

/**
 * 版本信息文件管理类，负责接收文件信息并生成版本信息，以及处理其他版本信息相关操作
 */
public class VersionManager {
    private final Project project;
    private final Path versionDirectory;
    private final List<VersionInfo> versionHistory = new ArrayList<>();
    private final int maxSavedVersions = 10; // 最大保存版本数量

    public VersionManager(Project project) throws IOException {
        this.project = project;
        String basePath = project.getBasePath();
        if (basePath == null) {
            throw new IllegalStateException("Project base path is not available");
        }
        this.versionDirectory = Paths.get(basePath, ".version_tracker");

        System.out.println("Version directory: " + this.versionDirectory);
    }

    /**
     * 创建版本目录
     * <br/>
     * 此方法负责在文件系统中创建一个版本目录如果目录已经存在，则不会抛出异常
     * 使用ApplicationManager的runWriteAction方法来执行写操作，以确保在IDE的写操作上下文中执行，这在使用IDE扩展时尤为重要
     * <br/>
     * 异常处理：当创建目录失败时，打印异常堆栈跟踪信息这有助于调试但应在生产环境中进行适当的错误处理和日志记录
     */
    public void createVersionDirectory() {
        ApplicationManager.getApplication().runWriteAction(() -> {
            try {
                Files.createDirectories(versionDirectory);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    /**
     * 跟踪文件变更
     * 该方法用于跟踪给定文件的变更，只跟踪有效且非空的文件
     * 如果文件为空、无效或为目录，将不执行任何操作
     * 由 {@link FileListener} 调用，用户手动保存时触发，事件包含信息较少，可做全量保存
     *
     * @param file 要跟踪的文件，不能为空且必须是有效的非目录文件
     */
   public void trackChange(VirtualFile file) {
    if (file == null || !file.isValid() || file.isDirectory()) return;

    ApplicationManager.getApplication().runReadAction(() -> {
        try {
            String content = new String(file.contentsToByteArray());
            VersionInfo.ChangeType changeType = VersionInfo.ChangeType.MODIFY;
            saveVersion(file.getPath(), content, changeType);
        } catch (IOException e) {
            e.printStackTrace();
        }
    });
}

    /**
     * 细粒度地追踪文件变更，每次编辑或进行文件相关操作时触发
     * 触发事件可能为文件修改、移动、创建、删除等
     *
     * @param event 触发事件，包含被修改文件的信息
     */
    public void trackChange(@NotNull PsiTreeChangeEvent event) {
        var file = event.getFile();
        if (file == null) return;

        VirtualFile virtualFile = file.getVirtualFile();
        if (virtualFile == null || !virtualFile.isValid() || virtualFile.isDirectory()) return;

        // 获取文件路径
        String filePath = virtualFile.getPath();

        // 排除不需要跟踪的文件夹，如 build、target、out
        if (filePath.contains("build") || filePath.contains("target") || filePath.contains("out") || filePath.contains(".version_tracker")) {
            return; // 如果路径包含这些目录，则忽略不处理
        }

        ApplicationManager.getApplication().runReadAction(() -> {
            try {
                String content = new String(virtualFile.contentsToByteArray());
                VersionInfo.ChangeType changeType;

                if (event.getChild() != null) {
                    if (event.getParent() != null) {
                        // 子节点被添加或移除
                        changeType = event.getChild().getParent() == null ? VersionInfo.ChangeType.DELETE : VersionInfo.ChangeType.CREATE;
                    } else {
                        // 子节点被替换或移动
                        changeType = VersionInfo.ChangeType.MODIFY;
                    }
                } else {
                    // 属性发生变化或子节点发生变化
                    changeType = VersionInfo.ChangeType.MODIFY;
                }

                // 保存版本
                saveVersion(virtualFile.getPath(), content, changeType);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }


    /**
     * 保存文件版本
     * 该方法负责将指定文件的内容保存到版本目录中，并在版本历史中记录该版本的信息
     * 它首先计算文件的相对路径，然后生成包含当前时间戳的安全文件名，最后将内容保存为字节流
     * 此方法确保文件的保存和版本跟踪，便于后续的版本回溯和管理
     */
    private void saveVersion(String filePath, String content, VersionInfo.ChangeType changeType) {
        try {
            // 获取基础路径
            String basePath = project.getBasePath();
            if (basePath == null) {
                System.err.println("Project base path is not available.");
                return;
            }

            // 使用 Path 进行路径标准化和拼接
            Path basePathPath = Paths.get(FileUtil.toSystemIndependentName(basePath));
            Path filePathPath = Paths.get(FileUtil.toSystemIndependentName(filePath));

            // 获取相对路径
            Path relativePath = basePathPath.relativize(filePathPath);
            if (relativePath == null) {
                System.err.println("Unable to get relative path for: " + filePath);
                return;
            }

            // 清理相对路径中的非法字符，以防在文件系统中创建时出错
            String cleanedRelativePath = relativePath.toString().replaceAll("[^a-zA-Z0-9_/\\.\\-]", "_");

            // 生成时间戳
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));

            if (changeType == VersionInfo.ChangeType.MODIFY) {
                // 保存增量
                Path incrementalPath = versionDirectory.resolve("incremental").resolve(cleanedRelativePath);
                // 确保创建完整的目录结构
                Files.createDirectories(incrementalPath.getParent() != null ? incrementalPath.getParent() : incrementalPath);

                // 保存增量文件，以时间戳命名
                Path incrementalFile = incrementalPath.resolveSibling("increment_" + timestamp + ".diff");
                Files.write(incrementalFile, content.getBytes(StandardCharsets.UTF_8));

                System.out.println("Incremental version saved: " + incrementalFile);
            } else {
                // 保存完整版本快照
                Path snapshotPath = versionDirectory.resolve("snapshot").resolve(timestamp).resolve(cleanedRelativePath);
                Files.createDirectories(snapshotPath.getParent());
                Files.write(snapshotPath, content.getBytes(StandardCharsets.UTF_8));

                // 在版本历史中添加新的版本信息
                versionHistory.add(new VersionInfo(timestamp, cleanedRelativePath, snapshotPath.toString(), changeType));

                System.out.println("Snapshot version saved: " + snapshotPath);
            }

            // 在保存版本后执行清理与合并逻辑
            cleanUpOldVersions();
            mergeAndCleanIncrementals();

            System.out.println("Change type: " + changeType);
            System.out.println("Total versions: " + versionHistory.size());
        } catch (IOException e) {
            System.err.println("Error saving version for file: " + filePath);
            e.printStackTrace();
        }
    }




    /**
     * 获取版本历史记录
     *
     * @return 返回一个包含所有版本信息的列表
     */
    public List<VersionInfo> getVersionHistory() {
        return new ArrayList<>(versionHistory);
    }

    /**
     * 版本信息类
     * <br/>
     * 该类用于封装特定时间点上文件及其版本文件的路径信息
     */
    public static class VersionInfo {
        public final String timestamp; // 版本时间戳
        public final String filePath; // 文件路径
        public final String versionFilePath; // 版本文件路径
        public final ChangeType changeType; // 当前文件修改的类型

        /**
         * 构造方法
         *
         * @param timestamp    时间戳，表示版本创建的时间
         * @param filePath     文件的路径，表示文件在系统中的位置
         * @param versionFilePath   版本文件的路径，表示存储版本信息的文件位置
         * @param changeType   文件修改的类型
         */
        VersionInfo(String timestamp, String filePath, String versionFilePath, ChangeType changeType) {
            this.timestamp = timestamp;
            this.filePath = filePath;
            this.versionFilePath = versionFilePath;
            this.changeType = changeType;
        }

        public enum ChangeType {
            CREATE,
            MODIFY,
            DELETE,
        }
    }

    /**
     *  版本回溯与恢复
     *
     * 增量回溯与完整版本回溯
     */
    public void rollbackToIncremental(int incrementalIndex) throws IOException {
        if (incrementalIndex < 0 || incrementalIndex >= versionHistory.size()) {
            throw new IllegalArgumentException("Invalid incremental index");
        }
        // 获取到增量版本历史
        VersionInfo targetVersion = versionHistory.get(incrementalIndex);
        String versionFilePath = targetVersion.versionFilePath;
        String restoredContent = new String(Files.readAllBytes(Paths.get(versionFilePath)));

        // 在这里实现文件的恢复逻辑，将当前文件替换为指定增量保存状态
        Path targetFilePath = Paths.get(targetVersion.filePath);
        Files.write(targetFilePath, restoredContent.getBytes());

        System.out.println("Restored to incremental version at index: " + incrementalIndex);
    }

    public void rollbackToVersion(int versionIndex) throws IOException {
        if (versionIndex < 0 || versionIndex >= versionHistory.size()) {
            throw new IllegalArgumentException("Invalid version index");
        }
        // 获取完整版本历史
        VersionInfo targetVersion = versionHistory.get(versionIndex);
        String versionFilePath = targetVersion.versionFilePath;
        String restoredContent = new String(Files.readAllBytes(Paths.get(versionFilePath)));

        // 实现文件恢复，将当前文件替换为指定的完整版本状态
        Path targetFilePath = Paths.get(targetVersion.filePath);
        Files.write(targetFilePath, restoredContent.getBytes());

        System.out.println("Restored to complete version at index: " + versionIndex);
    }

    /**
     * 版本控制的优化与清理
     *
     * 自动清理旧版本和冗余增量数据
     */
    public void cleanUpOldVersions() {
        if (versionHistory.size() <= maxSavedVersions) {
            return; // 无需清理
        }

        int versionsToDelete = versionHistory.size() - maxSavedVersions;
        for (int i = 0; i < versionsToDelete; i++) {
            VersionInfo versionInfo = versionHistory.get(i);
            try {
                // 删除旧的增量或完整版本文件
                Files.deleteIfExists(Paths.get(versionInfo.versionFilePath));
                versionHistory.remove(i);
                System.out.println("Deleted old version: " + versionInfo.versionFilePath);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 在保存完整版本后合并增量并清理
     */
    public void mergeAndCleanIncrementals() {
        List<VersionInfo> toBeDeleted = new ArrayList<>();
        for (VersionInfo versionInfo : versionHistory) {
            if (versionInfo.changeType == VersionInfo.ChangeType.MODIFY) {
                toBeDeleted.add(versionInfo);
            }
        }

        // 删除所有已合并的增量数据
        for (VersionInfo versionInfo : toBeDeleted) {
            try {
                Files.deleteIfExists(Paths.get(versionInfo.versionFilePath));
                versionHistory.remove(versionInfo);
                System.out.println("Deleted merged incremental version: " + versionInfo.versionFilePath);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
