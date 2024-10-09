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

    /**
     * 创建版本目录
     *
     * 此方法负责在文件系统中创建一个版本目录如果目录已经存在，则不会抛出异常
     * 使用ApplicationManager的runWriteAction方法来执行写操作，以确保在IDE的写操作上下文中执行，这在使用IDE扩展时尤为重要
     *
     * 异常处理：当创建目录失败时，打印异常堆栈跟踪信息这有助于调试但应在生产环境中进行适当的错误处理和日志记录
     */
    private void createVersionDirectory() {
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
     *
     * @param file 要跟踪的文件，不能为空且必须是有效的非目录文件
     */
    public void trackChange(VirtualFile file) {
        // 检查文件是否为空、无效或为目录，如果是，则直接返回，不执行任何操作
        if (file == null || !file.isValid() || file.isDirectory()) return;

        // 使用应用程序管理器的读取操作运行，确保在读取文件内容时不会阻塞UI线程
        ApplicationManager.getApplication().runReadAction(() -> {
            try {
                // 读取文件内容并将其转换为字符串
                String content = new String(file.contentsToByteArray());
                // 保存文件的当前版本，包括文件路径和内容
                saveVersion(file.getPath(), content);
            } catch (IOException e) {
                // 如果在读取文件时发生IO异常，则打印异常堆栈跟踪信息
                e.printStackTrace();
            }
        });
    }

    /**
     * 保存文件版本
     *
     * 该方法负责将指定文件的内容保存到版本目录中，并在版本历史中记录该版本的信息
     * 它首先计算文件的相对路径，然后生成包含当前时间戳的安全文件名，最后将内容保存为字节流
     * 此方法确保文件的保存和版本跟踪，便于后续的版本回溯和管理
     *
     * @param filePath 待保存文件的完整路径
     * @param content 文件内容
     * @throws IOException 如果文件写入过程中发生I/O错误
     */
    private void saveVersion(String filePath, String content) throws IOException {
        // 获取文件相对于项目基目录的路径，如果无法获取，则直接返回
        String relativePath = FileUtil.getRelativePath(project.getBasePath(), filePath, File.separatorChar);
        if (relativePath == null) return;

        // 生成当前时间的时间戳，格式为yyyyMMddHHmmss
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        // 生成安全的文件名，包括时间戳、相对路径替换为下划线，并过滤掉非字母数字字符
        String safeFileName = timestamp + "_" + relativePath.replace(File.separatorChar, '_').replaceAll("[^a-zA-Z0-9.-]", "_");
        // 构建版本文件的完整路径
        Path versionFile = versionDirectory.resolve(safeFileName);

        // 将文件内容写入到版本文件中
        Files.write(versionFile, content.getBytes());
        // 在版本历史中添加新的版本信息
        versionHistory.add(new VersionInfo(timestamp, relativePath, versionFile.toString()));
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
     *
     * 该类用于封装特定时间点上文件及其版本文件的路径信息
     */
    public static class VersionInfo {
        public final String timestamp; // 版本时间戳
        public final String filePath; // 文件路径
        public final String versionFilePath; // 版本文件路径

        /**
         * 构造方法
         *
         * @param timestamp    时间戳，表示版本创建的时间
         * @param filePath     文件的路径，表示文件在系统中的位置
         * @param versionFilePath   版本文件的路径，表示存储版本信息的文件位置
         */
        VersionInfo(String timestamp, String filePath, String versionFilePath) {
            this.timestamp = timestamp;
            this.filePath = filePath;
            this.versionFilePath = versionFilePath;
        }
    }
}
