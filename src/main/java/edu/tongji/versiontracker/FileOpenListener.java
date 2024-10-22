package edu.tongji.versiontracker;

import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.messages.MessageBusConnection;
import org.jetbrains.annotations.NotNull;

/**
 * 文件打开监听器
 * 用于监听文件在编辑器中被打开的事件
 */
public class FileOpenListener implements FileEditorManagerListener {

    private final VersionManager versionManager;

    public FileOpenListener(VersionManager versionManager) {
        this.versionManager = versionManager;
    }

    @Override
    public void fileOpened(@NotNull FileEditorManager source, @NotNull VirtualFile file) {
        // 当文件被打开时，调用 saveInitialVersion 方法
        versionManager.saveInitialVersion(file);
    }

}
