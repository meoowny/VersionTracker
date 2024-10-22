package edu.tongji.versiontracker;

import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;


import java.util.List;

/**
 * 文件修改监听器
 * 粒度较粗，用户关闭文件时触发
 */

public class MyFileEditorManagerListener implements FileEditorManagerListener {

    private final VersionManager versionManager;

    public MyFileEditorManagerListener(VersionManager versionManager) {
        this.versionManager = versionManager;
    }

    @Override
    public void fileClosed(@NotNull FileEditorManager source, @NotNull VirtualFile file) {
        versionManager.saveVersion(file);
    }
}
