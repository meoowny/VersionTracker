package edu.tongji.versiontracker;

import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

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
//        versionManager.saveVersion(file);
    }

}