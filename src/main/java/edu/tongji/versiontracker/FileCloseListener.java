package edu.tongji.versiontracker;

import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import static com.intellij.ide.actions.OpenProjectFileChooserDescriptor.isProjectFile;

/**
 * 文件修改监听器
 * 粒度较粗，用户关闭文件时触发
 */

public class FileCloseListener implements FileEditorManagerListener {

    private final VersionManager versionManager;

    public FileCloseListener(VersionManager versionManager) {
        this.versionManager = versionManager;
    }

    // 关闭文件
    @Override
    public void fileClosed(@NotNull FileEditorManager source, @NotNull VirtualFile file) {
        // 检查文件是否属于项目
        if (!file.isInLocalFileSystem() || !isProjectFile(file)) {
            return;
        }
        versionManager.saveVersion(file);
    }
}