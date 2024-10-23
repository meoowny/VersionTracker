package edu.tongji.versiontracker;

import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.roots.ProjectRootManager;
import org.jetbrains.annotations.NotNull;

/**
 * 文件修改监听器
 * 粗粒度，用户关闭文件时触发
 */
public class FileCloseListener implements FileEditorManagerListener {

    private final VersionManager versionManager;
    private final Project project;

    public FileCloseListener(VersionManager versionManager, Project project) {
        this.versionManager = versionManager;
        this.project = project;
    }

    // 关闭文件
    @Override
    public void fileClosed(@NotNull FileEditorManager source, @NotNull VirtualFile file) {
        // 检查文件是否属于项目
        if (isProjectFile(file)) {
            versionManager.saveVersion(file);
        }
    }

    /**
     * 判断文件是否属于项目且需要进行版本控制。
     *
     * @param file VirtualFile 要判断的文件
     * @return true 如果文件属于项目且符合条件；否则返回 false
     */
    private boolean isProjectFile(@NotNull VirtualFile file) {
        // 获取项目基准路径
        String projectBasePath = project.getBasePath();
        if (projectBasePath == null) {
            return false; // 项目基准路径为空，无法判断，返回 false
        }

        // 获取文件路径
        String filePath = file.getPath();

        // 判断文件路径是否以项目基准路径为前缀
        if (!filePath.startsWith(projectBasePath)) {
            return false; // 文件不在项目目录下
        }

        // 排除特定文件夹和文件类型
        if (filePath.contains("/.idea/") || filePath.contains("/.gradle/") ||
            filePath.contains("/build/") || filePath.endsWith(".class") || filePath.endsWith(".iml")) {
            return false; // 排除.idea, .gradle, build 文件夹中的文件以及 .class 和 .iml 文件
        }

        // 检查文件是否属于项目的内容部分（如 src/main/java）
        ProjectFileIndex fileIndex = ProjectRootManager.getInstance(project).getFileIndex();
        if (!fileIndex.isInContent(file)) {
            return false; // 文件不在项目内容中
        }

        // 最终返回 true，表示文件属于项目且符合要求
        return true;
    }
}
