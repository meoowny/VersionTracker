package edu.tongji.versiontracker;

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.newvfs.BulkFileListener;
import com.intellij.openapi.vfs.newvfs.events.VFileEvent;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * 文件修改监听器
 * 粒度较粗，用户手动保存后触发
 */
public class FileListener implements BulkFileListener {

    private final VersionManager versionManager;

    public FileListener(VersionManager versionManager) {
        this.versionManager = versionManager;
    }

    @Override
    public void after(@NotNull List<? extends VFileEvent> events) {
        for (VFileEvent event : events) {
            VirtualFile file = event.getFile();
            if (file == null || file.isDirectory()) {
                continue;
            }

            // 文件保存事件
            if (event.isFromSave()) {
                versionManager.saveVersion(file);
            }
        }
    }
}
