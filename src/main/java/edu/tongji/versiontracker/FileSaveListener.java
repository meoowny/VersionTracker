package edu.tongji.versiontracker;

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.newvfs.BulkFileListener;
import com.intellij.openapi.vfs.newvfs.events.VFileEvent;
import org.jetbrains.annotations.NotNull;

import java.util.List;

import static com.intellij.ide.actions.OpenProjectFileChooserDescriptor.isProjectFile;

/**
 * 文件修改监听器
 * 粒度较粗，用户手动保存后触发
 */
public class FileSaveListener implements BulkFileListener {

    private final VersionManager versionManager;

    public FileSaveListener(VersionManager versionManager) {
        this.versionManager = versionManager;
    }

    @Override
    public void after(@NotNull List<? extends VFileEvent> events) {
        for (VFileEvent event : events) {
            VirtualFile file = event.getFile();
            if (file == null || file.isDirectory()) {
                continue;
            }
            // 检查文件是否属于项目
            if (!file.isInLocalFileSystem() || !isProjectFile(file)) {
                return;
            }

            // 文件保存事件
            if (event.isFromSave()) {
//                versionManager.handleEditEvent(file);
            }
        }
    }
}
